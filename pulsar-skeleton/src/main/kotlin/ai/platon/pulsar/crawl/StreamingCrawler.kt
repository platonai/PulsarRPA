package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.collect.ConcurrentLoadingIterable
import ai.platon.pulsar.common.collect.DelayUrl
import ai.platon.pulsar.common.config.AppConstants.BROWSER_DRIVER_INSTANCE_REQUIRED_MEMORY
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_NUMBER
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.message.LoadedPageFormatter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyInsufficientBalanceException
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.common.urls.DegenerateUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.Urls
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import kotlinx.coroutines.*
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
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
    val finishes = registry.multiMetric(this, "finishes")

    val fetchSuccesses = registry.multiMetric(this, "fetchSuccesses")

    val drops = registry.meter(this, "drops")
    val processing = registry.meter(this, "processing")
    val timeouts = registry.meter(this, "timeouts")
}

enum class CriticalWarning(val message: String) {
    OUT_OF_MEMORY("OUT OF MEMORY"),
    OUT_OF_DISK_STORAGE("OUT OF DISK STORAGE"),
    NO_PROXY("NO PROXY AVAILABLE"),
    FAST_CONTEXT_LEAK("CONTEXT LEAK TOO FAST"),
    WRONG_DISTRICT("WRONG DISTRICT! ALL RESIDENT TASKS ARE PAUSED"),
}

