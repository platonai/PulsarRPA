package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.common.persist.ext.event
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import com.codahale.metrics.Gauge
import com.google.common.annotations.Beta
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The web driver pool manager.
 *
 * The web driver pool manager provides web drivers to run web page fetch tasks.
 */
open class WebDriverPoolManager(
    val browserManager: BrowserManager,
    val driverFactory: WebDriverFactory,
    val immutableConfig: ImmutableConfig,
    val suppressMetrics: Boolean = false,
) : Parameterized, PreemptChannelSupport("WebDriverPoolManager"), AutoCloseable {
    companion object {
        val DRIVER_CLOSE_TIME_OUT = Duration.ofSeconds(60)
    }

    private val logger = LoggerFactory.getLogger(WebDriverPoolManager::class.java)
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && AppContext.isActive

    val isReady get() = isActive

    private val driverPoolPool = ConcurrentStatefulDriverPoolPool()

    /**
     * The max number of drivers the pool can hold
     * */
    private val poolCapacity = immutableConfig.getInt(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)

    val driverSettings get() = driverFactory.driverSettings
    val idleTimeout = Duration.ofMinutes(18)

    val workingDriverPools get() = driverPoolPool.workingDriverPools

    val retiredDriverPools get() = driverPoolPool.retiredDriverPools

    val closedDriverPools get() = driverPoolPool.closedDriverPools

    val startTime = Instant.now()
    var lastActiveTime = startTime
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = idleTime > idleTimeout

    val numWaitingTasks get() = workingDriverPools.values.sumOf { it.numWaiting }
    val numStandbyDrivers get() = workingDriverPools.values.sumOf { it.numStandby }
    val numWorkingDrivers get() = workingDriverPools.values.sumOf { it.numWorking }
    val numAvailableDriverSlots get() = workingDriverPools.values.sumOf { it.numDriverSlots }
    val numActiveDrivers get() = workingDriverPools.values.sumOf { it.numActive }

    val numDyingDrivers get() = retiredDriverPools.values.sumOf { it.numCreated }

    val numClosedDrivers get() = closedDriverPools.size

    // Maximum allowed number of retired drives, if it's exceeded, the oldest driver pool should be closed.
    val maxAllowedDyingDrivers = 10

    val numReset by lazy { MetricsSystem.reg.meter(this, "numReset") }
    val numTimeout by lazy { MetricsSystem.reg.meter(this, "numTimeout") }
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

    private var lastMaintainTime = Instant.now()
    private val maintainCount = AtomicInteger()
    private val minMaintainInterval = Duration.ofSeconds(15)
    private val tooFrequentMaintenance get() = DateTimes.isNotExpired(lastMaintainTime, minMaintainInterval)

    /**
     * The web driver pool closer
     * */
    private val driverPoolCloser = BrowserAccompaniedDriverPoolCloser(driverPoolPool, this)

    /**
     * The deferred tasks
     * */
    private val _deferredTasks = ConcurrentSkipListMap<Int, Deferred<FetchResult?>>()

    init {
        gauges?.let { MetricsSystem.reg.registerAll(this, it) }
    }

    /**
     * Run the task using the default browser.
     * */
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
        browseFun: suspend (driver: WebDriver) -> FetchResult?,
    ) = run(WebDriverTask(browserId, task.page, task.priority, browseFun))

    /**
     * Run the task with the driver specified.
     * */
    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun run(
        driverId: Int, task: FetchTask,
        browseFun: suspend (driver: WebDriver) -> FetchResult?,
    ) {
        // subscribe a driver first, and then use it to run a task
        TODO("NOT_IMPLEMENTED")
    }

    /**
     * Run the task.
     * */
    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun run(task: WebDriverTask): FetchResult? {
        lastActiveTime = Instant.now()
        try {
            return doRun(task)
        } catch (e: InterruptedException) {
            warnInterruptible(this, e)
            return null
        } catch (e: WebDriverException) {
            logger.warn("Failed to run the task | {} | {}", task.page.url, e.message)
            return null
        } finally {
            lastActiveTime = Instant.now()
        }
    }

    /**
     * Run the task and save the execution state, so it can be canceled by [cancel] and [cancelAll].
     * */
    private suspend fun runCancelable(task: WebDriverTask, driver: WebDriver): FetchResult? {
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
        browserId: BrowserId = BrowserId.DEFAULT, priority: Int = 0,
    ): LoadingWebDriverPool {
        return LoadingWebDriverPool(browserId, priority, this, driverFactory, immutableConfig)
    }

    /**
     * Get the number of drivers which can serve tasks in the pool.
     * */
    fun promisedDriverCount(browserId: BrowserId) = driverPoolPool.promisedDriverCount(browserId, poolCapacity)

    /**
     * Check if the pool has at least one driver to serve.
     * */
    fun hasDriverPromise(browserId: BrowserId) = promisedDriverCount(browserId) > 0

    /**
     * Check if the pool is on full capacity.
     * */
    fun isFullCapacity(browserId: BrowserId) = driverPoolPool.isFullCapacity(browserId)

    /**
     * Subscribe a web driver in the pool specified by [browserId], the other subscriber
     * should not use the driver.
     * */
    @Beta
    fun subscribeDriver(browserId: BrowserId) = driverPoolPool.subscribeDriver(browserId)

    /**
     * Subscribe a web driver, the other subscriber should not use the driver.
     * */
    @Beta
    fun subscribeDriver() = driverPoolPool.subscribeDriver()

    /**
     * Check if a pool is retired.
     * */
    fun isRetiredPool(browserId: BrowserId) = driverPoolPool.isRetiredPool(browserId)

    /**
     * Cancel the fetch task specified by [url] remotely.
     * NOTE: A cancel request should run immediately not waiting for any browser task return.
     * */
    fun cancel(url: String): WebDriver? {
        var driver: WebDriver? = null
        workingDriverPools.values.forEach { driverPool ->
            driver = driverPool.cancel(url)
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
        // TODO: remove preempt guard, just mark the driver pool and all the drivers as retired first and them close them
        //
        // Wait until there is no normal tasks, and if there is at least one preemptive task
        // in the critical section, all normal tasks have to wait.
        preempt {
            driverPoolCloser.closeGracefully(browserId)
        }
//        driverPoolCloser.closeGracefully(browserId)
    }

    /**
     * Maintain all the driver pools, check and report inconsistency, illness, idleness, etc.,
     * close bad pools if necessary.
     *
     * If "takeDriverPoolSnapshot" is in file AppPaths.PATH_LOCAL_COMMAND, perform the action.
     *
     * If the tmp dir is the default one, run the following command to take snapshot once:
     * echo takeDriverPoolSnapshot >> /tmp/pulsar/pulsar-commands
     * */
    @Throws(Exception::class)
    fun maintain(force: Boolean = false) {
        if (!force && tooFrequentMaintenance) {
            return
        }
        lastMaintainTime = Instant.now()

        if (maintainCount.getAndIncrement() == 0) {
            logger.info("Maintaining service is started, minimal maintain interval: {}", minMaintainInterval)
        }

        try {
            doMaintain()
        } catch (e: InterruptedException) {
            logger.warn("Interrupted | {}", e.message)
            Thread.currentThread().interrupt()
        } catch (t: Throwable) {
            logger.warn(t.stringify("Failed to maintain the driver pool"))
        }

        // assign the last maintain time again
        lastMaintainTime = Instant.now()
    }

    @Throws(InterruptedException::class, Exception::class)
    private fun doMaintain() {
        // To close retired driver pools, there is no need to wait for normal tasks, so no preempting is required
        driverPoolCloser.closeOldestRetiredDriverPoolSafely()
        // Close unexpected active browsers
        driverPoolCloser.closeUnexpectedActiveBrowsers()

        /**
         * Check if there is zombie browsers who are not in active browser list nor in closed browser list,
         * if there are some of such browsers, issue warnings and destroy them.
         * */
        browserManager.destroyZombieBrowsersForcibly()

        val idleDriverPoolCount = workingDriverPools.values.count { it.isIdle }
        if (idleDriverPoolCount > 0) {
            logger.warn("There are {} idle driver pools, preempt and do the maintaining", idleDriverPoolCount)
            // Preempt the channel to ensure consistency
            // TODO: check if it's necessary to preempt the channel
            // We doubt the preemptive maintainer slows down the system if it runs too frequent
            preempt {
                driverPoolCloser.closeIdleDriverPoolsSafely()
            }
        }

        /**
         * If "takeDriverPoolSnapshot" is in file AppPaths.PATH_LOCAL_COMMAND, perform the action.
         *
         * If the tmp dir is the default one, run the following command to take snapshot once:
         * echo takeDriverPoolSnapshot >> /tmp/pulsar/pulsar-commands
         * */
        if (FileCommand.check("takeDriverPoolSnapshot")) {
            logger.info("\nDriver pool manager: \n")
            logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            logger.info("\n{}", takeSnapshot())
            logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
        }
    }

    /**
     * Take a snapshot about the state of the driver pools.
     * */
    fun takeSnapshot(browserId: BrowserId, verbose: Boolean = false): String {
        return workingDriverPools[browserId]?.takeSnapshot()?.format(verbose) ?: ""
    }

    /**
     * Take a snapshot about the state of the driver pools.
     * */
    fun takeSnapshot(verbose: Boolean = false): String {
        val sb = StringBuilder()
        if (workingDriverPools.isNotEmpty()) {
            workingDriverPools.entries.joinTo(sb, "\n") { it.value.takeSnapshot().format(verbose) + " | " + it.key }
        }
        return sb.toString()
    }

    /**
     * Close the web driver pool manager. All deferred tasks will be canceled, the pool of driver pools will be cleared
     * and the browser manager will be closed.
     *
     * It happens when the program exits.
     *
     * The closing call stack:
     *
     * PrivacyContextManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _deferredTasks.values.forEach {
                // Cancels job, including all its children with a specified diagnostic error
                it.cancel("This process is closing")
            }
            _deferredTasks.clear()

            // Actually no exception to catch
            kotlin.runCatching { driverPoolPool.close() }.onFailure { warnForClose(this, it) }
            // Actually no exception to catch
            kotlin.runCatching { browserManager.close() }.onFailure { warnForClose(this, it) }

            logger.info("Web driver pool manager is closed")
        }
    }

    /**
     * Return a string to represent the snapshot of the status.
     * */
    override fun toString(): String = takeSnapshot(false)

    @Throws(WebDriverException::class, WebDriverPoolException::class, InterruptedException::class)
    private suspend fun doRun(task: WebDriverTask): FetchResult? {
        maintain()
        
        val result = runWithDriverPool(task)
        return result
    }

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    private suspend fun runWithDriver(task: WebDriverTask, driver: WebDriver): FetchResult? {
        return runCancelableWithTimeout(task, driver)
    }

    @Throws(WebDriverException::class, WebDriverPoolException::class, InterruptedException::class)
    private suspend fun runWithDriverPool(task: WebDriverTask): FetchResult? {
        val browserId = task.browserId
        var result: FetchResult? = null
        /**
         * There are two kind of tasks, normal tasks and monitor tasks.
         * Normal tasks are executed concurrently; however, they cannot be executed simultaneously with monitor tasks.
         *
         * [PreemptChannelSupport] is developed for such mechanism.
         * */
        whenNormalDeferred {
            if (!isActive) {
                logger.warn("Web driver pool manager is inactive")
                return@whenNormalDeferred null
            }

            // The browser id is specified by the caller, a typical strategy is
            // taking a browser id in a list in sequence, in which case, driver pools are return
            // one by one in sequence.
            //
            val driverPool = getOrCreateDriverPool(browserId, task.priority)

            result = runWithDriverPool(task, driverPool)
        }

        return result
    }

    @Throws(WebDriverPoolException::class)
    private fun getOrCreateDriverPool(browserId: BrowserId, priority: Int): LoadingWebDriverPool {
        if (isRetiredPool(browserId)) {
            val message = "Web driver pool is retired | $browserId"
            logger.warn(message)
            throw WebDriverPoolException(message)
        }

        // The browser id is specified by the caller, a typical strategy is
        // taking a browser id in a list in sequence, in which case, driver pools are return
        // one by one in sequence.
        //
        val driverPool = driverPoolPool.computeIfAbsent(browserId) { createUnmanagedDriverPool(browserId, priority) }

        if (!driverPool.isActive) {
            val message = "Driver pool is inactive | $driverPool | $browserId"
            logger.warn("{}\n{}", message, driverPool.takeSnapshot())
            throw WebDriverPoolException(message)
        }

        return driverPool
    }
    
    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class, InterruptedException::class)
    private suspend fun runWithDriverPool(task: WebDriverTask, driverPool: LoadingWebDriverPool): FetchResult? {
        var driver: WebDriver? = null
        try {
            driver = driverPool.poll(task.priority, task.volatileConfig, task.page.event?.browseEvent, task.page)

            return runWithDriver(task, driver)
        } finally {
            driver?.let { driverPool.put(it) }
        }
    }

    private suspend fun runCancelableWithTimeout(task: WebDriverTask, driver: WebDriver): FetchResult? {
        // do not take up too much time on this driver
        val fetchTaskTimeout = driverSettings.fetchTaskTimeout

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
                fetchTaskTimeout.readable(), takeSnapshot(browserId), browserId
            )
            null
        } finally {
            _deferredTasks.remove(task.id)
        }
    }
}

