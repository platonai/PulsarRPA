package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_EAGER_ALLOCATE_TABS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import com.codahale.metrics.Gauge
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class WebDriverTask<R> (
        val browserId: BrowserInstanceId,
        val priority: Int,
        val volatileConfig: VolatileConfig,
        val action: suspend (driver: WebDriver) -> R
)

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

    val driverSettings get() = driverFactory.driverSettings
    val eagerAllocateTabs = immutableConfig.getBoolean(BROWSER_EAGER_ALLOCATE_TABS, false)
    val taskTimeout = Duration.ofMinutes(6)
    val pollingDriverTimeout = LoadingWebDriverPool.POLLING_TIMEOUT
    val idleTimeout = Duration.ofMinutes(18)

    val driverPools = ConcurrentSkipListMap<BrowserInstanceId, LoadingWebDriverPool>()
    val retiredPools = ConcurrentSkipListSet<BrowserInstanceId>()

    val isActive get() = !closed.get() && AppContext.isActive
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

    init {
        gauges?.let { AppMetrics.reg.registerAll(this, it) }
    }

    /**
     * TODO: consider pro-actor model instead
     *
     * reactor: tell me if you can do this job
     * proactor: here is a job, tell me if you finished it
     *
     * @return The result of action, or null if timeout
     * */
    @Throws(IllegalApplicationContextStateException::class)
    suspend fun <R> run(browserId: BrowserInstanceId, priority: Int, volatileConfig: VolatileConfig,
                        action: suspend (driver: WebDriver) -> R?
    ) = run(WebDriverTask(browserId, priority, volatileConfig, action))

    @Throws(IllegalApplicationContextStateException::class)
    suspend fun <R> run(task: WebDriverTask<R>): R? {
        lastActiveTime = Instant.now()
        return run0(task).also { lastActiveTime = Instant.now() }
    }

    fun createUnmanagedDriverPool(
            browserId: BrowserInstanceId = BrowserInstanceId.DEFAULT,
            priority: Int = 0,
            volatileConfig: VolatileConfig? = null
    ): LoadingWebDriverPool {
        return LoadingWebDriverPool(browserId, priority, driverFactory, immutableConfig).also {
            it.takeIf { eagerAllocateTabs }?.allocate(volatileConfig?:immutableConfig.toVolatileConfig())
        }
    }

    fun isRetiredPool(browserId: BrowserInstanceId) = retiredPools.contains(browserId)

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(url: String): WebDriver? {
        checkState()
        var driver: WebDriver? = null
        driverPools.values.forEach { driverPool ->
            driver = driverPool.firstOrNull { it.url == url }?.also {
                it.cancel()
            }
        }
        return driver
    }

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(browserId: BrowserInstanceId, url: String): WebDriver? {
        checkState()
        val driverPool = driverPools[browserId] ?: return null
        return driverPool.firstOrNull { it.url == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        checkState()
        driverPools.values.forEach { driverPool ->
            driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
        }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll(browserId: BrowserInstanceId) {
        checkState()
        val driverPool = driverPools[browserId] ?: return
        driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun closeDriverPool(browserId: BrowserInstanceId, timeToWait: Duration) {
        numReset.mark()
        // Mark all drivers are canceled
        doCloseDriverPool(browserId)
    }

    fun formatStatus(browserId: BrowserInstanceId, verbose: Boolean = false): String {
        return driverPools[browserId]?.formatStatus(verbose)?:""
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            driverPools.keys.forEach { doCloseDriverPool(it) }
            driverPools.clear()
            logger.info("Web driver pool manager is closed")
            if (gauges?.entries?.isEmpty() == false || driverPools.isNotEmpty()) {
                logger.info(formatStatus(true))
            }
        }
    }

    override fun toString(): String = formatStatus(false)

    @Throws(IllegalApplicationContextStateException::class)
    private suspend fun <R> run0(task: WebDriverTask<R>): R? {
        val browserId = task.browserId
        var result: R? = null
        whenNormalDeferred {
            checkState()

            if (isRetiredPool(browserId)) {
                throw WebDriverPoolException("Web driver pool is retired | $browserId")
            }

            val driverPool = computeDriverPoolIfAbsent(browserId, task)
            if (!driverPool.isActive) {
                throw WebDriverPoolException("Driver pool is already closed | $driverPool | $browserId")
            }

            var driver: WebDriver? = null
            try {
                checkState()
                driver = driverPool.poll(task.priority, task.volatileConfig, pollingDriverTimeout).apply { startWork() }
                driverPool.numTasks.incrementAndGet()
                result = withTimeoutOrNull(taskTimeout.toMillis()) {
                    checkState()
                    task.action(driver)
                }

                if (result == null) {
                    numTimeout.mark()
                    driverPool.numTimeout.incrementAndGet()
                    driverPool.numDismissWarnings.incrementAndGet()

                    // This should not happen since the task itself should handle the timeout event
                    logger.warn("Web driver task timeout({}) | {} | {}",
                            taskTimeout.readable(), formatStatus(browserId), browserId)
                } else {
                    driverPool.numSuccess.incrementAndGet()
                    driverPool.numDismissWarnings.decrementAndGet()
                }
            }
            finally {
                driver?.let { driverPool.put(it) }
            }
        }

        return result
    }

    @Synchronized
    private fun <R> computeDriverPoolIfAbsent(browserId: BrowserInstanceId, task: WebDriverTask<R>): LoadingWebDriverPool {
        return driverPools.computeIfAbsent(browserId) { createUnmanagedDriverPool(browserId, task.priority, task.volatileConfig) }
    }

    private fun doCloseDriverPool(browserId: BrowserInstanceId) {
        preempt {
            retiredPools.add(browserId)

            val isGUI = driverSettings.isGUI
            val displayMode = driverSettings.displayMode
            logger.info("Web drivers are in {} mode with {} ", displayMode, browserId)

            val driverPool = when {
                !isGUI -> driverPools.remove(browserId)
                isGUI && driverPools.size > 10 -> {
                    driverPools.values.filter { it.isRetired }.minByOrNull { it.lastActiveTime }
                }
                else -> null
            }

            if (driverPool != null) {
                driverPool.isRetired = true
                logger.info(driverPool.formatStatus(verbose = true))

                logger.info("Closing driver pool in {} mode with {}", displayMode, browserId)
                driverPool.close()
            }
        }
    }

    private fun checkState() {
        if (!isActive) throw IllegalApplicationContextStateException("Web driver manager is closed")
    }

    private fun formatStatus(verbose: Boolean = false): String {
        val sb = StringBuilder()
        if (driverPools.isNotEmpty()) {
            driverPools.entries.joinTo(sb, "\n") { it.value.formatStatus(verbose) + " | " + it.key }
        }
        return sb.toString()
    }
}
