package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.PreemptChannelSupport
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_EAGER_ALLOCATE_TABS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.BrowserInstanceId
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
        val action: suspend (driver: ManagedWebDriver) -> R
)

class WebDriverPoolRetiredException(message: String): IllegalStateException(message)

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverPoolManager(
        val driverControl: WebDriverControl,
        val proxyManager: ProxyPoolManager,
        val driverFactory: WebDriverFactory,
        val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    companion object {
        val DRIVER_CLOSE_TIME_OUT = Duration.ofSeconds(60)
    }

    private val log = LoggerFactory.getLogger(WebDriverPoolManager::class.java)
    private val closed = AtomicBoolean()
    private val eagerAllocateTabs = immutableConfig.getBoolean(BROWSER_EAGER_ALLOCATE_TABS, false)
    private val taskTimeout = Duration.ofMinutes(5)
    private val pollingDriverTimeout = LoadingWebDriverPool.POLLING_TIMEOUT

    val isActive get() = !closed.get()
    val startTime = Instant.now()
    val numReset = MetricsManagement.meter(this, "numReset")
    val numTimeout = MetricsManagement.meter(this, "numTimeout")
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val driverPools = ConcurrentSkipListMap<BrowserInstanceId, LoadingWebDriverPool>()
    val retiredPools = ConcurrentSkipListSet<BrowserInstanceId>()

    val numWaiting get() = driverPools.values.sumBy { it.numWaiting.get() }
    val numFreeDrivers get() = driverPools.values.sumBy { it.numFree }
    val numWorkingDrivers get() = driverPools.values.sumBy { it.numWorking.get() }
    val numAvailableDrivers get() = driverPools.values.sumBy { it.numAvailable }
    val numOnline get() = driverPools.values.sumBy { it.onlineDrivers.size }

    init {
        mapOf(
                "waitingDrivers" to Gauge<Int> { numWaiting },
                "freeDrivers" to Gauge<Int> { numFreeDrivers },
                "workingDrivers" to Gauge<Int> { numWorkingDrivers },
                "onlineDrivers" to Gauge<Int> { numOnline }
        ).forEach { MetricsManagement.register(this, it.key, it.value) }
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
                        action: suspend (driver: ManagedWebDriver) -> R?
    ) = run(WebDriverTask(browserId, priority, volatileConfig, action))

    @Throws(IllegalApplicationContextStateException::class)
    suspend fun <R> run(task: WebDriverTask<R>): R? {
        val browserId = task.browserId
        val result: R?

        checkState()

        if (isRetiredPool(browserId)) {
            throw WebDriverPoolRetiredException("$browserId")
        }

        val driverPool = computeDriverPoolIfAbsent(browserId, task)
        if (!driverPool.isActive) {
            log.warn("Driver pool is closed already | {}", driverPool)
            return null
        }

        var driver: ManagedWebDriver? = null
        try {
            checkState()
            driver = driverPool.poll(task.priority, task.volatileConfig, pollingDriverTimeout).apply { startWork() }
            result = withTimeoutOrNull(taskTimeout.toMillis()) {
                checkState()
                task.action(driver)
            }

            if (result == null) {
                numTimeout.mark()
                log.warn("Task timeout after {} with driver {} | {}", taskTimeout.readable(), driver, browserId)
            }
        } finally {
            driver?.let { driverPool.put(it) }
        }

        return result
    }

    @Synchronized
    private fun <R> computeDriverPoolIfAbsent(browserId: BrowserInstanceId, task: WebDriverTask<R>): LoadingWebDriverPool {
        return driverPools.computeIfAbsent(browserId) { path ->
            require("browser" in path.toString())
            LoadingWebDriverPool(browserId, task.priority, driverFactory, immutableConfig).also {
                it.takeIf { eagerAllocateTabs }?.allocate(task.volatileConfig)
            }
        }
    }

    fun isRetiredPool(browserId: BrowserInstanceId) = retiredPools.contains(browserId)

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(url: String): ManagedWebDriver? {
        checkState()
        var driver: ManagedWebDriver? = null
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
    fun cancel(browserId: BrowserInstanceId, url: String): ManagedWebDriver? {
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
        checkState()
        numReset.mark()
        // Mark all drivers are canceled
        doCloseDriverPool(browserId)
    }

    fun formatStatus(browserId: BrowserInstanceId, verbose: Boolean = false): String {
        return driverPools[browserId]?.formatStatus(verbose)?:""
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            driverPools.clear()
            log.info("Web driver manager is closed\n{}", toString())
        }
    }

    override fun toString(): String = formatStatus(false)

    private fun doCloseDriverPool(browserId: BrowserInstanceId) {
        checkState()
        retiredPools.add(browserId)
        driverPools.remove(browserId)?.also { driverPool ->
            log.info("Closing driver pool | {} | {}", driverPool.formatStatus(verbose = true), browserId)
            driverPool.close()
        }
    }

    private fun checkState() {
        if (!isActive) throw IllegalApplicationContextStateException("Web driver manager is closed")
    }

    private fun formatStatus(verbose: Boolean = false): String {
        return driverPools.entries.joinToString("\n") { it.value.formatStatus(verbose) + " | " + it.key }
    }
}
