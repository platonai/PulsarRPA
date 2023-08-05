package ai.platon.pulsar.crawl.impl

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.collect.ConcurrentLoadingIterable
import ai.platon.pulsar.common.collect.DelayUrl
import ai.platon.pulsar.common.config.AppConstants.FETCH_TASK_TIMEOUT_DEFAULT
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.emoji.PopularEmoji
import ai.platon.pulsar.common.measure.ByteUnitConverter
import ai.platon.pulsar.common.message.PageLoadStatusFormatter
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyInsufficientBalanceException
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.common.urls.CallableDegenerateUrl
import ai.platon.pulsar.common.urls.DegenerateUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.common.url.ListenableUrl
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.persist.WebDBException
import ai.platon.pulsar.persist.WebPage
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
import kotlin.random.Random

private class StreamingCrawlerMetrics {
    private val registry = MetricsSystem.defaultMetricRegistry

    val retries = registry.multiMetric(this, "retries")
    val cancels = registry.multiMetric(this, "cancels")
    val gone = registry.multiMetric(this, "gone")
    val tasks = registry.multiMetric(this, "tasks")
    val successes = registry.multiMetric(this, "successes")
    val finishes = registry.multiMetric(this, "finishes")

    val fetchSuccesses = registry.multiMetric(this, "fetchSuccesses")

    val drops = registry.meter(this, "drops")
    val timeouts = registry.meter(this, "timeouts")
}

private enum class CriticalWarning(val message: String) {
    HIGH_CPU_LOAD("HIGH CPU LOAD"),
    OUT_OF_MEMORY("OUT OF MEMORY"),
    OUT_OF_DISK_STORAGE("OUT OF DISK STORAGE"),
    WEB_DB_LOST("WEB DB LOST"),
    NO_PROXY("NO PROXY AVAILABLE"),
    FAST_CONTEXT_LEAK("CONTEXT LEAK TOO FAST"),
    FAST_CANCELS("CANCELS TOO FAST"),
    WRONG_DISTRICT("WRONG DISTRICT! ALL RESIDENT TASKS ARE PAUSED"),
    WRONG_PROFILE("WRONG PROFILE! ALL RESIDENT TASKS ARE PAUSED"),
}

