package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.collect.ConcurrentLoadingIterable
import ai.platon.pulsar.common.collect.DelayUrl
import ai.platon.pulsar.common.config.AppConstants.BROWSER_TAB_REQUIRED_MEMORY
import ai.platon.pulsar.common.config.AppConstants.DEFAULT_BROWSER_RESERVED_MEMORY
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.measure.ByteUnitConverter
import ai.platon.pulsar.common.message.LoadStatusFormatter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyInsufficientBalanceException
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.common.urls.DegenerateUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.common.url.ListenableUrl
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.AbstractPulsarSession
import ai.platon.pulsar.session.PulsarSession
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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    var urls: Sequence<T>,
    /**
     * The default load options
     * */
    defaultOptions: LoadOptions,
    /**
     * The default pulsar session to use
     * */
    session: PulsarSession = PulsarContexts.createSession(),
    /**
     * An optional global cache which will hold the retry tasks
     * */
    val globalCacheFactory: GlobalCacheFactory? = null,
    /**
     * The crawl event handler
     * */
    val crawlEventHandler: CrawlEventHandler = DefaultCrawlEventHandler(),
    /**
     * Do not use proxy
     * */
    val noProxy: Boolean = true,
    /**
     * Auto close or not
     * */
    autoClose: Boolean = true,
) : AbstractCrawler(session, defaultOptions, autoClose) {
    companion object {
        private val globalRunningInstances = AtomicInteger()
        private val globalRunningTasks = AtomicInteger()
        private val globalKilledTasks = AtomicInteger()
        private val globalTasks = AtomicInteger()

        private val globalMetrics = StreamingCrawlerMetrics()

        private val globalLoadingUrls = ConcurrentSkipListSet<String>()

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

    val numPrivacyContexts get() = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)
    val numMaxActiveTabs get() = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)
    val fetchConcurrency get() = numPrivacyContexts * numMaxActiveTabs

    private val totalMemory get() = Runtime.getRuntime().totalMemory()
    private val totalMemoryGiB get() = ByteUnit.BYTE.toGiB(totalMemory.toDouble())
    private val availableMemory get() = AppMetrics.availableMemory
    private val availableMemoryGiB get() = ByteUnit.BYTE.toGiB(availableMemory.toDouble())
    private val memoryToReserveLarge get() = conf.getDouble(BROWSER_MEMORY_TO_RESERVE_KEY, DEFAULT_BROWSER_RESERVED_MEMORY)
    private val memoryToReserve = when {
        totalMemoryGiB >= 14 -> ByteUnit.GIB.toBytes(3.0) // 3 GiB
        totalMemoryGiB >= 30 -> memoryToReserveLarge
        else -> BROWSER_TAB_REQUIRED_MEMORY
    }

    private val abstractSession get() = session as AbstractPulsarSession
    private val proxyPool: ProxyPool? = if (noProxy) null else abstractSession.context.getBeanOrNull()
    private var proxyOutOfService = 0

    val outOfWorkTimeout = Duration.ofMinutes(10)
    val fetchTaskTimeout get() = conf.getDuration(FETCH_TASK_TIMEOUT, Duration.ofMinutes(10))

    private var lastActiveTime = Instant.now()
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isOutOfWork get() = idleTime > outOfWorkTimeout

    private val enableSmartRetry get() = conf.getBoolean(CRAWL_SMART_RETRY, true)
    private val isIdle: Boolean
        get() {
            return !urls.iterator().hasNext()
                    && globalLoadingUrls.isEmpty()
                    && idleTime > Duration.ofSeconds(5)
        }
    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()
    private var forceQuit = false
    override val isActive get() = super.isActive && !forceQuit && !isIllegalApplicationState.get()

    @Volatile
    private var flowState = FlowState.CONTINUE

    var jobName: String = "crawler-" + RandomStringUtils.randomAlphanumeric(5)

    private val gauges = mapOf(
        "idleTime" to Gauge { idleTime.readable() },
        "numPrivacyContexts" to Gauge { numPrivacyContexts },
        "numMaxActiveTabs" to Gauge { numMaxActiveTabs },
        "fetchConcurrency" to Gauge { fetchConcurrency },
    )

    init {
        AppMetrics.reg.registerAll(this, "$id.g", gauges)

        val globalCache = globalCacheFactory?.globalCache
        if (globalCache != null) {
            val cacheGauges = mapOf(
                "pageCacheSize" to Gauge { globalCache.pageCache.size },
                "documentCacheSize" to Gauge { globalCache.documentCache.size }
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

    override fun await() {
        lock.withLock { notBusy.await() }
    }

    fun quit() {
        forceQuit = true
    }

    protected suspend fun startCrawlLoop(scope: CoroutineScope) {
        logger.info("Starting {} #{} ... {}", name, id, defaultOptions)

        globalRunningInstances.incrementAndGet()

        val startTime = Instant.now()

        var idleSeconds = 0
        while (isActive) {
            checkEmptyUrlSequence(++idleSeconds)

            urls.forEachIndexed { j, url ->
                idleSeconds = 0
                globalTasks.incrementAndGet()

                if (!isActive) {
                    globalMetrics.drops.mark()
                    return@startCrawlLoop
                }

                tracer?.trace(
                    "{}. {}/{} running tasks, processing {}",
                    globalTasks, globalLoadingUrls.size, globalRunningTasks, url.configuredUrl
                )

                // TODO: disabled in this branch, because AppMetrics.freeSpace may throw an exception on Windows 11
                // The largest disk must have at least 10 GiB remaining space
//                val freeSpace = AppMetrics.freeSpace.maxOfOrNull { ByteUnitConverter.convert(it, "G") } ?: 0.0
//                if (freeSpace < 10.0) {
//                    logger.error("Disk space is full!")
//                    criticalWarning = CriticalWarning.OUT_OF_DISK_STORAGE
//                    return@startCrawlLoop
//                }

                if (url.isNil) {
                    globalMetrics.drops.mark()
                    return@forEachIndexed
                }

                // disabled, might be slow
                val urlSpec = UrlUtils.splitUrlArgs(url.url).first
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

    private suspend fun checkEmptyUrlSequence(idleSeconds: Int) {
        if (urls.iterator().hasNext()) {
            return
        }

        val reportPeriod = when {
            idleSeconds < 1000 -> 120
            idleSeconds < 10000 -> 300
            else -> 6000
        }

        if (idleSeconds % reportPeriod == 0) {
            logger.debug("The url sequence is empty. {} {}", globalLoadingUrls.size, idleTime)
        }

        delay(1_000)

        if (isIdle) {
            lock.withLock { notBusy.signalAll() }
        }
    }

    private suspend fun runWithStatusCheck(j: Int, url: UrlAware, scope: CoroutineScope): FlowState {
        lastActiveTime = Instant.now()

        while (isActive && globalRunningTasks.get() >= fetchConcurrency) {
            if (j % 120 == 0) {
                logger.info("$j. Long time to run $globalRunningTasks tasks | $lastActiveTime -> {}",
                    idleTime.readable())
            }
            delay(1000)
        }

        var k = 0
        while (isActive && availableMemory < memoryToReserve) {
            if (k++ % 20 == 0) {
                handleMemoryShortage(k)
            }
            criticalWarning = CriticalWarning.OUT_OF_MEMORY
            delay(1000)
        }

        val contextLeaksRate = PrivacyContext.globalMetrics.contextLeaks.meter.fifteenMinuteRate
        if (isActive && contextLeaksRate >= 5 / 60f) {
            criticalWarning = CriticalWarning.FAST_CONTEXT_LEAK
            handleContextLeaks()
        }

        if (isActive && wrongDistrict.hourlyCounter.count > 60) {
            handleWrongDistrict()
        }

        if (isActive && proxyOutOfService > 0) {
            criticalWarning = CriticalWarning.NO_PROXY
            handleProxyOutOfService()
        }

        if (isActive && FileCommand.check("finish-job")) {
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
        val urlSpec = UrlUtils.splitUrlArgs(url.url).first
        val isAppActive = isActive
        // must increase before launch because we have to control the number of running tasks
        globalRunningTasks.incrementAndGet()
        scope.launch(context) {
            if (!isAppActive) {
                return@launch
            }

            try {
                globalMetrics.tasks.mark()
                runUrlTask(url)
            } finally {
                lastActiveTime = Instant.now()

                globalLoadingUrls.remove(urlSpec)
                globalRunningTasks.decrementAndGet()

                globalMetrics.finishes.mark()
            }
        }

        return flowState
    }

    private suspend fun runUrlTask(url: UrlAware) {
        if (url is ListenableUrl && url is DegenerateUrl) {
            // The url is degenerated, which means it's not a resource in the network to fetch.
            val eventHandler = url.eventHandler.crawlEventHandler
            runSafely("onWillLoad") { eventHandler.onWillLoad(url) }
            runSafely("onLoad") { eventHandler.onLoad(url) }
            runSafely("onLoaded") { eventHandler.onLoaded(url, WebPage.NIL) }
        } else {
            val normalizedUrl = onWillLoad(url)
            if (normalizedUrl != null) {
                val page = loadUrl(normalizedUrl)
                onLoaded(normalizedUrl, page)
            }
        }
    }

    private suspend fun loadUrl(url: UrlAware): WebPage? {
        var page: WebPage? = null
        val timeout = fetchTaskTimeout.plusSeconds(30).toMillis()
        try {
            page = withTimeout(timeout) { loadWithEventHandlers(url) }
            if (page != null) {
                collectStatAfterLoad(page)
            }
        } catch (e: TimeoutCancellationException) {
            globalMetrics.timeouts.mark()
            logger.info("{}. Task timeout ({}) to load page, thrown by [withTimeout] | {}",
                globalMetrics.timeouts.count, timeout, url)
        } catch (e: Throwable) {
            when {
                // The following exceptions can be caught as a Throwable but not the concrete exception,
                // one of the reason is the concrete exception is not public.
                e.javaClass.name == "kotlinx.coroutines.JobCancellationException" -> {
                    if (isIllegalApplicationState.compareAndSet(false, true)) {
                        logger.warn("Coroutine was cancelled, quit")
                    }
                    flowState = FlowState.BREAK
                }
                e.javaClass.name.contains("DriverLaunchException") -> {
                    logger.warn(e.message)
                }
                else -> {
                    logger.warn("[Unexpected]", e)
                }
            }
        }

        return page
    }

    private fun collectStatAfterLoad(page: WebPage) {
        lastFetchError = page.protocolStatus.takeIf { !it.isSuccess }?.toString() ?: ""
        if (!page.protocolStatus.isSuccess) {
            return
        }

        lastUrl = page.configuredUrl
        lastHtmlIntegrity = page.htmlIntegrity.toString()
        if (page.htmlIntegrity == HtmlIntegrity.WRONG_DISTRICT) {
            wrongDistrict.mark()
        } else {
            wrongDistrict.reset()
        }

        if (page.isFetched) {
            globalMetrics.fetchSuccesses.mark()
        }
        globalMetrics.successes.mark()
    }

    private fun onWillLoad(url: UrlAware): UrlAware? {
        if (url is ListenableUrl) {
            runSafely("onFilter") { url.eventHandler.loadEventHandler.onFilter(url.url) } ?: return null
        }

        runSafely("onFilter") { crawlEventHandler.onFilter(url) } ?: return null

        runSafely("onWillLoad") { crawlEventHandler.onWillLoad(url) }

        if (url is ListenableUrl) {
            runSafely("onWillLoad") { url.eventHandler.crawlEventHandler.onWillLoad(url) }
        }

        return url
    }

    private fun onLoaded(url: UrlAware, page: WebPage?) {
        // TODO: the event handlers should be merged
        if (url is ListenableUrl) {
            runSafely("onLoaded") { url.eventHandler.crawlEventHandler.onLoaded(url, page) }
        }

        if (page != null) {
            runSafely("onLoaded") { crawlEventHandler.onLoaded(url, page) }
        }

        if (enableSmartRetry) {
            handleRetry(url, page)
        }
    }

    private fun handleRetry(url: UrlAware, page: WebPage?) {
        when {
            !isActive -> return
            page == null -> handleRetry0(url, page)
            page.protocolStatus.isRetry -> handleRetry0(url, page)
            page.crawlStatus.isRetry -> handleRetry0(url, page)
            page.crawlStatus.isGone -> {
                globalMetrics.gone.mark()
                taskLogger.info("{}", LoadStatusFormatter(page, prefix = "Gone"))
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun loadWithEventHandlers(url: UrlAware): WebPage? {
        if (!isActive) {
            return null
        }

        // apply the default options, arguments in the url has the highest priority, so url.args needs to appear last
        val options = session.options("$defaultArgs ${url.args}")
        if (options.isDead()) {
            // The url is dead, drop the task
            globalKilledTasks.incrementAndGet()
            return null
        }

        return kotlin.runCatching { session.loadDeferred(url, options) }
            .onFailure { flowState = handleException(url, it) }
            .getOrNull()
    }

    @Throws(Exception::class)
    private fun handleException(url: UrlAware, e: Throwable): FlowState {
        if (flowState == FlowState.BREAK) {
            return flowState
        }

        if (!isActive) {
            logger.debug("Process is closing")
            return FlowState.BREAK
        }

        when (e) {
            is IllegalApplicationContextStateException -> {
                if (isIllegalApplicationState.compareAndSet(false, true)) {
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
                logger.warn("[Unexpected] proxy exception | {}", e.simplify())
            }
            is TimeoutCancellationException -> {
                logger.warn("[Timeout] Coroutine was cancelled, thrown by [withTimeout]. {} | {}",
                    e.simplify(), url)
            }
            is CancellationException -> {
                // Has to come after TimeoutCancellationException
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    logger.warn("Streaming crawler job was canceled, quit ...", e)
                }
                return FlowState.BREAK
            }
            else -> throw e
        }

        return FlowState.CONTINUE
    }

    private fun doLaterIfProcessing(urlSpec: String, url: UrlAware, delay: Duration): Boolean {
        val globalCache = globalCacheFactory?.globalCache ?: return false

        if (urlSpec in globalLoadingUrls || urlSpec in globalCache.fetchingCache) {
            // process later, hope the page is fetched
            logger.debug("Task is in process, do it {} later | {}", delay.readable(), url.configuredUrl)
            fetchDelayed(url, delay)
            return true
        }

        return false
    }

    private fun handleRetry0(url: UrlAware, page: WebPage?) {
        if (!isActive) {
            return
        }

        val nextRetryNumber = 1 + (page?.fetchRetries ?: 0)

        if (page != null && nextRetryNumber > page.maxRetries) {
            // should not go here, because the page should be marked as GONE
            globalMetrics.gone.mark()
            taskLogger.info("{}", LoadStatusFormatter(page, prefix = "Gone (unexpected)"))
            return
        }

        val delay = page?.retryDelay?.takeIf { !it.isZero } ?: retryDelayPolicy(nextRetryNumber, url)
//        val delayCache = globalCache.urlPool.delayCache
//        // erase -refresh options
//        url.args = url.args?.replace("-refresh", "-refresh-erased")
//        delayCache.add(DelayUrl(url, delay))
        fetchDelayed(url, delay)

        globalMetrics.retries.mark()
        if (page != null) {
            val prefix = "Trying ${nextRetryNumber}th ${delay.readable()} later"
            taskLogger.info("{}", LoadStatusFormatter(page, prefix = prefix))
        }
    }

    private fun fetchDelayed(url: UrlAware, delay: Duration) {
        val globalCache = globalCacheFactory?.globalCache ?: return

        val delayCache = globalCache.urlPool.delayCache
        // erase -refresh options
//        url.args = url.args?.replace("-refresh", "-refresh-erased")
        url.args = url.args?.let { LoadOptions.eraseOptions(it, "refresh") }
        require(url.args?.contains("refresh") != true)

        delayCache.add(DelayUrl(url, delay))
    }

    private fun handleMemoryShortage(j: Int) {
        logger.info(
            "$j.\tnumRunning: {}, availableMemory: {}, memoryToReserve: {}, shortage: {}",
            globalRunningTasks,
            Strings.readableBytes(availableMemory),
            Strings.readableBytes(memoryToReserve.toLong()),
            Strings.readableBytes(availableMemory - memoryToReserve.toLong())
        )
        session.globalCacheFactory.globalCache.clearPDCaches()
        System.gc()
    }

    /**
     * Proxies should live for more than 5 minutes.
     * If proxy is not enabled, the rate is always 0.
     *
     * 5 / 60f = 0.083
     * */
    private suspend fun handleContextLeaks() {
        val contextLeaks = PrivacyContext.globalMetrics.contextLeaks
        val contextLeaksRate = contextLeaks.meter.fifteenMinuteRate
        var k = 0
        while (isActive && contextLeaksRate >= 5 / 60f && ++k < 600) {
            logger.takeIf { k % 60 == 0 }?.warn(
                    "Context leaks too fast: {} leaks/seconds, available memory: {}",
                    contextLeaksRate, Strings.readableBytes(availableMemory))
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
            logger.warn("Proxy out of service, check it again ...")
            val p = proxyPool
            if (p != null) {
                p.runCatching { take() }.onFailure {
                    if (it !is ProxyInsufficientBalanceException) {
                        proxyOutOfService = 0
                    } else {
                        logger.warn("Proxy account insufficient balance")
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

    private fun <T> runSafely(name: String, action: () -> T?): T? {
        if (!isActive) {
            return null
        }

        try {
            return action()
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }

        return null
    }
}
