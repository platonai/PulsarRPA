package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.PreemptChannelSupport
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.common.proxy.ProxyMonitorFactory
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverManager(
        val driverControl: WebDriverControl,
        val proxyMonitorFactory: ProxyMonitorFactory,
        val immutableConfig: ImmutableConfig
): Parameterized, PreemptChannelSupport(), AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverManager::class.java)

    val proxyManager = proxyMonitorFactory.get()
    val driverFactory = WebDriverFactory(driverControl, proxyManager, immutableConfig)
    val driverPool = LoadingWebDriverPool(driverFactory, immutableConfig)

    private val closed = AtomicBoolean()
    val startTime = Instant.now()
    private val metricRegistry = SharedMetricRegistries.getDefault()
    val numReset = metricRegistry.meter(prependReadableClassName(this, "numReset"))
    val elapsedTime get() = Duration.between(startTime, Instant.now())

    init {
        metricRegistry.register(prependReadableClassName(this,"tasks"), object: Gauge<Int> {
            override fun getValue(): Int = numTasks.get()
        })
        metricRegistry.register(prependReadableClassName(this,"runningTasks"), object: Gauge<Int> {
            override fun getValue(): Int = numRunningTasks.get()
        })
        metricRegistry.register(prependReadableClassName(this,"readyPreemptiveTasks"), object: Gauge<Int> {
            override fun getValue(): Int = numReadyPreemptiveTasks.get()
        })
        metricRegistry.register(prependReadableClassName(this,"runningPreemptiveTasks"), object: Gauge<Int> {
            override fun getValue(): Int = numRunningPreemptiveTasks.get()
        })
    }

    fun allocate(n: Int, volatileConfig: VolatileConfig) = allocate(0, n, volatileConfig)

    /**
     * Allocate [n] drivers with priority [priority]
     * */
    fun allocate(priority: Int, n: Int, volatileConfig: VolatileConfig) {
        preempt {
            repeat(n) { driverPool.runCatching { put(take(priority, volatileConfig)) } }
        }
    }

    suspend fun <R> submit(priority: Int, volatileConfig: VolatileConfig, action: suspend (driver: ManagedWebDriver) -> R): R {
        return whenNormalDeferred {
            val driver = driverPool.take(priority, volatileConfig).apply { startWork() }
            try {
                action(driver)
            } finally {
                driverPool.put(driver)
            }
        }
    }

    /**
     * Run an action in this pool
     * */
    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        return whenNormal {
            val driver = driverPool.take(priority, volatileConfig).apply { startWork() }
            try {
                action(driver)
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
        return driverPool.firstOrNull { it.url == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun reset() {
        numReset.mark()
        closeAll(incognito = true)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            cancelAll()
            closeAll(incognito = true, processExit = true)
        }
    }

    override fun toString(): String = formatStatus(false)

    private fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        preempt {
            log.info("Closing all web drivers | {}", formatStatus(verbose = true))
            cancelAll()
            if (processExit) {
                driverPool.use { it.close() }
            } else {
                driverPool.closeAll(incognito)
            }
        }
    }

    private fun formatStatus(verbose: Boolean = false): String {
        val p = driverPool
        return if (verbose) {
            String.format("online: %d, free: %d, waiting: %d, working: %d, active: %d",
                    p.numOnline, p.numFree, p.numWaiting.get(), p.numWorking.get(), p.numActive)
        } else {
            String.format("%d/%d/%d/%d/%d (online/free/waiting/working/active)",
                    p.numOnline, p.numFree, p.numWaiting.get(), p.numWorking.get(), p.numActive)
        }
    }
}
