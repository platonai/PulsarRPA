package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import kotlinx.coroutines.*
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.io.IOException
import java.lang.IllegalStateException
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

open class StreamingCrawler(
        private val urls: Sequence<String>,
        private val options: LoadOptions = LoadOptions.create(),
        session: PulsarSession = PulsarContexts.createSession(),
        autoClose: Boolean = true
): Crawler(session, autoClose) {
    companion object {
        private val instanceSequencer = AtomicInteger()
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
        private val illegalState = AtomicBoolean()

        init {
            mapOf(
                    "availableMemory" to Gauge { Strings.readableBytes(availableMemory) },
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
    private val isAppActive get() = isActive && !illegalState.get()
    private val numTasks = AtomicInteger()
    private val taskTimeout = Duration.ofMinutes(10)
    private var flowState = FlowState.CONTINUE
    private var finishScript: Path? = null

    var jobName: String = "crawler-" + RandomStringUtils.randomAlphanumeric(5)
    var pageCollector: ConcurrentLinkedQueue<WebPage>? = null
    var globalCache: GlobalCache? = null

    val id = instanceSequencer.incrementAndGet()
    var onLoadComplete: (WebPage) -> Unit = {}

    init {
        generateFinishCommand()

        mapOf(
                "idleTime" to Gauge { idleTime.readable() },
                "numTasks" to Gauge { numTasks.get() }
        ).forEach { AppMetrics.register(this, id.toString(), it.key, it.value) }
    }

    open suspend fun run() {
        supervisorScope {
            run(this)
        }

        log.info("Total {} tasks are done in session {}", numTasks, session)
    }

    open suspend fun run(scope: CoroutineScope) {
        urls.forEachIndexed { j, url ->
            if (!isAppActive) {
                return@run
            }

            globalTasks.incrementAndGet()
            val state = load(1 + j, url, scope)
            globalFinishedTasks.incrementAndGet()

            if (state != FlowState.CONTINUE) {
                return@run
            }
        }
    }

    private suspend fun load(j: Int, url: String, scope: CoroutineScope): FlowState {
        lastActiveTime = Instant.now()
        numTasks.incrementAndGet()

        while (isAppActive && globalRunningTasks.get() > fetchConcurrency) {
            if (j % 120 == 0) {
                val elapsedTime = Duration.between(lastActiveTime, Instant.now())
                log.info("It takes long time to run {} tasks | {} -> {}",
                        globalRunningTasks, lastActiveTime, elapsedTime.readable())
            }
            delay(1000)
        }

        while (isAppActive && remainingMemory < 0) {
            if (j % 20 == 0) {
                handleMemoryShortage(j)
            }
            delay(1000)
        }

        if (FileCommand.check("finish-job")) {
            log.info("Found finish-job command, quit streaming crawler ...")
            return FlowState.BREAK
        }

        if (!isAppActive) {
            return FlowState.BREAK
        }

        val context = Dispatchers.Default + CoroutineName("w")
        // must increase before launch because we have to control the number of running tasks
        globalRunningTasks.incrementAndGet()
        scope.launch(context) { load(url) }

        return flowState
    }

    private suspend fun load(url: String): WebPage? {
        if (!isAppActive) {
            return null
        }

        val page = kotlin.runCatching { withTimeoutOrNull(taskTimeout.toMillis()) { load0(url) } }
                .onFailure { log.warn("Unexpected exception", it) }
                .getOrNull()
        globalRunningTasks.decrementAndGet()

        if (!isAppActive) {
            return null
        }

        if (page == null) {
            globalTimeout.incrementAndGet()
            log.info("Task timeout ({}) to load page | {}", taskTimeout, url)
        }

        if (page == null || page.crawlStatus.isUnFetched) {
            globalCache?.also {
                val added = it.limitedReentrantFetchUrls.add(url)
                globalRetries.incrementAndGet()
                log.info("{}. Will retry task the {}/{}th times | {}",
                        page?.id?:0,
                        page?.fetchRetries ?: 0, it.limitedReentrantFetchUrls.count(url),
                        url)
            }
        }

        lastActiveTime = Instant.now()
        page?.let(onLoadComplete)

        return page
    }

    private suspend fun load0(url: String): WebPage? {
        return session.runCatching { loadDeferred(url, options) }
                .onFailure { flowState = handleException(url, it) }
                .getOrNull()
                ?.also { pageCollector?.add(it) }
    }

    private fun handleException(url: String, e: Throwable): FlowState {
        if (flowState == FlowState.BREAK) {
            return flowState
        }

        when (e) {
            is CancellationException,
            is IllegalStateException,
            is IllegalApplicationContextStateException -> {
                if (illegalState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.warn("Illegal app context, quit streaming crawler ... | {}", e.message)
                }
                return FlowState.BREAK
            }
            is ProxyVendorUntrustedException -> log.error(e.message?:"Unexpected error").let { return FlowState.BREAK }
            is TimeoutCancellationException -> log.warn("Timeout cancellation: {} | {}", Strings.simplifyException(e), url)
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
