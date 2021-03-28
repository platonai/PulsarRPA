package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.collect.ConcurrentLoadingIterable
import ai.platon.pulsar.common.collect.DelayUrl
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
import ai.platon.pulsar.common.measure.FileSizeUnits
import ai.platon.pulsar.common.message.CompletedPageFormatter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyInsufficientBalanceException
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.common.url.PseudoUrl
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import kotlinx.coroutines.*
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class StreamingCrawlerMetrics {
    private val registry = AppMetrics.defaultMetricRegistry

    val retries = registry.multiMetric(this, "retries")
    val gone = registry.multiMetric(this, "gone")
    val tasks = registry.multiMetric(this, "tasks")
    val successes = registry.multiMetric(this, "successes")
    val fetchSuccesses = registry.multiMetric(this, "fetchSuccesses")
    val finishes = registry.multiMetric(this, "finishes")

    val drops = registry.meter(this, "drops")
    val processing = registry.meter(this, "processing")
    val timeouts = registry.meter(this, "timeouts")
}

open class StreamingCrawler<T : UrlAware>(
    private val urls: Sequence<T>,
    private val defaultOptions: LoadOptions,
    session: PulsarSession = PulsarContexts.createSession(),
    /**
     * A optional global cache which will hold the retry tasks
     * */
    val globalCache: GlobalCache? = null,
    autoClose: Boolean = true
) : AbstractCrawler(session, autoClose) {
    companion object {
        private val instanceSequencer = AtomicInteger()
        private val globalRunningInstances = AtomicInteger()
        private val globalRunningTasks = AtomicInteger()

        private val globalMetrics = StreamingCrawlerMetrics()

        private val globalLoadingUrls = ConcurrentSkipListSet<String>()

        private val availableMemory get() = AppMetrics.availableMemory
        private val requiredMemory = 500 * 1024 * 1024L // 500 MiB
        private val remainingMemory get() = availableMemory - requiredMemory
        private var contextLeakWaitingTime = Duration.ZERO
        private var proxyVendorWaitingTime = Duration.ZERO
        private var lastUrl = ""
        private val isIllegalApplicationState = AtomicBoolean()

        init {
            mapOf(
                "globalRunningInstances" to Gauge { globalRunningInstances.get() },
                "globalRunningTasks" to Gauge { globalRunningTasks.get() },

                "contextLeakWaitingTime" to Gauge { contextLeakWaitingTime },
                "proxyVendorWaitingTime" to Gauge { proxyVendorWaitingTime },
                "lastUrl" to Gauge { lastUrl }
            ).let { AppMetrics.reg.registerAll(this, it) }
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
    private val defaultArgs = defaultOptions.toString()

    private var proxyOutOfService = 0
    private var quit = false
    override val isActive get() = super.isActive && !quit && !isIllegalApplicationState.get()
    private val taskTimeout = Duration.ofMinutes(10)

    @Volatile
    private var flowState = FlowState.CONTINUE

    var jobName: String = "crawler-" + RandomStringUtils.randomAlphanumeric(5)

    var proxyPool: ProxyPool? = null

    val id = instanceSequencer.incrementAndGet()

    val crawlEventHandler = DefaultCrawlEventHandler()

    private val gauges = mapOf(
        "idleTime" to Gauge { idleTime.readable() }
    )

    init {
        AppMetrics.reg.registerAll(this, "$id.g", gauges)
        generateFinishCommand()
    }

    open fun run() {
        runBlocking {
            supervisorScope {
                run(this)
            }
        }
    }

    open suspend fun run(scope: CoroutineScope) {
        startCrawlLoop(scope)
    }

    fun quit() {
        quit = true
    }

    protected suspend fun startCrawlLoop(scope: CoroutineScope) {
        log.info("Starting streaming crawler ... | {}", defaultOptions)

        globalRunningInstances.incrementAndGet()

        val startTime = Instant.now()

        while (isActive) {
            if (!urls.iterator().hasNext()) {
                sleepSeconds(1)
            }

            urls.forEachIndexed { j, url ->
                if (!isActive) {
                    globalMetrics.drops.mark()
                    return@startCrawlLoop
                }

                // The largest disk must have at least 10GiB remaining space
                if (AppMetrics.freeSpace.maxOfOrNull { FileSizeUnits.convert(it, "G") } ?: 0.0 < 10.0) {
                    // log.error("Disk space is full!")
                    // return@runInScope
                }

                if (url.isNil) {
                    globalMetrics.drops.mark()
                    return@forEachIndexed
                }

                /**
                 * TODO: proper handling the result, especially the client ask for a result
                 * */
                if (url.url in globalLoadingUrls) {
                    globalMetrics.drops.mark()
                    globalMetrics.processing.mark()
                    return@forEachIndexed
                }

                val state = try {
                    globalLoadingUrls.add(url.url)
                    runWithStatusCheck(1 + j, url, scope)
                } finally {
                    globalMetrics.finishes.mark()
                    globalLoadingUrls.remove(url.url)
                }

                if (state != FlowState.CONTINUE) {
                    return@startCrawlLoop
                } else {
                    // if urls is ConcurrentLoadingIterable
                    // TODO: the line below can be removed
                    (urls.iterator() as? ConcurrentLoadingIterable.LoadingIterator)?.tryLoad()
                }
            }
        }

        globalRunningInstances.decrementAndGet()

        log.info(
            "All done. Total {} tasks are processed in session {} in {}",
            globalMetrics.tasks.counter.count, session,
            DateTimes.elapsedTime(startTime).readable()
        )
    }

    private suspend fun runWithStatusCheck(j: Int, url: UrlAware, scope: CoroutineScope): FlowState {
        lastActiveTime = Instant.now()
        globalMetrics.tasks.mark()

        while (isActive && globalRunningTasks.get() > fetchConcurrency) {
            if (j % 120 == 0) {
                log.info(
                    "{}. It takes long time to run {} tasks | {} -> {}",
                    j, globalRunningTasks, lastActiveTime, idleTime.readable()
                )
            }
            delay(1000)
        }

        var k = 0
        while (isActive && remainingMemory < 0) {
            if (k++ % 20 == 0) {
                handleMemoryShortage(k)
            }
            delay(1000)
        }

        val contextLeaksRate = PrivacyContext.globalMetrics.contextLeaks.meter.fifteenMinuteRate
        if (contextLeaksRate >= 5 / 60f) {
            handleContextLeaks()
        }

        if (proxyOutOfService > 0) {
            handleProxyOutOfService()
        }

        if (FileCommand.check("finish-job")) {
            log.info("Find finish-job command, quit streaming crawler ...")
            flowState = FlowState.BREAK
            return flowState
        }

        if (!isActive) {
            flowState = FlowState.BREAK
            return flowState
        }

        val context = Dispatchers.Default + CoroutineName("w")
        // must increase before launch because we have to control the number of running tasks
        globalRunningTasks.incrementAndGet()
        scope.launch(context) {
            runUrlTask(url)
            lastActiveTime = Instant.now()
            globalRunningTasks.decrementAndGet()
        }

        return flowState
    }

    private suspend fun runUrlTask(url: UrlAware) {
        if (url is ListenableHyperlink && url is PseudoUrl) {
            val eventHandler = url.crawlEventHandler
            if (eventHandler != null) {
                eventHandler.onBeforeLoad(url)
                eventHandler.onLoad(url)
                eventHandler.onAfterLoad(url, WebPage.NIL)
            }
        } else {
            val normalizedUrl = beforeUrlLoad(url)
            if (normalizedUrl != null) {
                val page = loadUrl(normalizedUrl)
                afterUrlLoad(normalizedUrl, page)
            }
        }
    }

    private suspend fun loadUrl(url: UrlAware): WebPage? {
        var page: WebPage? = null
        try {
            page = withTimeout(taskTimeout.toMillis()) { loadWithEventHandlers(url) }
            if (page != null && page.protocolStatus.isSuccess) {
                lastUrl = page.configuredUrl
                if (page.isContentUpdated) {
                    globalMetrics.fetchSuccesses.mark()
                }
                globalMetrics.successes.mark()
            }
        } catch (e: TimeoutCancellationException) {
            globalMetrics.timeouts.mark()
            log.info("{}. Task timeout ({}) to load page | {}", globalMetrics.timeouts.count, taskTimeout, url)
        } catch (e: Throwable) {
            if (e.javaClass.name == "kotlinx.coroutines.JobCancellationException") {
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.warn("Streaming crawler coroutine was cancelled, quit ...", e)
                }
                flowState = FlowState.BREAK
            } else {
                log.warn("Unexpected exception", e)
            }
        }

        return page
    }

    private fun beforeUrlLoad(url: UrlAware): UrlAware? {
        if (url is ListenableHyperlink) {
            url.loadEventHandler.onFilter(url.url) ?: return null
        }

        crawlEventHandler.onFilter(url) ?: return null

        crawlEventHandler.onBeforeLoad(url)

        if (url is ListenableHyperlink) {
            url.crawlEventHandler?.onBeforeLoad?.invoke(url)
        }

        return url
    }

    private fun afterUrlLoad(url: UrlAware, page: WebPage?) {
        if (page == null) {
            handleRetry(url, page)
        } else if (page.isContentUpdated && page.protocolStatus.isRetry) {
            handleRetry(url, page)
        }

        if (page != null) {
            crawlEventHandler.onAfterLoad(url, page)
            if (url is ListenableHyperlink) {
                url.crawlEventHandler?.onAfterLoad?.invoke(url, page)
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun loadWithEventHandlers(url: UrlAware): WebPage? {
        // apply the default options, arguments in the url has the highest priority
        val options = session.options("$defaultArgs ${url.args}")

        return session.runCatching { loadDeferred(url, options) }
            .onFailure { flowState = handleException(url, it) }
            .getOrNull()
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
                    log.warn("\n!!!Illegal application context, quit ... | {}", e.message)
                }
                return FlowState.BREAK
            }
            is IllegalStateException -> {
                log.warn("Illegal state", e)
            }
            is ProxyInsufficientBalanceException -> {
                proxyOutOfService++
                log.warn("{}. {}", proxyOutOfService, e.message)
            }
            is ProxyVendorUntrustedException -> {
                log.warn("Proxy is untrusted | {}", e.message)
                return FlowState.BREAK
            }
            is ProxyException -> {
                log.warn("Unexpected proxy exception | {}", Strings.simplifyException(e))
            }
            is TimeoutCancellationException -> {
                log.warn("Timeout cancellation: {} | {}", Strings.simplifyException(e), url)
            }
            is CancellationException -> {
                // Comes after TimeoutCancellationException
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.warn("Streaming crawler job was canceled, quit ...", e)
                }
                return FlowState.BREAK
            }
            else -> throw e
        }

        return FlowState.CONTINUE
    }

    private fun handleRetry(url: UrlAware, page: WebPage?) {
        val gCache = globalCache
        if (gCache != null && !gCache.isFetching(url)) {
            val retries = 1L + (page?.fetchRetries ?: 0)
            val delay = Duration.ofMinutes(5L + 5 * retries)
            val cache = gCache.fetchCacheManager.delayCache
            if (cache.add(DelayUrl(url, delay))) {
                globalMetrics.retries.mark()
                if (page != null) {
                    log.info("{}", CompletedPageFormatter(page, prefix = "Retrying ${retries}th $delay"))
                }
            } else {
                globalMetrics.gone.mark()
                if (page != null) {
                    log.info("{}", CompletedPageFormatter(page, prefix = "Gone"))
                } else {
                    log.info("Page is gone | {}", url)
                }
            }
        }
    }

    private fun handleMemoryShortage(j: Int) {
        log.info(
            "$j.\tnumRunning: {}, availableMemory: {}, requiredMemory: {}, shortage: {}",
            globalRunningTasks,
            Strings.readableBytes(availableMemory),
            Strings.readableBytes(requiredMemory),
            Strings.readableBytes(abs(remainingMemory))
        )
        session.context.clearCaches()
        System.gc()
    }

    /**
     * The vendor proclaimed every ip can be used for more than 5 minutes,
     * If proxy is not enabled, the rate is always 0
     *
     * 5 / 60f = 0.083
     * */
    private suspend fun handleContextLeaks() {
        val contextLeaks = PrivacyContext.globalMetrics.contextLeaks
        val contextLeaksRate = contextLeaks.meter.fifteenMinuteRate
        var k = 0
        while (isActive && contextLeaksRate >= 5 / 60f && ++k < 600) {
            if (k % 60 == 0) {
                log.warn("Context leaks too fast: $contextLeaksRate leaks/seconds")
            }
            delay(1000)

            // trigger the meter updating
            contextLeaks.inc(-1)
            contextLeaks.inc(1)

            contextLeakWaitingTime += Duration.ofSeconds(1)
        }

        contextLeakWaitingTime = Duration.ZERO
    }

    private suspend fun handleProxyOutOfService() {
        while (isActive && proxyOutOfService > 0) {
            delay(1000)
            proxyVendorWaitingTime += Duration.ofSeconds(1)
            handleProxyOutOfService0()
        }

        proxyVendorWaitingTime = Duration.ZERO
    }

    private fun handleProxyOutOfService0() {
        if (++proxyOutOfService % 180 == 0) {
            log.warn("Proxy account insufficient balance, check it again ...")
            val p = proxyPool
            if (p != null) {
                p.runCatching { take() }.onFailure {
                    if (it !is ProxyInsufficientBalanceException) {
                        proxyOutOfService = 0
                    }
                }.onSuccess { proxyOutOfService = 0 }
            } else {
                proxyOutOfService = 0
            }
        }
    }

    private fun generateFinishCommand() {
        if (SystemUtils.IS_OS_UNIX) {
            generateFinishCommandUnix()
        }
    }

    private fun generateFinishCommandUnix() {
        val finishScriptPath = AppPaths.SCRIPT_DIR.resolve("finish-crawler.sh")
        val cmd = "#bin\necho finish-job $jobName >> " + AppPaths.PATH_LOCAL_COMMAND
        try {
            Files.write(finishScriptPath, cmd.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            Files.setPosixFilePermissions(finishScriptPath, PosixFilePermissions.fromString("rwxrw-r--"))
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }
}
