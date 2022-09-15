package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.crawl.PulsarEvent
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import com.codahale.metrics.Gauge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class WebDriverTask<R> (
        val browserId: BrowserId,
        val page: WebPage,
        val priority: Int = 0,
        val runWith: suspend (driver: WebDriver) -> R
) {
    val volatileConfig get() = page.conf
    val event get() = volatileConfig.getBeanOrNull(PulsarEvent::class)
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class WebDriverPoolManager(
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
    private val browserManager: BrowserManager get() = driverFactory.browserManager
    private val _driverPools = ConcurrentSkipListMap<BrowserId, LoadingWebDriverPool>()
    private val _retiredDriverPools = ConcurrentSkipListSet<BrowserId>()

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

    @Throws(WebDriverException::class)
    suspend fun <R> run(task: FetchTask, browseFun: suspend (driver: WebDriver) -> R?) =
        run(WebDriverTask(BrowserId.DEFAULT, task.page, task.priority, browseFun))

    /**
     * TODO: consider pro-actor model instead
     *
     * reactor: tell me if you can do this job
     * proactor: here is a job, tell me if you finished it
     *
     * @return The result of action, or null if timeout
     * */
    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun <R> run(browserId: BrowserId, task: FetchTask,
                        browseFun: suspend (driver: WebDriver) -> R?
    ) = run(WebDriverTask(browserId, task.page, task.priority, browseFun))

    @Throws(WebDriverException::class, WebDriverPoolException::class)
    suspend fun <R> run(task: WebDriverTask<R>): R? {
        lastActiveTime = Instant.now()
        return run0(task).also { lastActiveTime = Instant.now() }
    }

    /**
     * Create a driver pool, but the driver pool is not added to [driverPools]
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
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(browserId: BrowserId, url: String): WebDriver? {
        val driverPool = driverPools[browserId] ?: return null
        return driverPool.firstOrNull { it.navigateEntry.pageUrl == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        driverPools.values.forEach { it.cancelAll() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll(browserId: BrowserId) {
        val driverPool = driverPools[browserId] ?: return
        driverPool.cancelAll()
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun closeDriverPool(browserId: BrowserId, timeToWait: Duration) {
        numReset.mark()
        // Mark all drivers are canceled
        doCloseDriverPool(browserId)
    }

    fun formatStatus(browserId: BrowserId, verbose: Boolean = false): String {
        return _driverPools[browserId]?.formatStatus(verbose)?:""
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _driverPools.keys.forEach { doCloseDriverPool(it) }
            _driverPools.clear()
            logger.info("Web driver pool manager is closed")
            if (gauges?.entries?.isEmpty() == false || _driverPools.isNotEmpty()) {
                val s = formatStatus(true)
                if (s.isNotEmpty()) {
                    logger.info(s)
                }
            }
        }
    }

    override fun toString(): String = formatStatus(false)

    @Throws(WebDriverException::class)
    private suspend fun <R> run0(task: WebDriverTask<R>): R? {
        val browserId = task.browserId
        var result: R? = null
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
                    if (isActive) poll(driverPool, task) else return@whenNormalDeferred null
                }

                result = runWithTimeout(task, driver)
            } finally {
                driver?.let { driverPool.put(it) }
            }
        }

        return result
    }

    private suspend fun <R> runWithTimeout(task: WebDriverTask<R>, driver: WebDriver): R? {
        // do not take up too much time on this driver
        val fetchTaskTimeout = driverSettings.fetchTaskTimeout
        val result = withTimeoutOrNull(fetchTaskTimeout.toMillis()) {
            if (isActive) task.runWith(driver) else null
        }

        if (result == null) {
            numTimeout.mark()

            // This should not happen since the task itself should handle the timeout event
            val browserId = driver.browserId
            logger.warn("Coroutine timeout({}) (by [withTimeoutOrNull]) | {} | {}",
                fetchTaskTimeout.readable(), formatStatus(browserId), browserId)
        }

        return result
    }

    private suspend fun <R> poll(driverPool: LoadingWebDriverPool, task: WebDriverTask<R>): WebDriver {
        val notLaunched = driverPool.launched.compareAndSet(false, true)
        return if (notLaunched) {
            launchAndPoll(driverPool, task)
        } else {
            pollWebDriver(driverPool, task)
        }
    }

    private suspend fun <R> launchAndPoll(driverPool: LoadingWebDriverPool, task: WebDriverTask<R>): WebDriver {
        val event = task.event?.loadEvent
        val page = task.page

        runSafely("onWillLaunchBrowser") { event?.onWillLaunchBrowser?.invoke(page) }

        return pollWebDriver(driverPool, task).also { driver ->
            runSafely("onBrowserLaunched") { event?.onBrowserLaunched?.invoke(page, driver) }
        }
    }

    private fun <R> pollWebDriver(driverPool: LoadingWebDriverPool, task: WebDriverTask<R>): WebDriver {
        val timeout = driverSettings.pollingDriverTimeout
        val driver = driverPool.poll(task.priority, task.volatileConfig, timeout)
        driver.startWork()
        return driver
    }

    @Synchronized
    private fun <R> computeDriverPoolIfAbsent(
        browserId: BrowserId, task: WebDriverTask<R>
    ): LoadingWebDriverPool {
        return _driverPools.computeIfAbsent(browserId) { createUnmanagedDriverPool(browserId, task.priority) }
    }

    private fun doCloseDriverPool(browserId: BrowserId) {
        // `preempt` prevents new tasks from being accepted
        preempt {
            _retiredDriverPools.add(browserId)

            val isGUI = driverSettings.isGUI
            val displayMode = driverSettings.displayMode
            val driverPool = when {
                !isGUI -> _driverPools.remove(browserId)
                isGUI && driverPools.size > 10 -> {
                    driverPools.values.filter { it.isRetired }.minByOrNull { it.lastActiveTime }
                }
                else -> null
            }

            if (driverPool != null) {
                driverPool.isRetired = true
                logger.info(driverPool.formatStatus(verbose = true))
                logger.info("Closing driver pool with {} mode | {}", displayMode, browserId)
                driverPool.close()
                browserManager.close(browserId)
            } else {
                logger.info("Web drivers are in {} mode, please close it manually | {} ", displayMode, browserId)
            }
        }
    }

    private fun formatStatus(verbose: Boolean = false): String {
        val sb = StringBuilder()
        if (driverPools.isNotEmpty()) {
            driverPools.entries.joinTo(sb, "\n") { it.value.formatStatus(verbose) + " | " + it.key }
        }
        return sb.toString()
    }

    private suspend fun runSafely(name: String, action: suspend () -> Unit) {
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