open class StreamingCrawler<T : UrlAware>(
    /**
     * The url sequence
     * */
    val urls: Sequence<T>,
    /**
     * The default load options
     * */
    val defaultOptions: LoadOptions,
    /**
     * The default pulsar session to use
     * */
    session: PulsarSession = PulsarContexts.createSession(),
    /**
     * A optional global cache which will hold the retry tasks
     * */
    val globalCache: GlobalCache? = null,
    /**
     * The crawl event handler
     * */
    val crawlEventHandler: CrawlEventHandler = DefaultCrawlEventHandler(),
    autoClose: Boolean = true
) : AbstractCrawler(session, autoClose) {
    companion object {
        private val instanceSequencer = AtomicInteger()
        private val globalRunningInstances = AtomicInteger()
        private val globalRunningTasks = AtomicInteger()
        private val globalKilledTasks = AtomicInteger()
        private val globalTasks = AtomicInteger()

        private val globalMetrics = StreamingCrawlerMetrics()

        private val globalLoadingUrls = ConcurrentSkipListSet<String>()

        private val availableMemory get() = AppMetrics.availableMemory
        private val requiredMemory = BROWSER_DRIVER_INSTANCE_REQUIRED_MEMORY // 500 MiB
        private val remainingMemory get() = availableMemory - requiredMemory
        private var contextLeakWaitingTime = Duration.ZERO
        private var proxyVendorWaitingTime = Duration.ZERO
        private var criticalWarning: CriticalWarning? = null
        private var lastUrl = ""
        private var lastHtmlIntegrity = ""
        private var lastFetchError = ""
        private val isIllegalApplicationState = AtomicBoolean()

        private var wrongDistrict = AppMetrics.reg.multiMetric(this, "WRONG_DISTRICT_COUNT")

        init {
            mapOf(
                "globalRunningInstances" to Gauge { globalRunningInstances.get() },
                "globalRunningTasks" to Gauge { globalRunningTasks.get() },
                "globalKilledTasks" to Gauge { globalKilledTasks.get() },

                "contextLeakWaitingTime" to Gauge { contextLeakWaitingTime },
                "proxyVendorWaitingTime" to Gauge { proxyVendorWaitingTime },
                "000WARNING" to Gauge { criticalWarning?.message?.let { "!!! WARNING !!! $it" } ?: "" },
                "lastUrl" to Gauge { lastUrl },
                "lastHtmlIntegrity" to Gauge { lastHtmlIntegrity },
                "lastFetchError" to Gauge { lastFetchError },
            ).let { AppMetrics.reg.registerAll(this, it) }
        }
    }

    private val logger = getLogger(StreamingCrawler::class)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    private val taskLogger = getLogger(StreamingCrawler::class, ".Task")
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

    private val gauges = mapOf(
        "idleTime" to Gauge { idleTime.readable() }
    )

    init {
        AppMetrics.reg.registerAll(this, "$id.g", gauges)

        val cache = globalCache
        if (cache != null) {
            val cacheGauges = mapOf(
                "pageCacheSize" to Gauge { cache.pageCache.size },
                "documentCacheSize" to Gauge { cache.documentCache.size }
            )
            AppMetrics.reg.registerAll(this, "$id.g", cacheGauges)
        }

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
        logger.info("Starting streaming crawler ... | {}", defaultOptions)

        globalRunningInstances.incrementAndGet()

        val startTime = Instant.now()

        while (isActive) {
            if (!urls.iterator().hasNext()) {
                sleepSeconds(1)
            }

            urls.forEachIndexed { j, url ->
                globalTasks.incrementAndGet()

                if (!isActive) {
                    globalMetrics.drops.mark()
                    return@startCrawlLoop
                }

                tracer?.trace(
                    "{}. {}/{} running tasks, processing {}",
                    globalTasks, globalLoadingUrls.size, globalRunningTasks, url.configuredUrl
                )

                // The largest disk must have at least 10GiB remaining space
                if (AppMetrics.freeSpace.maxOfOrNull { ByteUnit.convert(it, "G") } ?: 0.0 < 10.0) {
                    logger.error("Disk space is full!")
                    criticalWarning = CriticalWarning.OUT_OF_DISK_STORAGE
                    return@startCrawlLoop
                }

                if (url.isNil) {
                    globalMetrics.drops.mark()
                    return@forEachIndexed
                }

                // disabled, might be slow
                val urlSpec = Urls.splitUrlArgs(url.url).first
                if (alwaysFalse() && doLaterIfProcessing(urlSpec, url, Duration.ofSeconds(10))) {
                    return@forEachIndexed
                }

                globalLoadingUrls.add(urlSpec)
                val state = runWithStatusCheck(1 + j, url, scope)

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

        logger.info(
            "All done. Total {} tasks are processed in session {} in {}",
            globalMetrics.tasks.counter.count, session,
            DateTimes.elapsedTime(startTime).readable()
        )
    }

    private suspend fun runWithStatusCheck(j: Int, url: UrlAware, scope: CoroutineScope): FlowState {
        lastActiveTime = Instant.now()

        while (isActive && globalRunningTasks.get() > fetchConcurrency) {
            if (j % 120 == 0) {
                logger.info(
                    "$j. Long time to run $globalRunningTasks tasks" +
                            " | $lastActiveTime -> {}", idleTime.readable()
                )
            }
            delay(1000)
        }

        var k = 0
        while (isActive && remainingMemory < 0) {
            if (k++ % 20 == 0) {
                handleMemoryShortage(k)
            }
            criticalWarning = CriticalWarning.OUT_OF_MEMORY
            delay(1000)
        }

        val contextLeaksRate = PrivacyContext.globalMetrics.contextLeaks.meter.fifteenMinuteRate
        if (contextLeaksRate >= 5 / 60f) {
            criticalWarning = CriticalWarning.FAST_CONTEXT_LEAK
            handleContextLeaks()
        }

        if (wrongDistrict.hourlyCounter.count > 60) {
            handleWrongDistrict()
        }

        if (proxyOutOfService > 0) {
            criticalWarning = CriticalWarning.NO_PROXY
            handleProxyOutOfService()
        }

        if (FileCommand.check("finish-job")) {
            logger.info("Find finish-job command, quit streaming crawler ...")
            flowState = FlowState.BREAK
            return flowState
        }

        if (!isActive) {
            flowState = FlowState.BREAK
            return flowState
        }

        criticalWarning = null

        val context = Dispatchers.Default + CoroutineName("w")
        val urlSpec = Urls.splitUrlArgs(url.url).first
        // must increase before launch because we have to control the number of running tasks
        globalRunningTasks.incrementAndGet()
        scope.launch(context) {
            try {
                globalMetrics.tasks.mark()
                if (AmazonDiagnosis.isAmazon(urlSpec)) {
                    AmazonMetrics.tasks.mark(urlSpec)
                }
                runUrlTask(url)
            } finally {
                lastActiveTime = Instant.now()

                globalLoadingUrls.remove(urlSpec)
                globalRunningTasks.decrementAndGet()

                globalMetrics.finishes.mark()
                if (AmazonDiagnosis.isAmazon(urlSpec)) {
                    AmazonMetrics.finishes.mark(urlSpec)
                }
            }
        }

        return flowState
    }

    private suspend fun runUrlTask(url: UrlAware) {
        if (url is ListenableHyperlink && url is DegenerateUrl) {
            val eventHandler = url.crawlEventHandler
            eventHandler.onBeforeLoad(url)
            eventHandler.onLoad(url)
            eventHandler.onAfterLoad(url, WebPage.NIL)
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
            if (page != null) {
                collectStatAfterLoad(page)
            }
        } catch (e: TimeoutCancellationException) {
            globalMetrics.timeouts.mark()
            logger.info("{}. Task timeout ({}) to load page | {}", globalMetrics.timeouts.count, taskTimeout, url)
        } catch (e: Throwable) {
            if (e.javaClass.name == "kotlinx.coroutines.JobCancellationException") {
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.beginTerminate()
                    logger.warn("Streaming crawler coroutine was cancelled, quit ...", e)
                }
                flowState = FlowState.BREAK
            } else {
                logger.warn("Unexpected exception", e)
            }
        }

        return page
    }

    private fun collectStatAfterLoad(page: WebPage) {
        lastFetchError = page.protocolStatus.takeIf { !it.isSuccess }?.toString() ?: ""
        if (!page.protocolStatus.isSuccess) {
            return
        }

        wrongDistrict.reset()
        lastUrl = page.configuredUrl
        lastHtmlIntegrity = page.htmlIntegrity.toString()
        if (page.htmlIntegrity == HtmlIntegrity.WRONG_DISTRICT) {
            wrongDistrict.mark()
        }

        if (page.isFetched) {
            globalMetrics.fetchSuccesses.mark()
            if (AmazonDiagnosis.isAmazon(page.url)) {
                AmazonMetrics.fetchSuccesses.mark(page.url)
            }
        }
        globalMetrics.successes.mark()
        if (AmazonDiagnosis.isAmazon(page.url)) {
            AmazonMetrics.successes.mark(page.url)
        }
    }

    private fun beforeUrlLoad(url: UrlAware): UrlAware? {
        if (url is ListenableHyperlink) {
            url.loadEventHandler.onFilter(url.url) ?: return null
        }

        crawlEventHandler.onFilter(url) ?: return null

        crawlEventHandler.onBeforeLoad(url)

        if (url is ListenableHyperlink) {
            url.crawlEventHandler.onBeforeLoad(url)
        }

        return url
    }

    /**
     * TODO: keep consistent with protocolStatus.isRetry and crawlStatus.isRetry
     * */
    private fun afterUrlLoad(url: UrlAware, page: WebPage?) {
        if (url is ListenableHyperlink) {
            url.crawlEventHandler.onAfterLoad(url, page)
        }

        if (page != null) {
            crawlEventHandler.onAfterLoad(url, page)
        }

        when {
            page == null -> handleRetry(url, page)
            page.protocolStatus.isRetry -> handleRetry(url, page)
            page.crawlStatus.isRetry -> handleRetry(url, page)
            page.crawlStatus.isGone -> {
                globalMetrics.gone.mark()
                taskLogger.info("{}", LoadedPageFormatter(page, prefix = "Gone"))
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun loadWithEventHandlers(url: UrlAware): WebPage? {
        // apply the default options, arguments in the url has the highest priority
        val options = session.options("$defaultArgs ${url.args}")
        if (options.isDead()) {
            globalKilledTasks.incrementAndGet()
            return null
        }

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
                    AppContext.beginTerminate()
                    logger.warn("\n!!!Illegal application context, quit ... | {}", e.message)
                }
                return FlowState.BREAK
            }
            is IllegalStateException -> {
                logger.warn("Illegal state", e)
            }
            is ProxyInsufficientBalanceException -> {
                proxyOutOfService++
                logger.warn("{}. {}", proxyOutOfService, e.message)
            }
            is ProxyVendorUntrustedException -> {
                logger.warn("Proxy is untrusted | {}", e.message)
                return FlowState.BREAK
            }
            is ProxyException -> {
                logger.warn("Unexpected proxy exception | {}", Strings.simplifyException(e))
            }
            is TimeoutCancellationException -> {
                logger.warn("Timeout cancellation: {} | {}", Strings.simplifyException(e), url)
            }
            is CancellationException -> {
                // Comes after TimeoutCancellationException
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    AppContext.beginTerminate()
                    logger.warn("Streaming crawler job was canceled, quit ...", e)
                }
                return FlowState.BREAK
            }
            else -> throw e
        }

        return FlowState.CONTINUE
    }

    private fun doLaterIfProcessing(urlSpec: String, url: UrlAware, delay: Duration): Boolean {
        if (globalCache == null) {
            return false
        }

        if (urlSpec in globalLoadingUrls || urlSpec in globalCache.fetchingUrlQueue) {
            // process later, hope the page is fetched
            logger.debug("Task is in process, do it {} later | {}", delay.readable(), url.configuredUrl)
            fetchDelayed(url, delay)
            return true
        }

        return false
    }

    private fun handleRetry(url: UrlAware, page: WebPage?) {
        val retries = 1L + (page?.fetchRetries ?: 0)
        if (page != null && retries > page.maxRetries) {
            // should not go here, because the page should be marked as GONE
            globalMetrics.gone.mark()
            taskLogger.info("{}", LoadedPageFormatter(page, prefix = "Gone (unexpected)"))
            return
        }

        val delay = Duration.ofMinutes(1L + 2 * retries)
//        val delayCache = globalCache.fetchCaches.delayCache
//        // erase -refresh options
//        url.args = url.args?.replace("-refresh", "-refresh-erased")
//        delayCache.add(DelayUrl(url, delay))
        fetchDelayed(url, delay)

        globalMetrics.retries.mark()
        if (page != null) {
            val prefix = "Trying ${retries}th ${delay.readable()} later"
            taskLogger.info("{}", LoadedPageFormatter(page, prefix = prefix))
        }
    }

    private fun fetchDelayed(url: UrlAware, delay: Duration) {
        if (globalCache == null) {
            return
        }

        val delayCache = globalCache.fetchCaches.delayCache
        // erase -refresh options
//        url.args = url.args?.replace("-refresh", "-refresh-erased")
        url.args = url.args?.let { LoadOptions.eraseOptions(it, "refresh") }
        require(url.args?.contains("refresh") != true)

        delayCache.add(DelayUrl(url, delay))
    }

    private fun handleMemoryShortage(j: Int) {
        logger.info(
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
            logger.takeIf { k % 60 == 0 }
                ?.warn(
                    "Context leaks too fast: {} leaks/seconds, memory: {}",
                    contextLeaksRate, Strings.readableBytes(availableMemory)
                )
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
            logger.warn("Proxy account insufficient balance, check it again ...")
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

    private suspend fun handleWrongDistrict() {
        var k = 0
        while (wrongDistrict.hourlyCounter.count > 60) {
            criticalWarning = CriticalWarning.WRONG_DISTRICT
            logger.takeIf { k++ % 20 == 0 }?.warn("{}", criticalWarning?.message ?: "")
            delay(1000)
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
            logger.error(e.toString())
        }
    }
}
