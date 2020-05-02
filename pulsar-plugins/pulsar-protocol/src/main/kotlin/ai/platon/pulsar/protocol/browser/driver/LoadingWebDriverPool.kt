package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_HEADLESS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.prependReadableClassName
import com.codahale.metrics.SharedMetricRegistries
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
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
    private val concurrency = conf.getInt(CapabilityTypes.FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    val capacity = conf.getInt(CapabilityTypes.BROWSER_POOL_CAPACITY, concurrency)
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
    val counterRetired = metrics.counter(prependReadableClassName(this, "retired"))
    val counterQuit = metrics.counter(prependReadableClassName(this, "quit"))

    val isActive get() = !closed.get() && PulsarEnv.isActive
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
            waitUntilIdleOrTimeout(Duration.ofMinutes(2))
        }

        closeAllDrivers(processExit)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(incognito = true, processExit = true)
        }
    }

    override fun toString(): String = "$numFree/$numWorking/$numActive/$numOnline (free/working/active/online)"

    private fun offer(driver: ManagedWebDriver) = freeDrivers.offer(driver.apply { free() })

    private fun retire(driver: ManagedWebDriver, external: Boolean = true) {
        if (external && !isActive) {
            return
        }

        counterRetired.inc()
        freeDrivers.remove(driver.apply { retire() })
        driver.runCatching { quit().also { counterQuit.inc() } }
                .onFailure { log.warn("Exception occurs when quit $driver {}", Strings.simplifyException(it)) }
    }

    @Throws(InterruptedException::class)
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
        if (!isHeadless) {
            return
        }

        freeDrivers.clear()

        // create a non-synchronized list for quitting all drivers in parallel
        val nonSynchronized = onlineDrivers.toList().also { onlineDrivers.clear() }
        nonSynchronized.parallelStream().forEach {
            it.quit().also { counterQuit.inc() }
            log.info("Quit driver {}", it)
        }
    }

    private fun waitUntilIdleOrTimeout(timeout: Duration = Duration.ofMinutes(2)) {
        lock.withLock {
            var i = 0
            val pollingInterval = Duration.ofSeconds(1)
            while (isActive && numWorking.get() > 0 && i++ < timeout.seconds) {
                notBusy.await(pollingInterval.seconds, TimeUnit.SECONDS)
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
