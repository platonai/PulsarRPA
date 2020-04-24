package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_HEADLESS
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_POOL_CAPACITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class LoadingWebDriverPool(
        val driverFactory: WebDriverFactory,
        val conf: ImmutableConfig
): Parameterized, AutoCloseable {

    private val log = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)
    val capacity = conf.getInt(BROWSER_POOL_CAPACITY, AppConstants.DEFAULT_NUM_BROWSERS)
    private val onlineDrivers = ConcurrentSkipListSet<ManagedWebDriver>()
    private val freeDrivers = ArrayBlockingQueue<ManagedWebDriver>(capacity)

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val isHeadless = conf.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val instanceRequiredMemory = 200 * 1024 * 1024 // 200 MiB

    private val metrics = SharedMetricRegistries.getDefault()
    val counterRetired = metrics.counter(MetricRegistry.name(javaClass, "retired"))
    val counterQuit = metrics.counter(MetricRegistry.name(javaClass, "quit"))

    val isClosed get() = closed.get()
    val isActive get() = !isClosed && PulsarEnv.isActive
    val numWorking = AtomicInteger()
    val numFree get() = freeDrivers.size
    val numActive get() = numWorking.get() + numFree
    val numOnline get() = onlineDrivers.size

    fun take(conf: ImmutableConfig): ManagedWebDriver = take(0, conf)

    fun take(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        return take0(priority, conf).also { numWorking.incrementAndGet() }
    }

    fun put(driver: ManagedWebDriver) {
        if (numWorking.decrementAndGet() == 0) {
            lock.withLock { notBusy.signalAll() }
        }

        if (driver.isRetired) retire(driver) else offer(driver)
    }

    fun forEach(action: (ManagedWebDriver) -> Unit) = onlineDrivers.forEach(action)

    fun firstOrNull(predicate: (ManagedWebDriver) -> Boolean) = onlineDrivers.firstOrNull(predicate)

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        if (!processExit) {
            waitUntilIdle()
        }

        closeAllDrivers(processExit)

        if (incognito) {
            // Force delete all browser data
            forceDeleteBrowserDataDir()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(incognito = true, processExit = true)
        }
    }

    override fun toString(): String = "$numFree/$numWorking/$numActive/$numOnline (free/working/active/online)"

    private fun offer(driver: ManagedWebDriver) = freeDrivers.offer(driver.apply { free() })

    private fun retire(driver: ManagedWebDriver, external: Boolean = true) {
        if (external && isClosed) {
            return
        }

        counterRetired.inc()
        freeDrivers.remove(driver.apply { retire() })
        driver.runCatching { quit().also { counterQuit.inc() } }
                .onFailure { log.warn("Exception occurs when quit $driver {}", Strings.simplifyException(it)) }
    }

    private fun take0(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        driverFactory.takeIf { isActive && onlineDrivers.size < capacity && availableMemory > instanceRequiredMemory }
                ?.create(priority, conf)
                ?.also {
                    freeDrivers.add(it)
                    onlineDrivers.add(it)
                    logDriverOnline(it)
                }

        return freeDrivers.take()
    }

    private fun closeAllDrivers(processExit: Boolean = false) {
        if (!isHeadless || onlineDrivers.isEmpty()) {
            return
        }

        freeDrivers.clear()

        // create a non-synchronized list for quitting all drivers in parallel
        val nonSynchronized = onlineDrivers.toList()
        onlineDrivers.clear()
        nonSynchronized.parallelStream().forEach {
            it.quit()
            counterQuit.inc()
        }
    }

    /**
     * Force delete all browser data
     * */
    private fun forceDeleteBrowserDataDir() {
        // TODO: delete data that might leak privacy only, cookies, sessions, local storage, etc
        synchronized(LoadingWebDriverPool::class.java) {
            val lock = AppPaths.BROWSER_TMP_DIR_LOCK
            val pathToDelete = AppPaths.BROWSER_TMP_DIR

            val maxTry = 10
            var i = 0
            while (i++ < maxTry && Files.exists(pathToDelete)) {
                FileChannel.open(lock, StandardOpenOption.APPEND).use {
                    it.lock()
                    kotlin.runCatching { FileUtils.deleteDirectory(pathToDelete.toFile()) }
                            .onFailure { log.warn(Strings.simplifyException(it)) }
                }
            }
        }
    }

    private fun waitUntilIdle() {
        lock.withLock {
            var i = 0
            while (!isClosed && numWorking.get() > 0 && i++ < 120) {
                notBusy.await(1, TimeUnit.SECONDS)
            }
        }
    }

    private fun logDriverOnline(driver: ManagedWebDriver) {
        if (log.isTraceEnabled) {
            val driverControl = driverFactory.driverControl
            log.trace("The {}th web driver is online, browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {}",
                    numOnline,
                    driver.name,
                    driverControl.imagesEnabled,
                    driverControl.pageLoadStrategy, capacity)
        }
    }
}
