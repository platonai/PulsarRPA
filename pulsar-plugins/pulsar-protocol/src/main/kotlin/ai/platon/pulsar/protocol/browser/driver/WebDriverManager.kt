package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.PreemptChannelSupport
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import com.codahale.metrics.Gauge
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean

class WebDriverTask<R> (
        val dataDir: Path,
        val priority: Int,
        val volatileConfig: VolatileConfig,
        val action: suspend (driver: ManagedWebDriver) -> R
)

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverManager(
        val driverControl: WebDriverControl,
        val proxyMonitor: ProxyPoolMonitor,
        val driverFactory: WebDriverFactory,
        val immutableConfig: ImmutableConfig
): Parameterized, PreemptChannelSupport(), AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverManager::class.java)

    private val taskTimeout = Duration.ofMinutes(5)
    private val CLOSE_TIME_TO_WAIT = Duration.ofSeconds(90)
    private val closed = AtomicBoolean()
    val startTime = Instant.now()
    val numReset = MetricsManagement.meter(this, "numReset")
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val driverPools = ConcurrentSkipListMap<Path, LoadingWebDriverPool>()

    init {
        mapOf(
                "normalTasks" to Gauge<Int> { numNormalTasks.get() },
                "runningNormalTasks" to Gauge<Int> { numRunningNormalTasks.get() },
                "readyPreemptiveTasks" to Gauge<Int> { numReadyPreemptiveTasks.get() },
                "runningPreemptiveTasks" to Gauge<Int> { numRunningPreemptiveTasks.get() },

                "waitingDrivers" to Gauge<Int> { driverPools.values.sumBy { it.numWaiting.get() } },
                "workingDrivers" to Gauge<Int> { driverPools.values.sumBy { it.numWorking.get() } }
        ).forEach { MetricsManagement.register(this, it.key, it.value) }
    }

    /**
     * reactor: tell me if you can do this job
     * proactor: here is a job, tell me if you finished it
     * */
    suspend fun <R> run(dataDir: Path, priority: Int, volatileConfig: VolatileConfig,
                        action: suspend (driver: ManagedWebDriver) -> R
    ) = run(WebDriverTask(dataDir, priority, volatileConfig, action))

    suspend fun <R> run(task: WebDriverTask<R>): R {
        return whenNormalDeferred {
            val driverPool = driverPools.computeIfAbsent(task.dataDir) { path ->
                require("browser" in path.toString())
                LoadingWebDriverPool(path, task.priority, driverFactory, immutableConfig)
                        .also { it.allocate(task.volatileConfig) }
            }
            val driver = driverPool.take(task.priority, task.volatileConfig).apply { startWork() }
            try {
                withTimeout(taskTimeout.toMillis()) { task.action(driver) }
            } finally {
                driverPool.put(driver)
            }
        }
    }

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(url: String): ManagedWebDriver? {
        var driver: ManagedWebDriver? = null
        driverPools.values.forEach { it ->
            driver = it.firstOrNull { it.url == url }?.also { it.cancel() }
        }
        return driver
    }

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(dataDir: Path, url: String): ManagedWebDriver? {
        val driverPool = driverPools[dataDir] ?: return null
        return driverPool.firstOrNull { it.url == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        driverPools.values.forEach { driverPool ->
            driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
        }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll(dataDir: Path) {
        val driverPool = driverPools[dataDir] ?: return
        driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun reset(dataDir: Path, timeToWait: Duration) {
        numReset.mark()
        closeAll(dataDir, timeToWait = timeToWait)
    }

    fun formatStatus(dataDir: Path, verbose: Boolean = false): String {
        return driverPools[dataDir]?.formatStatus(verbose)?:""
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.info("Web driver manager is closed\n{}", toString())
        }
    }

    override fun toString(): String = formatStatus(false)

    private fun closeAll(dataDir: Path, timeToWait: Duration) {
        val driver = driverPools[dataDir]?:return
        log.info("Closing all web drivers | {}", driver.formatStatus(verbose = true))
        driver.closeAll(timeToWait = timeToWait)
    }

    private fun formatStatus(verbose: Boolean = false): String {
        return driverPools.entries.joinToString("\n") { it.value.formatStatus(verbose) + " | " + it.key }
    }
}
