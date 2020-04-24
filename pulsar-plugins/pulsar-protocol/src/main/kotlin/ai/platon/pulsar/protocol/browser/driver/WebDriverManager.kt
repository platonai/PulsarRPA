package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyManagerFactory
import ai.platon.pulsar.common.readable
import com.codahale.metrics.MetricRegistry
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
        val proxyManagerFactory: ProxyManagerFactory,
        val immutableConfig: ImmutableConfig
): Parameterized, Freezable(), AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverManager::class.java)

    val proxyManager = proxyManagerFactory.get()
    val driverFactory = WebDriverFactory(driverControl, proxyManager, immutableConfig)
    val driverPool = LoadingWebDriverPool(driverFactory, immutableConfig)

    private val closed = AtomicBoolean()

    val startTime = Instant.now()
    private val metrics = SharedMetricRegistries.getDefault()
    val numReset = metrics.meter(MetricRegistry.name(javaClass, "numReset"))
    val elapsedTime get() = Duration.between(startTime, Instant.now())

    fun allocate(n: Int, volatileConfig: VolatileConfig) = allocate(0, n, volatileConfig)

    /**
     * Allocate [n] drivers with priority [priority]
     * */
    fun allocate(priority: Int, n: Int, volatileConfig: VolatileConfig) {
        freeze {
            repeat(n) { driverPool.runCatching { put(take(priority, volatileConfig)) } }
        }
    }

    suspend fun <R> submit(priority: Int, volatileConfig: VolatileConfig, action: suspend (driver: ManagedWebDriver) -> R): R {
        val driver = driverPool.take(priority, volatileConfig).apply { startWork() }
        return try {
            action(driver)
        } finally {
            driverPool.put(driver)
        }
    }

    /**
     * Run an action in this pool
     * */
    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        return whenUnfrozen {
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
     * */
    fun cancel(url: String): ManagedWebDriver? =
            freeze { driverPool.firstOrNull { it.url == url }?.also { it.cancel() } }

    /**
     * Cancel all the fetch tasks remotely
     * */
    fun cancelAll() = freeze { driverPool.forEach { it.cancel() } }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun reset() {
        freeze {
            numReset.mark()
            cancelAll()
            closeAll(incognito = true)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(incognito = true, processExit = true)
        }
    }

    override fun toString(): String = formatStatus(false)

    private fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        freeze {
            log.info("Closing all web drivers | {}", formatStatus(verbose = true))
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
            String.format("online: %d, free: %d, working: %d, active: %d",
                    p.numOnline, p.numFree, p.numWorking.get(), p.numActive)
        } else {
            String.format("%d/%d/%d/%d (free/working/active/online)",
                    p.numFree, p.numWorking.get(), p.numActive, p.numOnline)
        }
    }
}
