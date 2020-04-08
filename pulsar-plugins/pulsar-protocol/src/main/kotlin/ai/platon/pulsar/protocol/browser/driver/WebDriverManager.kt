package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyManagerFactory
import ai.platon.pulsar.common.readable
import com.codahale.metrics.MetricRegistry
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    val numReset = AtomicInteger()
    val pageViews = AtomicInteger()
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val speed get() = 1.0 * pageViews.get() / elapsedTime.seconds

    fun allocate(n: Int, volatileConfig: VolatileConfig) {
        allocate(0, n, volatileConfig)
    }

    /**
     * Allocate [n] drivers with priority [priority]
     * */
    fun allocate(priority: Int, n: Int, volatileConfig: VolatileConfig) {
        freeze {
            repeat(n) {
                var driver: ManagedWebDriver? = null
                try {
                    driver = driverPool.poll(priority, volatileConfig)
                } finally {
                    driver?.let { driverPool.put(it) }
                }
            }
        }
    }

    suspend fun <R> submit(priority: Int, volatileConfig: VolatileConfig, action: suspend (driver: ManagedWebDriver) -> R): R {
        val driver = driverPool.poll(priority, volatileConfig)
        return try {
            driver.startWork()
            action(driver)
        } finally {
            driverPool.put(driver)
            driver.stat.pageViews++
            pageViews.incrementAndGet()
        }
    }

    /**
     * Run an action in this pool
     * */
    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        return whenUnfrozen {
            val driver = driverPool.poll(priority, volatileConfig)
            try {
                driver.startWork()
                action(driver)
            } finally {
                driverPool.put(driver)
                driver.stat.pageViews++
                pageViews.incrementAndGet()
            }
        }
    }

    /**
     * Cancel the fetch task specified by [url] remotely
     * */
    fun cancel(url: String): ManagedWebDriver? {
        return freeze {
            driverPool.workingDrivers.values.firstOrNull { it.url == url }?.also { it.cancel() }
        }
    }

    /**
     * Cancel all the fetch tasks remotely
     * */
    fun cancelAll() {
        freeze {
            driverPool.workingDrivers.values.forEach { it.cancel() }
        }
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun reset() {
        freeze {
            numReset.incrementAndGet()
            cancelAll()
            closeAll(incognito = true)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(true, true)
        }
    }

    override fun toString(): String {
        return formatStatus(true)
    }

    private fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        freeze {
            log.info("Closing all web drivers ... ")
            log.info(formatStatus(verbose = true))
            if (processExit) {
                driverPool.use { it.close() }
            } else {
                driverPool.closeAll(incognito)
            }
            log.info("Total ${driverPool.numQuit} drivers were quit | {}", formatStatus(true))
        }
    }

    private fun formatStatus(verbose: Boolean = false): String {
        val p = driverPool
        return if (verbose) {
            String.format("online: %d, free: %d, working: %d, active: %d," +
                    " | crashed: %d, retired: %d, quit: %d, reset: %d," +
                    " | pages: %d, elapsed: %s, speed: %.2f pages/s",
                    p.numOnline, p.numFree, p.numWorking, p.numActive,
                    p.numCrashed.get(), p.numRetired.get(), p.numQuit.get(), numReset.get(),
                    pageViews.get(), elapsedTime.readable(), speed
            )
        } else {
            String.format("%d/%d/%d/%d/%d/%d (free/working/active/online/crashed/retired)",
                    p.numFree, p.numWorking, p.numActive, p.numOnline,
                    p.numCrashed.get(), p.numRetired.get())
        }
    }
}
