package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.persist.ext.browseEvent
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
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
    private val _driverPools = ConcurrentSkipListMap<BrowserId, LoadingWebDriverPool>()
    private val _retiredDriverPools = ConcurrentSkipListSet<BrowserId>()
    private val _deferredTasks = ConcurrentSkipListMap<Int, Deferred<FetchResult?>>()

    val driverSettings get() = driverFactory.driverSettings
    val idleTimeout = Duration.ofMinutes(18)

    val driverPools: Map<BrowserId, LoadingWebDriverPool> get() = _driverPools
    val retiredDriverPools: Set<BrowserId> get() = _retiredDriverPools

    val startTime = Instant.now()
    var lastActiveTime = startTime
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = idleTime > idleTimeout

    val numReset by lazy { AppMetrics.reg.meter(this, "numReset") }
    val numTimeout by lazy { AppMetrics.reg.meter(this, "numTimeout") }
    val gauges = mapOf(
            "waitingDrivers" to Gauge { numWaiting },
            "freeDrivers" to Gauge { numFreeDrivers },
            "workingDrivers" to Gauge { numWorkingDrivers },
            "onlineDrivers" to Gauge { numOnline },
            "pTasks" to Gauge { numPreemptiveTasks.get() },
            "runningPTasks" to Gauge { numRunningPreemptiveTasks.get() },
            "pendingNTasks" to Gauge { numPendingNormalTasks.get() },
            "runningNTasks" to Gauge { numRunningNormalTasks.get() },
            "idleTime" to Gauge { idleTime.readable() }
    ).takeUnless { suppressMetrics }

    val numWaiting get() = driverPools.values.sumOf { it.numWaiting.get() }
    val numFreeDrivers get() = driverPools.values.sumOf { it.numFree }
    val numWorkingDrivers get() = driverPools.values.sumOf { it.numWorking.get() }
    val numAvailableDrivers get() = driverPools.values.sumOf { it.numAvailable }
    val numOnline get() = driverPools.values.sumOf { it.onlineDrivers.size }

//    private val launchLock = ReentrantLock()
    private val launchMutex = Mutex()

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
    suspend fun run(browserId: BrowserId, task: FetchTask,
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
     * Create a driver pool, but the driver pool is not added to [driverPools].
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
        driverPools.values.forEach { driverPool ->
            driver = driverPool.onlineDrivers.lastOrNull { it.navigateEntry.pageUrl == url }?.also {
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
        val driverPool = driverPools[browserId] ?: return null
        return driverPool.firstOrNull { it.navigateEntry.pageUrl == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages.
     * */
    fun cancelAll() {
        driverPools.values.forEach { it.cancelAll() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages.
     * */
    fun cancelAll(browserId: BrowserId) {
        val driverPool = driverPools[browserId] ?: return
        driverPool.cancelAll()
    }

    /**
     * Find the driver pool with browserId of [browserId], cancel all running tasks in it, and close all drivers in it.
     *
     * When we close a driver pool, all threads that are trying to get a driver should wait.
     * */
    fun closeDriverPoolGracefully(browserId: BrowserId, timeToWait: Duration) {
        numReset.mark()

        // Wait until there is no normal tasks, and if there is at least one preemptive task
        // in the critical section, all normal tasks must wait.
        preempt {
            closeDriverPoolGracefully0(browserId)
        }
    }

    fun formatStatus(browserId: BrowserId, verbose: Boolean = false): String {
        return _driverPools[browserId]?.formatStatus(verbose)?:""
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _deferredTasks.values.forEach {
                // Cancels job, including all its children with a specified diagnostic error
                it.cancel("Already closed")
            }
            _deferredTasks.clear()

            _driverPools.values.forEach { it.close() }
            _driverPools.clear()

            browserManager.close()

            logger.info("Web driver pool manager is closed")
        }
    }

    override fun toString(): String = formatStatus(false)

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    private suspend fun run0(task: WebDriverTask): FetchResult? {
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
            logger.warn("Coroutine canceled({}) (by [withTimeout]) | {} | {}",
                fetchTaskTimeout.readable(), formatStatus(browserId), browserId)
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
        driver.startWork()
        return driver
    }

    @Synchronized
    private fun computeDriverPoolIfAbsent(
        browserId: BrowserId, task: WebDriverTask
    ): LoadingWebDriverPool {
        return _driverPools.computeIfAbsent(browserId) { createUnmanagedDriverPool(browserId, task.priority) }
    }

    private fun closeDriverPoolGracefully0(browserId: BrowserId) {
        _driverPools[browserId]?.also {
            // Force the page stop all navigations and RELEASES all resources.
            it.onlineDrivers.forEach { kotlin.runCatching { runBlocking { it.stop() } } }
            it.isRetired = true
            _retiredDriverPools.add(browserId)
        }

        val isGUI = driverSettings.isGUI
        val displayMode = driverSettings.displayMode
        // Issue #17: when counting retired web drivers, all retired drivers in all driver pools should be counted.
        val totalRetiredDrivers = driverPools.values.sumOf { it.onlineDrivers.count { it.isRetired } }
        // Maximum allowed number of retired drives, if it's exceeded, the oldest driver pool should be closed.
        val maxAllowedRetiredDrivers = 10
        // Keep some web drivers in GUI mode open for diagnostic purposes.
        val driverPool = when {
            !isGUI -> _driverPools.remove(browserId)
            // The drivers are in GUI mode and there is many open drivers.
            totalRetiredDrivers > maxAllowedRetiredDrivers -> {
                // Find out the oldest retired driver pool
                driverPools.values.filter { it.isRetired }.minByOrNull { it.lastActiveTime }
            }
            else -> null
        }

        if (driverPool != null) {
            logger.info(driverPool.formatStatus(verbose = true))
            logger.info("Closing driver pool with {} mode | {}", displayMode, browserId)
            driverPool.close()
            browserManager.closeBrowserGracefully(browserId)
        } else {
            val key = browserId.userDataDir.toString()
            val browser = browserManager.browsers[key]
            if (browser != null) {
                // open for diagnosis
                val urls = listOf("chrome://version/", "chrome://history/")
                runBlocking { urls.forEach { browser.newDriver().navigateTo(it) } }
            }
            logger.info("Web drivers are in {} mode, please close it manually | {} ", displayMode, browserId)
        }
    }

    private fun formatStatus(verbose: Boolean = false): String {
        val sb = StringBuilder()
        if (driverPools.isNotEmpty()) {
            driverPools.entries.joinTo(sb, "\n") { it.value.formatStatus(verbose) + " | " + it.key }
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
}
