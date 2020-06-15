package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_HEADLESS
import ai.platon.pulsar.crawl.BrowserInstanceId
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
        private val browserInstanceId: BrowserInstanceId,
        private val priority: Int = 0,
        private val driverFactory: WebDriverFactory,
        immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {

    companion object {
        val CLOSE_ALL_TIMEOUT = Duration.ofSeconds(60)
    }

    private val log = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)
    val concurrency = immutableConfig.getInt(CapabilityTypes.FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    val capacity = immutableConfig.getInt(CapabilityTypes.BROWSER_POOL_CAPACITY, concurrency)
    val onlineDrivers = ConcurrentSkipListSet<ManagedWebDriver>()
    val freeDrivers = ArrayBlockingQueue<ManagedWebDriver>(500)

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()
    private val notEmpty = lock.newCondition()

    private val isHeadless = immutableConfig.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val instanceRequiredMemory = 200 * 1024 * 1024 // 200 MiB

    val counterRetired = MetricsManagement.counter(this, "retired")
    val counterQuit = MetricsManagement.counter(this, "quit")

    val isActive get() = !closed.get()
    val numWaiting = AtomicInteger()
    val numWorking = AtomicInteger()
    val numFree get() = freeDrivers.size
    val numActive get() = numWorking.get() + numFree
    val numOnline get() = onlineDrivers.size

    /**
     * Allocate [concurrency] drivers
     * */
    fun allocate(volatileConfig: VolatileConfig) {
        repeat(concurrency) {
            runCatching { put(take(priority, volatileConfig)) }.onFailure { log.warn("Unexpected exception", it) }
        }
    }

    fun take(conf: VolatileConfig): ManagedWebDriver = take(0, conf)

    fun take(priority: Int, conf: VolatileConfig) = take0(priority, conf).also { numWorking.incrementAndGet() }

    fun put(driver: ManagedWebDriver) {
        if (numWorking.decrementAndGet() == 0) {
            lock.withLock { notBusy.signalAll() }
        }

        if (driver.isRetired) retire(driver) else offer(driver)
    }

    fun forEach(action: (ManagedWebDriver) -> Unit) = onlineDrivers.forEach(action)

    fun firstOrNull(predicate: (ManagedWebDriver) -> Boolean) = onlineDrivers.firstOrNull(predicate)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                doClose(CLOSE_ALL_TIMEOUT)
            } catch (e: InterruptedException) {
                log.warn("Thread interrupted when closing | {}", this)
            }
        }
    }

    fun formatStatus(verbose: Boolean = false): String {
        val p = this
        return if (verbose) {
            String.format("online: %d, free: %d, waiting: %d, working: %d, active: %d",
                    p.numOnline, p.numFree, p.numWaiting.get(), p.numWorking.get(), p.numActive)
        } else {
            String.format("%d/%d/%d/%d/%d (online/free/waiting/working/active)",
                    p.numOnline, p.numFree, p.numWaiting.get(), p.numWorking.get(), p.numActive)
        }
    }

    override fun toString(): String = formatStatus(false)

    private fun offer(driver: ManagedWebDriver) {
        freeDrivers.offer(driver.apply { free() })
        lock.withLock { notEmpty.signalAll() }
    }

    private fun retire(driver: ManagedWebDriver, external: Boolean = true) {
        if (external && !isActive) {
            return
        }

        counterRetired.inc()
        freeDrivers.remove(driver.apply { retire() })
        driver.runCatching { quit().also { counterQuit.inc() } }.onFailure {
            log.warn("Unexpected exception quit $driver", it)
        }
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    private fun take0(priority: Int, conf: VolatileConfig): ManagedWebDriver {
        checkState()

        driverFactory.takeIf { onlineDrivers.size < capacity && availableMemory > instanceRequiredMemory }
                ?.create(browserInstanceId, priority, conf)
                ?.also {
                    lock.withLock {
                        freeDrivers.add(it)
                        notEmpty.signalAll()
                        onlineDrivers.add(it)
                    }
                    logDriverOnline(it)
                }

        numWaiting.incrementAndGet()
        var driver = freeDrivers.poll()
        while (isActive && driver == null) {
            lock.withLock { notEmpty.await(1, TimeUnit.SECONDS) }
            driver = freeDrivers.poll()
        }
        numWaiting.decrementAndGet()

        return driver?:throw IllegalStateException("LoadingWebDriverPool is closed")
    }

    private fun doClose(timeToWait: Duration) {
        freeDrivers.clear()

        val nonSynchronized = onlineDrivers.toList().also { onlineDrivers.clear() }
        nonSynchronized.parallelStream().forEach { it.cancel() }

        waitUntilIdleOrTimeout(timeToWait)

        closeAllDrivers(nonSynchronized)
    }

    private fun closeAllDrivers(drivers: List<ManagedWebDriver>) {
        if (!isHeadless) {
            // return
        }

        val i = AtomicInteger()
        val total = drivers.size
        val timeout = Duration.ofSeconds(20)

        drivers.parallelStream().forEach { driver ->
            driver.quit().also { counterQuit.inc() }
            log.info("Quit driver {}/{} {}", i.incrementAndGet(), total, driver)
        }
    }

    @Synchronized
    private fun waitUntilIdleOrTimeout(timeout: Duration) {
        lock.withLock {
            val ttl = timeout.seconds
            var i = 0
            try {
                while (isActive && numWorking.get() > 0 && ++i < ttl) {
                    notBusy.await(1, TimeUnit.SECONDS)
                    log.takeIf { i % 20 == 0 }?.info("Round $i/$ttl waiting for idle | $this")
                }
            } catch (ignored: InterruptedException) {}
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

    private fun checkState() {
        if (!isActive) {
            throw IllegalStateException("Loading web driver pool is closed")
        }
    }
}
