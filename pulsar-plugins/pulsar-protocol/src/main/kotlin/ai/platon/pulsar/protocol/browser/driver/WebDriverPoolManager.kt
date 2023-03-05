package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.persist.ext.browseEvent
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import com.codahale.metrics.Gauge
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WebDriverTask (
        val browserId: BrowserId,
        val page: WebPage,
        val priority: Int = 0,
        val driverFun: suspend (driver: WebDriver) -> FetchResult?
) {
    companion object {
        private val sequencer = AtomicInteger()
    }

    val id = sequencer.incrementAndGet()
    val volatileConfig get() = page.conf
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class WebDriverPoolManager(
    val browserManager: BrowserManager,
    val driverFactory: WebDriverFactory,
    val immutableConfig: ImmutableConfig,
    val suppressMetrics: Boolean = false
): Parameterized, PreemptChannelSupport("WebDriverPoolManager"), AutoCloseable {
    companion object {
        val DRIVER_CLOSE_TIME_OUT = Duration.ofSeconds(60)
    }

    private val logger = LoggerFactory.getLogger(WebDriverPoolManager::class.java)
    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get() && AppContext.isActive

    /**
     * Working driver pools
     * */
    private val _workingDriverPools = ConcurrentSkipListMap<BrowserId, LoadingWebDriverPool>()

    /**
     * Retired but not closed driver pools
     * */
    private val _retiredDriverPools = ConcurrentSkipListMap<BrowserId, LoadingWebDriverPool>()

    /**
     * Closed driver pool ids
     * */
    private val _closedDriverPools = ConcurrentSkipListSet<BrowserId>()

    /**
     * The deferred tasks
     * */
    private val _deferredTasks = ConcurrentSkipListMap<Int, Deferred<FetchResult?>>()

    val driverSettings get() = driverFactory.driverSettings
    val idleTimeout = Duration.ofMinutes(18)

    val workingDriverPools: Map<BrowserId, LoadingWebDriverPool> get() = _workingDriverPools

    val retiredDriverPools: Map<BrowserId, LoadingWebDriverPool> get() = _retiredDriverPools

    private val driverPoolCloser = DriverPoolCloser(this)

    val closedDriverPools: Set<BrowserId> get() = _closedDriverPools

    val startTime = Instant.now()
    var lastActiveTime = startTime
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = idleTime > idleTimeout

    val numReset by lazy { AppMetrics.reg.meter(this, "numReset") }
    val numTimeout by lazy { AppMetrics.reg.meter(this, "numTimeout") }
    val gauges = mapOf(
        "waitingTasks" to Gauge { numWaitingTasks },
        "standbyDrivers" to Gauge { numStandbyDrivers },
        "workingDrivers" to Gauge { numWorkingDrivers },
        "availableDriverSlots" to Gauge { numAvailableDriverSlots },
        "activeDrivers" to Gauge { numActiveDrivers },
        "dyingDrivers" to Gauge { numDyingDrivers },
        "closedDrivers" to Gauge { numClosedDrivers },
        "pTasks" to Gauge { numPreemptiveTasks.get() },
        "runningPTasks" to Gauge { numRunningPreemptiveTasks.get() },
        "pendingNTasks" to Gauge { numPendingNormalTasks.get() },
        "runningNTasks" to Gauge { numRunningNormalTasks.get() },
        "idleTime" to Gauge { idleTime.readable() }
    ).takeUnless { suppressMetrics }

    val numWaitingTasks get() = workingDriverPools.values.sumOf { it.numWaiting }
    val numStandbyDrivers get() = workingDriverPools.values.sumOf { it.numStandby }
    val numWorkingDrivers get() = workingDriverPools.values.sumOf { it.numWorking }
    val numAvailableDriverSlots get() = workingDriverPools.values.sumOf { it.numDriverSlots }
    val numActiveDrivers get() = workingDriverPools.values.sumOf { it.activeDrivers.size }

    val numDyingDrivers get() = retiredDriverPools.values.sumOf { it.numCreated }

    val numClosedDrivers get() = closedDriverPools.size

    //    private val launchLock = ReentrantLock()
    private val launchMutex = Mutex()

    // Maximum allowed number of retired drives, if it's exceeded, the oldest driver pool should be closed.
    private val maxAllowedDyingDrivers = 10

    private val lastMaintainTime = Instant.now()
    private val minMaintainInterval = Duration.ofSeconds(10)
    private val tooFrequentMaintenance get() = DateTimes.elapsedTime(lastMaintainTime) < minMaintainInterval

    init {
        gauges?.let { AppMetrics.reg.registerAll(this, it) }
    }

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun run(task: FetchTask, browseFun: suspend (driver: WebDriver) -> FetchResult?) =
        run(WebDriverTask(BrowserId.DEFAULT, task.page, task.priority, browseFun))

    /**
     * TODO: consider pro-actor model instead
     *
     * reactor: tell me if you can do this job
     * pro-actor: here is a job, tell me if you finished it
     *
     * @return The result of action, or null if timeout
     * */
    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun run(
        browserId: BrowserId, task: FetchTask,
        browseFun: suspend (driver: WebDriver) -> FetchResult?
    ) = run(WebDriverTask(browserId, task.page, task.priority, browseFun))

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun run(task: WebDriverTask): FetchResult? {
        lastActiveTime = Instant.now()
        return run0(task).also { lastActiveTime = Instant.now() }
    }

    @Throws(CancellationException::class)
    suspend fun runCancelable(task: WebDriverTask, driver: WebDriver): FetchResult? {
        val deferred = supervisorScope {
            // Creates a coroutine and returns its future result
            // The running coroutine is cancelled when the resulting deferred is cancelled
            async { task.driverFun(driver) }
        }

        _deferredTasks[task.id] = deferred
        // Awaits for completion of this value without blocking a thread and resumes
        // when deferred computation is complete, returning the resulting value or throwing
        // the corresponding exception if the deferred was cancelled.
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            logger.info("Coroutine cancelled, return null result | {}", e.message)
            null
        } finally {
            _deferredTasks.remove(task.id)
        }
    }

    /**
     * Create a driver pool, but the driver pool is not added to [workingDriverPools].
     * */
    fun createUnmanagedDriverPool(
        browserId: BrowserId = BrowserId.DEFAULT, priority: Int = 0
    ): LoadingWebDriverPool {
        return LoadingWebDriverPool(browserId, priority, driverFactory, immutableConfig)
    }

    fun isRetiredPool(browserId: BrowserId) = retiredDriverPools.contains(browserId)

    /**
     * Cancel the fetch task specified by [url] remotely.
     * NOTE: A cancel request should run immediately not waiting for any browser task return.
     * */
    fun cancel(url: String): WebDriver? {
        var driver: WebDriver? = null
        workingDriverPools.values.forEach { driverPool ->
            driver = driverPool.activeDrivers.lastOrNull { it.navigateEntry.pageUrl == url }?.also {
                it.cancel()
            }
        }
        return driver
    }

    /**
     * Cancel the fetch task specified by [url] remotely.
     * NOTE: A cancel request should run immediately not waiting for any browser task return.
     * */
    fun cancel(browserId: BrowserId, url: String): WebDriver? {
        val driverPool = workingDriverPools[browserId] ?: return null
        return driverPool.firstOrNull { it.navigateEntry.pageUrl == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages.
     * */
    fun cancelAll() {
        workingDriverPools.values.forEach { it.cancelAll() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages.
     * */
    fun cancelAll(browserId: BrowserId) {
        val driverPool = workingDriverPools[browserId] ?: return
        driverPool.cancelAll()
    }

    /**
     * Find the driver pool with [browserId], cancel all running tasks in it, and close all drivers in it.
     *
     * When we close a driver pool, all threads that are trying to get a driver should wait.
     * */
    fun closeDriverPoolGracefully(browserId: BrowserId, timeToWait: Duration) {
        numReset.mark()

        // Preempt the channel to ensure consistency.
        // Wait until there is no normal tasks, and if there is at least one preemptive task
        // in the critical section, all normal tasks have to wait.
        preempt {
            driverPoolCloser.closeDriverPoolGracefully(browserId)
        }
    }

    /**
     * Maintain the driver pools
     * */
    fun maintain() {
        if (tooFrequentMaintenance) {
            return
        }

        // To close retired driver pools, there is no need to wait for normal tasks, so no preempting is required
        driverPoolCloser.closeOldestRetiredDriverPoolSafely()
        // Preempt the channel to ensure consistency
        preempt {
            driverPoolCloser.closeIdleDriverPoolsSafely()
        }

        // maintain in a separate monitor
//        browserManager.maintain()
    }

    fun report(browserId: BrowserId, verbose: Boolean = false): String {
        return _workingDriverPools[browserId]?.report(verbose) ?: ""
    }

    /**
     * Closing call stack:
     *
     * PrivacyContextManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close
     *
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _deferredTasks.values.forEach {
                // Cancels job, including all its children with a specified diagnostic error
                it.cancel("This program is closed")
            }
            _deferredTasks.clear()

            _workingDriverPools.values.forEach { it.close() }
            _workingDriverPools.clear()

            browserManager.close()

            logger.info("Web driver pool manager is closed")
        }
    }

    override fun toString(): String = report(false)

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    private suspend fun run0(task: WebDriverTask) = runWithPolledDriver(task)

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    private suspend fun runWithSpecifiedDriver(task: WebDriverTask, driver: WebDriver) =
        runCancelableWithTimeout(task, driver)

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    private suspend fun runWithPolledDriver(task: WebDriverTask): FetchResult? {
        val browserId = task.browserId
        var result: FetchResult? = null
        whenNormalDeferred {
            if (!isActive) {
                return@whenNormalDeferred null
            }

            if (isRetiredPool(browserId)) {
                throw WebDriverPoolException("Web driver pool is retired | $browserId")
            }

            val driverPool = computeDriverPoolIfAbsent(browserId, task)
            if (!driverPool.isActive) {
                throw WebDriverPoolException("Driver pool is already closed | $driverPool | $browserId")
            }

            var driver: WebDriver? = null
            try {
                // Mutual exclusion for coroutines.
                driver = launchMutex.withLock {
                    poll(driverPool, task)
                }

                result = runCancelableWithTimeout(task, driver)
            } finally {
                driver?.let { driverPool.put(it) }
            }
        }

//        kotlin.runCatching { maintain() }.onFailure { logger.warn(it.stringify()) }

        return result
    }

    private suspend fun runCancelableWithTimeout(task: WebDriverTask, driver: WebDriver): FetchResult? {
        // do not take up too much time on this driver
        val fetchTaskTimeout = driverSettings.fetchTaskTimeout
//        val result = withTimeoutOrNull(fetchTaskTimeout.toMillis()) {
//            try {
//                runCancelable(task, driver)
//            } catch (e: CancellationException) {
//                null
//            }
//        }

        return try {
            // The code that is executing inside the [block] is cancelled on timeout.
            withTimeout(fetchTaskTimeout.toMillis()) {
                runCancelable(task, driver)
            }
        } catch (e: TimeoutCancellationException) {
            numTimeout.mark()
            val browserId = driver.browser.id
            logger.warn(
                "Coroutine canceled({}) (by [withTimeout]) | {} | {}",
                fetchTaskTimeout.readable(), report(browserId), browserId
            )
            null
        } finally {
            _deferredTasks.remove(task.id)
        }
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    private suspend fun poll(driverPool: LoadingWebDriverPool, task: WebDriverTask): WebDriver {
        val notLaunched = driverPool.launched.compareAndSet(false, true)
        return if (notLaunched) {
            launchAndPoll(driverPool, task)
        } else {
            pollWebDriver(driverPool, task)
        }
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    private suspend fun launchAndPoll(driverPool: LoadingWebDriverPool, task: WebDriverTask): WebDriver {
        val page = task.page
        val event = page.browseEvent

        dispatchEvent("onWillLaunchBrowser") { event?.onWillLaunchBrowser?.invoke(page) }

        return pollWebDriver(driverPool, task).also { driver ->
            dispatchEvent("onBrowserLaunched") { event?.onBrowserLaunched?.invoke(page, driver) }
        }
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    private fun pollWebDriver(driverPool: LoadingWebDriverPool, task: WebDriverTask): WebDriver {
        val timeout = driverSettings.pollingDriverTimeout
        val driver = driverPool.poll(task.priority, task.volatileConfig, timeout)
        require(driver.isWorking)
        return driver
    }

    @Synchronized
    private fun computeDriverPoolIfAbsent(browserId: BrowserId, task: WebDriverTask): LoadingWebDriverPool {
        return _workingDriverPools.computeIfAbsent(browserId) { createUnmanagedDriverPool(browserId, task.priority) }
    }

    private fun report(verbose: Boolean = false): String {
        val sb = StringBuilder()
        if (workingDriverPools.isNotEmpty()) {
            workingDriverPools.entries.joinTo(sb, "\n") { it.value.report(verbose) + " | " + it.key }
        }
        return sb.toString()
    }

    private suspend fun dispatchEvent(name: String, action: suspend () -> Unit) {
        if (!isActive) {
            return
        }

        try {
            action()
        } catch (e: WebDriverCancellationException) {
            logger.info("Web driver is cancelled")
        } catch (e: WebDriverException) {
            logger.warn(e.brief("[Ignored][$name] "))
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }
    }

    /**
     * A helper class to close the driver pool and the associated browser safely
     * */
    private class DriverPoolCloser(private val driverPoolManager: WebDriverPoolManager) {
        private val driverSettings get() = driverPoolManager.driverSettings
        private val workingDriverPools get() = driverPoolManager.workingDriverPools

        private val retiredDriverPools get() = driverPoolManager.retiredDriverPools

        private val browserManager get() = driverPoolManager.browserManager

        private val logger = getLogger(this)

        @Synchronized
        fun closeDriverPoolGracefully(browserId: BrowserId) {
            val retiredDriverPool = driverPoolManager._workingDriverPools[browserId]

            // TODO: configurable
            val isBusySystem = alwaysTrue()
            val diagnosis = !isBusySystem

            if (retiredDriverPool != null) {
                if (diagnosis) {
                    openInformationPages(browserId)
                }

                retireDriverPool(browserId, retiredDriverPool)

                // if diagnosis is not needed, close the browser immediately
                if (!diagnosis) {
                    closeBrowserAndDriverPool(retiredDriverPool)
                }
            }

            if (diagnosis) {
                closeLeastValuableDriverPool(browserId, retiredDriverPool)
            }
        }

        @Synchronized
        fun closeOldestRetiredDriverPoolSafely() {
            val dyingDriverPool = findOldestRetiredDriverPoolOrNull()

            if (dyingDriverPool != null) {
                closeBrowserAndDriverPool(dyingDriverPool)
            }
        }

        @Synchronized
        fun closeIdleDriverPoolsSafely() {
            workingDriverPools.values.filter { it.isIdle }.forEach {
                logger.info("Driver pool is idle, closing it ... | {}", it.browserId)
                closeDriverPool(it)
            }
        }

        private fun retireDriverPool(browserId: BrowserId, driverPool: LoadingWebDriverPool) {
            // Force the page to stop all navigations and RELEASE all resources.
            // mark the driver pool be retired, but not closed yet
            driverPool.retire()
            driverPoolManager._retiredDriverPools[browserId] = driverPool
            driverPoolManager._workingDriverPools.remove(browserId)
        }

        private fun openInformationPages(browserId: BrowserId) {
            if (!driverPoolManager.isActive) {
                // do not say anything to a browser when it's dying
                return
            }

            val isGUI = driverSettings.isGUI
            if (isGUI) {
                val browser = browserManager.findBrowser(browserId)
                if (browser != null) {
                    // open for diagnosis
                    val urls = listOf("chrome://version/", "chrome://history/")
                    runBlocking { urls.forEach { browser.newDriver().navigateTo(it) } }
                }
            }
        }

        private fun closeLeastValuableDriverPool(browserId: BrowserId, retiredDriverPool: LoadingWebDriverPool?) {
            val isGUI = driverSettings.isGUI
            // Keep some web drivers in GUI mode open for diagnostic purposes.
            val dyingDriverPool = when {
                !isGUI -> retiredDriverPool
                // The drivers are in GUI mode and there is many open drivers.
                else -> findOldestRetiredDriverPoolOrNull()
            }

            if (dyingDriverPool != null) {
                closeBrowserAndDriverPool(dyingDriverPool)
            } else {
                val displayMode = driverSettings.displayMode
                logger.info("Web drivers are in {} mode, please close them manually | {} ", displayMode, browserId)
            }
        }

        private fun closeDriver(driver: WebDriver) {
            val browserId = driver.browser.id
            val driverPool = retiredDriverPools[browserId] ?: workingDriverPools[browserId]
            driverPool?.closeDriver(driver)
            browserManager.closeDriver(driver)
        }

        private fun closeDriverPool(driverPool: LoadingWebDriverPool) {
            val browserId = driverPool.browserId
            logger.info(driverPool.report(verbose = true))

            driverPoolManager._workingDriverPools.remove(browserId)
            driverPoolManager._retiredDriverPools.remove(browserId)
            driverPoolManager._closedDriverPools.add(browserId)

            driverPool.close()
        }

        private fun closeBrowserAndDriverPool(driverPool: LoadingWebDriverPool) {
            val browser = browserManager.findBrowser(driverPool.browserId)
            if (browser != null) {
                closeBrowserAndDriverPool(browser, driverPool)
            } else {
                logger.warn("Browser should exists when driver pool exists | {}", driverPool.browserId)
            }
        }

        private fun closeBrowserAndDriverPool(browser: Browser, driverPool: LoadingWebDriverPool) {
            require(browser.id == driverPool.browserId) { "Browser id not match \n${browser.id}\n${driverPool.browserId}" }

            val browserId = driverPool.browserId
            val displayMode = driverSettings.displayMode
            logger.info("Closing browser & driver pool with {} mode | {}", displayMode, browserId)

            closeDriverPool(driverPool)
            browserManager.closeBrowser(browser)
        }

        private fun findOldestRetiredDriverPoolOrNull(): LoadingWebDriverPool? {
            // Find out the oldest retired driver pool
            val oldestRetiredDriverPool = driverPoolManager._retiredDriverPools.values
                .minByOrNull { it.lastActiveTime } ?: return null
            // Issue #17: when counting dying drivers, all drivers in all pools should be counted.
            val totalDyingDrivers = driverPoolManager._retiredDriverPools.values.sumOf { it.numCreated }

            if (logger.isTraceEnabled) {
                logger.trace("There are {} dying drivers in {} retired driver pools",
                    totalDyingDrivers, driverPoolManager._retiredDriverPools.size)
            }

            return when {
                // low memory
                AppSystemInfo.isCriticalResources -> oldestRetiredDriverPool
                // The drivers are in GUI mode and there are many open drivers.
                totalDyingDrivers > driverPoolManager.maxAllowedDyingDrivers -> oldestRetiredDriverPool
                else -> null
            }
        }
    }
}