class WebDriverTask(
    val browserId: BrowserId,
    val page: WebPage,
    val priority: Int = 0,
    val driverFun: suspend (driver: WebDriver) -> FetchResult?,
) {
    companion object {
        private val sequencer = AtomicInteger()
    }

    val id = sequencer.incrementAndGet()
    val volatileConfig get() = page.conf
}

/**
 * An auxiliary class can securely close the browser driver pool and associated browser,
 * and keep the browser drive pool and related browser status consistent.
 * */
private class BrowserAccompaniedDriverPoolCloser(
    private val driverPoolPool: ConcurrentStatefulDriverPoolPool,
    private val driverPoolManager: WebDriverPoolManager,
) {
    private val driverSettings get() = driverPoolManager.driverSettings
    private val browserManager get() = driverPoolManager.browserManager

    private val workingDriverPools get() = driverPoolPool.workingDriverPools

    private val retiredDriverPools get() = driverPoolPool.retiredDriverPools

    private val logger = getLogger(this)

    /**
     * Mark the browser as retired, remove it from the working driver pool, so it can no longer be used for new tasks.
     * All drivers in the browser are marked as retired, any task should be canceled who is using an inactive driver.
     *
     * All drivers in a driver pool are created by the same browser. The associated browser has to be closed if
     * we close a driver pool, and vice versa.
     * */
    @Synchronized
    fun closeGracefully(browserId: BrowserId) {
        kotlin.runCatching { doClose(browserId) }.onFailure { warnInterruptible(this, it) }
    }

    @Synchronized
    fun closeOldestRetiredDriverPoolSafely() {
        val dyingDriverPool = findOldestRetiredDriverPoolOrNull()

        if (dyingDriverPool != null) {
            closeBrowserAccompaniedDriverPool(dyingDriverPool)
        }
    }

    @Synchronized
    fun closeIdleDriverPoolsSafely() {
        // TODO: just mark them to be retired
//        workingDriverPools.values.filter { it.isIdle }.forEach {
//            it.retire()
//        }

        workingDriverPools.values.filter { it.isIdle }.forEach { driverPool ->
            logger.info("Driver pool is idle, closing it ... | {}", driverPool.browserId)
            logger.info(driverPool.takeSnapshot().format(true))
            runCatching { closeBrowserAccompaniedDriverPool(driverPool) }.onFailure { warnInterruptible(this, it) }
        }
    }

    @Synchronized
    fun closeUnexpectedActiveBrowsers() {
        driverPoolPool.closedDriverPools.forEach { browserId ->
            val browser = browserManager.findBrowser(browserId)
            if (browser != null) {
                logger.warn("Browser should be closed, but still in active list, closing them ... | {}", browserId)
                runCatching { browserManager.closeBrowser(browserId) }.onFailure { warnInterruptible(this, it) }
            }
        }
    }

    private fun doClose(browserId: BrowserId) {
        // Force the page to stop all navigations and RELEASE all resources.
        // mark the driver pool be retired, but not closed yet
        val retiredDriverPool = driverPoolPool.retire(browserId)

        if (retiredDriverPool != null) {
            closeBrowserAccompaniedDriverPool(retiredDriverPool)
        } else {

        }
    }

    private fun doCloseWithDiagnosis(browserId: BrowserId) {
        // Force the page to stop all navigations and RELEASE all resources.
        // mark the driver pool be retired, but not closed yet
        val retiredDriverPool = driverPoolPool.retire(browserId)

        if (retiredDriverPool != null) {
            openInformationPages(browserId)
        }

        closeLeastValuableDriverPool(browserId, retiredDriverPool)
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
                runBlocking { urls.forEach { openInformationPage(it, browser) } }
            }
        }
    }

    private suspend fun openInformationPage(url: String, browser: Browser) {
        runCatching { browser.newDriver().navigateTo(url) }.onFailure { warnInterruptible(this, it) }
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
            closeBrowserAccompaniedDriverPool(dyingDriverPool)
        } else {
            val displayMode = driverSettings.displayMode
            logger.info("Web drivers are in {} mode, please close them manually | {} ", displayMode, browserId)
        }
    }

    private fun closeBrowserAccompaniedDriverPool(driverPool: LoadingWebDriverPool) {
        val browser = browserManager.findBrowser(driverPool.browserId)
        if (browser != null) {
            closeBrowserAccompaniedDriverPool(browser, driverPool)
        } else {
            kotlin.runCatching { driverPoolPool.close(driverPool) }.onFailure { warnInterruptible(this, it) }
            logger.warn("Browser should exists when driver pool exists | {}", driverPool.browserId)
        }
    }

    private fun closeBrowserAccompaniedDriverPool(browser: Browser, driverPool: LoadingWebDriverPool) {
        require(browser.id == driverPool.browserId) { "Browser id not match \n${browser.id}\n${driverPool.browserId}" }

        val browserId = driverPool.browserId
        val displayMode = driverSettings.displayMode
        logger.info("Closing browser & driver pool with {} mode | {}", displayMode, browserId)

        kotlin.runCatching { driverPoolPool.close(driverPool) }.onFailure { warnInterruptible(this, it) }
        kotlin.runCatching { browserManager.closeBrowser(browser) }.onFailure { warnInterruptible(this, it) }
    }

    private fun findOldestRetiredDriverPoolOrNull(): LoadingWebDriverPool? {
        // Find out the oldest retired driver pool
        val oldestRetiredDriverPool = driverPoolManager.retiredDriverPools.values
            .minByOrNull { it.lastActiveTime } ?: return null
        // Issue #17: when counting dying drivers, all drivers in all pools should be counted.
        val totalDyingDrivers = driverPoolManager.retiredDriverPools.values.sumOf { it.numCreated }

        if (logger.isTraceEnabled) {
            logger.trace(
                "There are {} dying drivers in {} retired driver pools",
                totalDyingDrivers, driverPoolManager.retiredDriverPools.size
            )
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