open class StreamingCrawler(
    /**
     * The url sequence
     * */
    var urls: Sequence<UrlAware>,
    /**
     * The default pulsar session to use
     * */
    session: PulsarSession = PulsarContexts.createSession(),
    /**
     * Do not use proxy
     * */
    val noProxy: Boolean = true,
    /**
     * Auto close or not
     * */
    autoClose: Boolean = true,
): AbstractCrawler(session, autoClose) {
    companion object {
        private val globalRunningInstances = AtomicInteger()
        private val globalRunningTasks = AtomicInteger()
        private val globalKilledTasks = AtomicInteger()
        private val globalTasks = AtomicInteger()
        private var globalWebDBFailures = AtomicInteger()

        private val globalMetrics = StreamingCrawlerMetrics()

        private val globalLoadingUrls = ConcurrentSkipListSet<String>()

        private var contextLeakWaitingTime = Duration.ZERO
        private var proxyVendorWaitingTime = Duration.ZERO
        private var criticalWarning: CriticalWarning? = null
        private var lastUrl = ""
        private var lastHtmlIntegrity = ""
        private var lastFetchError = ""
        private val lastCancelReason = Frequency<String>()
        private val isIllegalApplicationState = AtomicBoolean()

        /**
         * TODO: change to wrong profile
         * */
        private var wrongDistrict = MetricsSystem.reg.multiMetric(this, "WRONG_DISTRICT_COUNT")
        private var wrongProfile = MetricsSystem.reg.multiMetric(this, "WRONG_PROFILE_COUNT")

        private val readableCriticalWarning: String
            get() = criticalWarning?.message?.let { "!!! WARNING !!! $it !!! ${Instant.now()}" } ?: ""

        init {
            mapOf(
                "globalRunningInstances" to Gauge { globalRunningInstances.get() },
                "globalRunningTasks" to Gauge { globalRunningTasks.get() },
                "globalKilledTasks" to Gauge { globalKilledTasks.get() },
                "globalWebDBFailures" to Gauge { globalWebDBFailures.get() },

                "contextLeakWaitingTime" to Gauge { contextLeakWaitingTime },
                "proxyVendorWaitingTime" to Gauge { proxyVendorWaitingTime },
                "000WARNING" to Gauge { readableCriticalWarning },
                "lastCancelReason" to Gauge { lastCancelReason.toString() },

                "lastUrl" to Gauge { lastUrl },
                "lastHtmlIntegrity" to Gauge { lastHtmlIntegrity },
                "lastFetchError" to Gauge { lastFetchError },
            ).let { MetricsSystem.reg.registerAll(this, it) }
        }
    }

    private val logger = getLogger(StreamingCrawler::class)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    private val taskLogger = getLogger(StreamingCrawler::class, ".Task")
    private val conf = session.sessionConfig
    private val context = session.context as AbstractPulsarContext
    private val globalCache get() = session.globalCache
    private val globalCacheOrNull get() = if (isActive) session.globalCache else null
    private val proxyPool: ProxyPool? = if (noProxy) null else context.getBeanOrNull(ProxyPool::class)
    private var proxyOutOfService = 0

    @Volatile
    private var flowState = FlowState.CONTINUE

    private var lastActiveTime = Instant.now()

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val gauges = mapOf(
        "idleTime" to Gauge { idleTime.readable() },
        "numPrivacyContexts" to Gauge { numPrivacyContexts },
        "numMaxActiveTabs" to Gauge { numMaxActiveTabs },
        "fetchConcurrency" to Gauge { fetchConcurrency },
    )

    private var forceQuit = false

    /**
     * The maximum number of privacy contexts allowed.
     * */
    val numPrivacyContexts get() = conf.getInt(PRIVACY_CONTEXT_NUMBER, 2)

    /**
     * The maximum number of open tabs allowed in each open browser.
     * */
    val numMaxActiveTabs get() = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)

    /**
     * The fetch concurrency equals to the number of all allowed open tabs.
     * */
    val fetchConcurrency get() = numPrivacyContexts * numMaxActiveTabs

    /**
     * The out of work timeout.
     * */
    val outOfWorkTimeout = Duration.ofMinutes(10)

    /**
     * The timeout for each fetch task.
     * */
    val fetchTaskTimeout get() = conf.getDuration(FETCH_TASK_TIMEOUT, FETCH_TASK_TIMEOUT_DEFAULT)

    /**
     * The idle time during which there is no fetch tasks.
     * */
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())

    /**
     * Check if the crawler is out of work (idle for long time).
     * */
    val isOutOfWork get() = idleTime > outOfWorkTimeout

    /**
     * Check if smart retry is enabled.
     *
     * If smart retry is enabled, tasks will be retried if it's canceled or marked as retry.
     * A continuous crawl system should enable smart retry, while a simple demo can disable it.
     * */
    val isSmartRetryEnabled get() = conf.getBoolean(CRAWL_SMART_RETRY, true)

    /**
     * Check if the crawler is idle.
     * An idle crawler means:
     * 1. no loading urls
     * 2. no urls in the loading queue
     * */
    val isIdle: Boolean
        get() {
            return !urls.iterator().hasNext() && globalLoadingUrls.isEmpty()
                    && idleTime > Duration.ofSeconds(5)
        }

    /**
     * Check if the crawler is active.
     * */
    override val isActive get() = super.isActive && !forceQuit && !isIllegalApplicationState.get()

    /**
     * The job name.
     * */
    var jobName: String = "crawler-" + RandomStringUtils.randomAlphanumeric(5)

    init {
        MetricsSystem.reg.registerAll(this, "$id.g", gauges)

        val cacheGauges = mapOf(
            "pageCacheSize" to Gauge { globalCacheOrNull?.pageCache?.size?: 0 },
            "documentCacheSize" to Gauge { globalCacheOrNull?.documentCache?.size?: 0 }
        )
        MetricsSystem.reg.registerAll(this, "$id.g", cacheGauges)

        generateFinishCommand()
    }

    /**
     * Run the crawler.
     * */
    open fun run() {
        runBlocking {
            supervisorScope {
                run(this)
            }
        }
    }

    /**
     * Run the crawler in the given coroutine scope.
     * */
    open suspend fun run(scope: CoroutineScope) {
        startCrawlLoop(scope)
    }

    /**
     * Wait until all tasks are done.
     * */
    override fun await() {
        lock.withLock { notBusy.await() }
    }

    /**
     * Quit the crawl loop.
     * */
    fun quit() {
        forceQuit = true
    }

    /**
     * Quit and close the crawl loop.
     * */
    override fun close() {
        quit()
        super.close()
    }

    protected suspend fun startCrawlLoop(scope: CoroutineScope) {
        logger.info("Starting {} #{} ...", name, id)

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

                // The largest disk must have at least 10 GiB remaining space
                val freeSpace =
                    Runtimes.unallocatedDiskSpaces().maxOfOrNull { ByteUnitConverter.convert(it, "G") } ?: 0.0
                if (freeSpace < 10.0) {
                    logger.error("Disk space is full!")
                    criticalWarning = CriticalWarning.OUT_OF_DISK_STORAGE
                    return@startCrawlLoop
                }

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

        delayIfEstimatedNoLoadResource(j)

        while (isActive && AppSystemInfo.isCriticalCPULoad) {
            criticalWarning = CriticalWarning.HIGH_CPU_LOAD
            // CPU load changes very fast, it drops immediately when a web driver becomes free,
            // so we delay for short and random time.
            randomDelay(200, 300)
        }

        /**
         * If all memory is used up, we can do nothing but wait.
         * */
        var k = 0
        while (isActive && AppSystemInfo.isCriticalMemory) {
            if (k++ % 20 == 0) {
                handleMemoryShortage(k)
            }
            criticalWarning = CriticalWarning.OUT_OF_MEMORY
            randomDelay(500, 500)
        }

        /**
         * If the privacy context leaks too quickly, there is a good chance that there is a bug.
         * */
        val contextLeaksRate = PrivacyContext.globalMetrics.contextLeaks.meter.fifteenMinuteRate
        if (isActive && contextLeaksRate >= 5 / 60f) {
            criticalWarning = CriticalWarning.FAST_CONTEXT_LEAK
            handleContextLeaks()
        }

        if (isActive && wrongProfile.hourlyCounter.count > 60) {
            handleWrongProfile()
        } else if (isActive && wrongDistrict.hourlyCounter.count > 60) {
            // since profile contains district setting, handleWrongDistrict will be removed
            handleWrongDistrict()
        }

        if (isActive && proxyOutOfService > 0) {
            criticalWarning = CriticalWarning.NO_PROXY
            handleProxyOutOfService()
        }

        if (isActive && globalWebDBFailures.get() > 0) {
            criticalWarning = CriticalWarning.WEB_DB_LOST
            handleWebDBLost()
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

        delayIfEstimatedNoLoadResource(j)

        criticalWarning = null

        val context = Dispatchers.Default + CoroutineName("w")
        val urlSpec = UrlUtils.splitUrlArgs(url.url).first
        // We must increase the number before the task is actually launched in a coroutine,
        // otherwise, it's easy to grow larger than fetchConcurrency.
        globalRunningTasks.incrementAndGet()
        scope.launch(context) {
            try {
                globalMetrics.tasks.mark()
                runTaskWithEventHandlers(url)
            } finally {
                lastActiveTime = Instant.now()

                globalLoadingUrls.remove(urlSpec)
                globalRunningTasks.decrementAndGet()

                globalMetrics.finishes.mark()
            }
        }

        return flowState
    }

    /**
     * Delay if there is no resource to load a new task.
     * Running task has to be no more than the available web drivers.
     * */
    private suspend fun delayIfEstimatedNoLoadResource(j: Int, maxTry: Int = 1000) {
        var k = 0
        while (isActive && ++k < maxTry && globalRunningTasks.get() >= fetchConcurrency) {
            if (j % 120 == 0) {
                logger.info(
                    "$j. Long time to run $globalRunningTasks tasks | $lastActiveTime -> {}",
                    idleTime.readable()
                )
            }
            val timeMillis = 500L + Random.nextInt(500)
            delay(timeMillis)
        }
    }

    private suspend fun runTaskWithEventHandlers(url: UrlAware) {
        when {
            !isActive -> {
                return
            }
            url is DegenerateUrl -> {
                runDegenerateUrlTask(url)
            }
            else -> {
                loadWithEventHandlers(url)
            }
        }
    }

    private fun runDegenerateUrlTask(url: DegenerateUrl) {
        when (url) {
            is CallableDegenerateUrl -> {
                url.invoke()
            }

            is ListenableUrl -> {
                emit(CrawlEvents.willLoad, url)
                // The url is degenerated, which means it's not a resource on the Internet but a normal executable task.
                emit(CrawlEvents.load, url)
                emit(CrawlEvents.loaded, url, null)
            }
        }
    }

    private suspend fun loadWithEventHandlers(url: UrlAware) {
        emit(CrawlEvents.willLoad, url)

        val page = loadWithTimeout(url)

        // A continuous crawl system should enable smart retry, while a simple demo can disable it
        if (isSmartRetryEnabled) {
            handleRetry(url, page)
        }

        if (page != null) {
            collectStatAfterLoad(page)
        }

        emit(CrawlEvents.loaded, url, page)
    }

    private suspend fun loadWithTimeout(url: UrlAware): WebPage? {
        var page: WebPage? = null
        val timeout = fetchTaskTimeout.plusSeconds(30).toMillis()
        try {
            page = withTimeout(timeout) {
                loadWithMinorExceptionHandled(url)
            }
        } catch (e: TimeoutCancellationException) {
            globalMetrics.timeouts.mark()
            logger.info(
                "{}. Task timeout ({}) to load page, thrown by [withTimeout] | {}",
                globalMetrics.timeouts.count, timeout, url
            )
        } catch (e: Throwable) {
            when {
                // The following exceptions can be caught as a Throwable but not the concrete exception,
                // one of the reason is the concrete exception is not public.
                e.javaClass.name == "kotlinx.coroutines.JobCancellationException" -> {
                    if (isIllegalApplicationState.compareAndSet(false, true)) {
                        logger.warn("Coroutine was cancelled, quit... (JobCancellationException)")
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
        if (page.isCanceled) {
            return
        }

        lastFetchError = page.protocolStatus.takeIf { !it.isSuccess }?.toString() ?: ""
        if (!page.protocolStatus.isSuccess) {
            return
        }

        lastUrl = page.configuredUrl
        lastHtmlIntegrity = page.htmlIntegrity.toString()


        if (page.htmlIntegrity.isWrongProfile) {
            wrongProfile.mark()
        } else {
            wrongProfile.reset()
        }

        if (!page.htmlIntegrity.isWrongProfile) {
            // TODO: since profile contains district setting, this will be removed later
            if (page.htmlIntegrity == HtmlIntegrity.WRONG_DISTRICT) {
                wrongDistrict.mark()
            } else {
                wrongDistrict.reset()
            }
        }

        if (page.isFetched) {
            globalMetrics.fetchSuccesses.mark()
        }

        globalMetrics.successes.mark()
    }

    /**
     * Find a proper strategy to retry the task.
     * */
    private suspend fun handleRetry(url: UrlAware, page: WebPage?) {
        when {
            !isActive -> return
            page == null -> handleRetry0(url, null)
            page.isCanceled -> handleCanceled(url, page)
            // TODO: keep consistency with protocolStatus.isRetry and crawlStatus.isRetry
            page.protocolStatus.isRetry -> handleRetry0(url, page)
            page.crawlStatus.isRetry -> handleRetry0(url, page)
            page.crawlStatus.isGone -> {
                globalMetrics.gone.mark()
                taskLogger.info("{}", PageLoadStatusFormatter(page, prefix = "Gone"))
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun loadWithMinorExceptionHandled(url: UrlAware): WebPage? {
        val options = session.options(url.args ?: "")
        if (options.isDead()) {
            // The url is dead, drop the task
            globalKilledTasks.incrementAndGet()
            return null
        }

        // TODO: use the code below, to avoid option creation, which leads to too complex option merging
//        if (url.deadline > Instant.now()) {
//            session.loadDeferred(url)
//        }

        return kotlin.runCatching { session.loadDeferred(url, options) }
            .onSuccess { flowState = handleLoadSuccess(url, it) }
            .onFailure { flowState = handleLoadException(url, it) }
            .getOrNull()
    }

    @Throws(Exception::class)
    private fun handleLoadSuccess(url: UrlAware, page: WebPage): FlowState {
//        if (globalWebDBFailures.get() > 0 && globalWebDBFailures.decrementAndGet() < 0) {
//            globalWebDBFailures.set(0)
//        }
        return FlowState.CONTINUE
    }

    @Throws(Exception::class)
    private fun handleLoadException(url: UrlAware, e: Throwable): FlowState {
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

            is ProxyInsufficientBalanceException -> {
                proxyOutOfService++
                logger.warn("{}. {}", proxyOutOfService, e.message)
            }

            is ProxyVendorUntrustedException -> {
                logger.warn("Proxy is untrusted | {}", e.message)
                return FlowState.BREAK
            }

            is ProxyException -> {
                logger.warn("[Unexpected] proxy exception | {}", e.brief())
            }

            is WebDBException -> {
                globalWebDBFailures.incrementAndGet()
                // logger.warn("Web DB failure | {} | the Web DB layer should have reported the detail", continousWebDBFailureCount)
            }

            is TimeoutCancellationException -> {
                logger.warn(
                    "[Timeout] Coroutine was cancelled, thrown by [withTimeout]. {} | {}",
                    e.brief(), url
                )
            }

            is CancellationException -> {
                // Has to come after TimeoutCancellationException
                if (isIllegalApplicationState.compareAndSet(false, true)) {
                    logger.warn("Streaming crawler job was canceled, quit ...", e)
                }
                return FlowState.BREAK
            }

            is IllegalStateException -> {
                logger.warn("Illegal state", e)
            }

            else -> throw e
        }

        return FlowState.CONTINUE
    }

    private fun doLaterIfProcessing(urlSpec: String, url: UrlAware, delay: Duration): Boolean {
        if (urlSpec in globalLoadingUrls || urlSpec in globalCache.fetchingCache) {
            // process later, hope the page is fetched
            logger.debug("Task is in process, do it {} later | {}", delay.readable(), url.configuredUrl)
            fetchDelayed(url, delay)
            return true
        }

        return false
    }

    private suspend fun handleCanceled(url: UrlAware, page: WebPage?) {
        globalMetrics.cancels.mark()
        val delay = page?.retryDelay?.takeIf { !it.isZero } ?: Duration.ofSeconds(10)
        // Delay fetching the page.
        fetchDelayed(url, delay)

        // Collect all cancel reasons
        if (page != null) {
            // page is not updated using page datum if the page is canceled, so use pageDatum
            val reason = page.pageDatum?.protocolStatus?.reason ?: "unknown"
            lastCancelReason.add(reason.toString())
        }

        // Set a guard to prevent too many cancels.
        // If there are too many cancels, the loop should have a rest.
        //
        // rate_unit=events/second
        val oneMinuteRate = globalMetrics.cancels.meter.oneMinuteRate
        if (isActive && oneMinuteRate >= 1.0) {
            criticalWarning = CriticalWarning.FAST_CANCELS
            delay(1_000)
        }
    }

    private fun handleRetry0(url: UrlAware, page: WebPage?) {
        val nextRetryNumber = 1 + (page?.fetchRetries ?: 0)
        if (page != null && nextRetryNumber > page.maxRetries) {
            // should not go here, because the page should be marked as GONE
            globalMetrics.gone.mark()
            taskLogger.info("{}", PageLoadStatusFormatter(page, prefix = "Gone (unexpected)"))
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
            val symbol = PopularEmoji.FENCER
            val prefix = "$symbol Trying ${nextRetryNumber}th ${delay.readable()} later | "
            taskLogger.info("{}", PageLoadStatusFormatter(page, prefix = prefix))
        }
    }

    private fun fetchDelayed(url: UrlAware, delay: Duration) {
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
            globalRunningTasks, AppSystemInfo.formatAvailableMemory(),
            Strings.compactFormat(AppSystemInfo.actualCriticalMemory.toLong()),
            AppSystemInfo.formatMemoryShortage()
        )
        session.globalCache.clearPDCaches()

        // When control returns from the method call, the Java Virtual Machine
        // has made the best effort to reclaim space from all unused objects.
        System.gc()
    }
    
    /**
     * Proxies should live for more than 5 minutes. If proxy is not enabled, the rate is always 0.
     *
     * 5 / 60f ~= 0.083
     * */
    private suspend fun handleContextLeaks() {
        val contextLeaks = PrivacyContext.globalMetrics.contextLeaks
        val contextLeaksRate = contextLeaks.meter.fifteenMinuteRate
        var k = 0
        while (isActive && contextLeaksRate >= 5 / 60f && ++k < 600) {
            logger.takeIf { k % 60 == 0 }?.warn(
                    "Context leaks too fast: {} leaks/seconds, available memory: {}",
                    contextLeaksRate, AppSystemInfo.formatAvailableMemory())
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

    private suspend fun handleWebDBLost() {
        val startTime = Instant.now()
        var lastReportTime = Instant.EPOCH
        var canConnect = false
        while (isActive && !canConnect) {
            canConnect = context.webDb.canConnect()
            if (!canConnect) {
                val elapsedTime = DateTimes.elapsedTime(startTime)
                val elapsedSeconds = elapsedTime.seconds
                val delaySeconds = elapsedSeconds.coerceAtLeast(5).coerceAtMost(30)
                val delayInterval = Duration.ofSeconds(delaySeconds)

                val reportInterval = when {
                    elapsedSeconds < 30 -> Duration.ofSeconds(30)
                    elapsedSeconds < 600 -> Duration.ofMinutes(1)
                    else -> Duration.ofMinutes(2)
                }
                if (DateTimes.elapsedTime(lastReportTime) > reportInterval) {
                    logger.warn("Web DB lost for {}", elapsedTime.readable())
                    lastReportTime = Instant.now()
                }

                delay(delayInterval.toMillis())
            }
        }

        if (canConnect) {
            globalWebDBFailures.set(0)
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

    private suspend fun handleWrongProfile() {
        var k = 0
        while (wrongProfile.hourlyCounter.count > 60) {
            criticalWarning = CriticalWarning.WRONG_PROFILE
            logger.takeIf { k++ % 20 == 0 }?.warn("{}", criticalWarning?.message ?: "")
            delay(1000)
        }
    }

    private suspend fun randomDelay(baseMills: Int, randomDelta: Int) = delay(baseMills.toLong() + Random.nextInt(randomDelta))

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
