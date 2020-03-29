package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyManagerFactory
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
    val driverPool = WebDriverPool(driverFactory, immutableConfig)

    private val closed = AtomicBoolean()
    private var lastActiveTime = Instant.now()
    private var idleTimeout = Duration.ofMinutes(5)

    val startTime = Instant.now()
    val numReset = AtomicInteger()
    val pageViews = AtomicInteger()
    val isIdle get() = driverPool.numWorking == 0 && idleTime > idleTimeout
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val speed get() = 1.0 * pageViews.get() / elapsedTime.seconds
    val isClosed get() = closed.get()

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

    /**
     * Run an action in this pool
     * */
    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        return whenUnfrozen {
            var driver: ManagedWebDriver? = null
            try {
                driver = driverPool.poll(priority, volatileConfig)
                driver.startWork()
                action(driver)
            } finally {
                if (driver != null) {
                    lastActiveTime = Instant.now()

                    driverPool.put(driver)

                    driver.stat.pageViews++
                    pageViews.incrementAndGet()
                }
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

    fun formatStatus(verbose: Boolean = false): String {
        return if (verbose) {
            String.format("online: %d, free: %d, working: %d, active: %d," +
                    " | crashed: %d, retired: %d, quit: %d, reset: %d," +
                    " | pages: %d, elapsed: %s, speed: %.2f, pages/s",
                    driverPool.numOnline, driverPool.numFree, driverPool.numWorking, driverPool.numActive,
                    driverPool.numCrashed.get(), driverPool.numRetired.get(), driverPool.numQuit.get(), numReset.get(),
                    pageViews.get(), DateTimes.readableDuration(elapsedTime), speed
            )
        } else {
            String.format("%d/%d/%d/%d/%d/%d (free/working/active/online/crashed/retired)",
                    driverPool.numFree, driverPool.numWorking, driverPool.numActive, driverPool.numOnline,
                    driverPool.numCrashed.get(), driverPool.numRetired.get())
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
}
