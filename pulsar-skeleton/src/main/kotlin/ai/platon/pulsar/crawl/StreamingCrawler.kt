package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.collect.ConcurrentLoadingIterable
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.LoadOptionsNormalizer
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.Hyperlinks
import ai.platon.pulsar.crawl.common.ListenableHyperlink
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import kotlinx.coroutines.*
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

open class StreamingCrawler<T: UrlAware>(
        private val urls: Sequence<T>,
        private val options: LoadOptions = LoadOptions.create(),
        session: PulsarSession = PulsarContexts.createSession(),
        /**
         * A optional global cache which will hold the retry tasks
         * */
        val globalCache: GlobalCache? = null,
        autoClose: Boolean = true
): Crawler(session, autoClose) {
    companion object {
        private val instanceSequencer = AtomicInteger()
        private val globalRunningInstances = AtomicInteger()
        private val globalTasks = AtomicInteger()
        private val globalRunningTasks = AtomicInteger()
        private val globalFinishedTasks = AtomicInteger()
        private val globalTimeout = AtomicInteger()
        private val globalRetries = AtomicInteger()
        private val systemInfo = SystemInfo()
        // OSHI cached the value, so it's fast and safe to be called frequently
        private val availableMemory get() = systemInfo.hardware.memory.available
        private val requiredMemory = 500 * 1024 * 1024L // 500 MiB
        private val remainingMemory get() = availableMemory - requiredMemory
        private val isIllegalApplicationState = AtomicBoolean()

        init {
            mapOf(
                    "availableMemory" to Gauge { Strings.readableBytes(availableMemory) },
                    "globalRunningInstances" to Gauge { globalRunningInstances.get() },
                    "globalTasks" to Gauge { globalTasks.get() },
                    "globalRunningTasks" to Gauge { globalRunningTasks.get() },
                    "globalFinishedTasks" to Gauge { globalFinishedTasks.get() },
                    "globalTimeout" to Gauge { globalTimeout.get() },
                    "globalRetries" to Gauge { globalRetries.get() }
            ).forEach { AppMetrics.register(this, it.key, it.value) }
        }
    }

    private val log = LoggerFactory.getLogger(StreamingCrawler::class.java)
    private val conf = session.sessionConfig
    private val numPrivacyContexts get() = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)
    private val numMaxActiveTabs get() = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)
    private val fetchConcurrency get() = numPrivacyContexts * numMaxActiveTabs
    private val idleTimeout = Duration.ofMinutes(20)
    private var lastActiveTime = Instant.now()
    private val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    private val isIdle get() = idleTime > idleTimeout
    private var quit = false
    override val isActive get() = super.isActive && !quit && !isIllegalApplicationState.get()
    private val numTasks = AtomicInteger()
    private val taskTimeout = Duration.ofMinutes(10)
    private var flowState = FlowState.CONTINUE
    private var finishScript: Path? = null

    var jobName: String = "crawler-" + RandomStringUtils.randomAlphanumeric(5)
    var pageCollector: ConcurrentLinkedQueue<WebPage>? = null

    val id = instanceSequencer.incrementAndGet()
    // TODO: use event handler instead
    var onLoadComplete: (UrlAware, WebPage) -> Unit = { _: UrlAware, _: WebPage -> }

    init {
        generateFinishCommand()

        mapOf(
                "idleTime" to Gauge { idleTime.readable() },
                "numTasks" to Gauge { numTasks.get() }
        ).forEach { AppMetrics.register(this, id.toString(), it.key, it.value) }
    }

    fun quit() {
        quit = true
    }

    open suspend fun run() {
        supervisorScope {
            run(this)
        }
    }

    open suspend fun run(scope: CoroutineScope) {
        log.info("Starting streaming crawler ...")

        globalRunningInstances.incrementAndGet()

        val startTime = Instant.now()

        while (isActive) {
            if (!urls.iterator().hasNext()) {
                sleepSeconds(1)
            }

            urls.forEachIndexed { j, url ->
                if (!isActive) {
                    return@run
                }

                if (url.isNil) {
                    return@forEachIndexed
                }

                globalTasks.incrementAndGet()
                val state = load(1 + j, url, scope)
                globalFinishedTasks.incrementAndGet()

                if (state != FlowState.CONTINUE) {
                    return@run
                }
            }
        }

        globalRunningInstances.decrementAndGet()

        log.info("All done. Total {} tasks are processed in session {} in {}",
                numTasks, session, DateTimes.elapsedTime(startTime).readable())
    }

    private suspend fun load(j: Int, url: UrlAware, scope: CoroutineScope): FlowState {
        lastActiveTime = Instant.now()
        numTasks.incrementAndGet()

        while (isActive && globalRunningTasks.get() > fetchConcurrency) {
            if (j % 120 == 0) {
                log.info("It takes long time to run {} tasks | {} -> {}",
                        globalRunningTasks, lastActiveTime, idleTime.readable())
            }
            delay(1000)
        }

        while (isActive && remainingMemory < 0) {
            if (j % 20 == 0) {
                handleMemoryShortage(j)
            }
            delay(1000)
        }

        if (FileCommand.check("finish-job")) {
            log.info("Find finish-job command, quit streaming crawler ...")
            return FlowState.BREAK
        }

        if (!isActive) {
            return FlowState.BREAK
        }

        val context = Dispatchers.Default + CoroutineName("w")
        // must increase before launch because we have to control the number of running tasks
        globalRunningTasks.incrementAndGet()
        scope.launch(context) { load(url) }

        return flowState
    }

    private suspend fun load(url: UrlAware): WebPage? {
        if (!isActive) {
            return null
        }

        val page = runCatching {
            withTimeoutOrNull(taskTimeout.toMillis()) { loadWithEventHandlers(url) }
        }.onFailure {
            if (it.javaClass.name == "kotlinx.coroutines.JobCancellationException") {
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.warn("Streaming crawler coroutine was cancelled, quit ...", it)
                }
                flowState = FlowState.BREAK
            } else {
                log.warn("Unexpected exception", it)
            }
        }.getOrNull()
        globalRunningTasks.decrementAndGet()

        if (!isActive) {
            return null
        }

        if (page == null) {
            globalTimeout.incrementAndGet()
            log.info("Task timeout ({}) to load page | {}", taskTimeout, url)
        }

        if (page == null || page.crawlStatus.isUnFetched) {
            globalCache?.also {
                val added = it.fetchCacheManager.higherCache.nReentrantQueue.add(url)
                globalRetries.incrementAndGet()
                if (page != null) {
                    log.info("{}. Will retry task the {}th times | {}", page.id, page.fetchRetries, page.href ?: url)
                }
            }
        }

        lastActiveTime = Instant.now()
        page?.let { onLoadComplete(url, it) }

        // if urls is ConcurrentLoadingIterable
        (urls.iterator() as? ConcurrentLoadingIterable.LoadingIterator)?.tryLoad()

        return page
    }

    @Throws(Exception::class)
    private suspend fun loadWithEventHandlers(url: UrlAware): WebPage? {
        val actualOptions = LoadOptionsNormalizer.normalize(options, url)

        val volatileConfig = conf.toVolatileConfig().apply { name = actualOptions.label }
        actualOptions.volatileConfig = volatileConfig
        if (url is ListenableHyperlink) {
            Hyperlinks.registerHandlers(url, volatileConfig)
        } else {
            volatileConfig.putBean(CapabilityTypes.FETCH_AFTER_FETCH_HANDLER, AddRefererAfterFetchHandler(url))
        }

        val normUrl = session.normalize(url, actualOptions)
        return session.runCatching { loadDeferred(normUrl) }
                .onFailure { flowState = handleException(url, it) }
                .getOrNull()
                ?.also { pageCollector?.add(it) }
    }

    @Throws(Exception::class)
    private fun handleException(url: UrlAware, e: Throwable): FlowState {
        if (flowState == FlowState.BREAK) {
            return flowState
        }

        when (e) {
            is IllegalApplicationContextStateException -> {
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.warn("\n!!!Illegal application context, quit streaming crawler ... | {}", e.message)
                }
                return FlowState.BREAK
            }
            is ProxyVendorUntrustedException -> log.error(e.message?:"Unexpected error").let { return FlowState.BREAK }
            is TimeoutCancellationException -> log.warn("Timeout cancellation: {} | {}", Strings.simplifyException(e), url)
            is CancellationException -> {
                // Comes after TimeoutCancellationException
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.warn("Streaming crawler job was canceled, quit ...", e)
                }
                return FlowState.BREAK
            }
            is IllegalStateException -> log.warn("Illegal state", e)
            else -> throw e
        }

        return FlowState.CONTINUE
    }

    private fun handleMemoryShortage(j: Int) {
        log.info("$j.\tnumRunning: {}, availableMemory: {}, requiredMemory: {}, shortage: {}",
                globalRunningTasks,
                Strings.readableBytes(availableMemory),
                Strings.readableBytes(requiredMemory),
                Strings.readableBytes(abs(remainingMemory))
        )
        session.context.clearCaches()
        System.gc()
    }

    private fun generateFinishCommand() {
        val script = finishScript?:return

        val cmd = "#bin\necho finish-job $jobName >> " + AppPaths.PATH_LOCAL_COMMAND
        try {
            Files.write(script, cmd.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxrw-r--"))
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }
}
