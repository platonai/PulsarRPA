package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.AppConstants.BROWSER_DRIVER_INSTANCE_REQUIRED_MEMORY
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.time.Duration
import java.time.Instant
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
        private val conf: ImmutableConfig
): Parameterized, AutoCloseable {

    companion object {
        val CLOSE_ALL_TIMEOUT = Duration.ofSeconds(60)
        val POLLING_TIMEOUT = Duration.ofSeconds(60)
        val instanceSequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)
    val id = instanceSequencer.incrementAndGet()
    val capacity get() = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppConstants.NCPU)
    val onlineDrivers = ConcurrentSkipListSet<ManagedWebDriver>()
    val freeDrivers = ArrayBlockingQueue<ManagedWebDriver>(500)

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()
    private val notEmpty = lock.newCondition()

    private val isHeadless get() = conf.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available

    val counterRetired = MetricsManagement.counter(this, "retired")
    val counterQuit = MetricsManagement.counter(this, "quit")

    val isActive get() = !closed.get()
    val numWaiting = AtomicInteger()
    val numWorking = AtomicInteger()
    val numFree get() = freeDrivers.size
    val numActive get() = numWorking.get() + numFree
    val numAvailable get() = capacity - numWorking.get()
    val numOnline get() = onlineDrivers.size

    var lastActiveTime = Instant.now()
    var idleTimeout = conf.getDuration(BROWSER_DRIVER_POOL_IDLE_TIMEOUT, Duration.ofMinutes(10))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = (numWorking.get() == 0 && idleTime > idleTimeout)

    /**
     * Allocate [capacity] drivers
     * */
    fun allocate(volatileConfig: VolatileConfig) {
        repeat(capacity) {
            runCatching { put(poll(priority, volatileConfig, POLLING_TIMEOUT.seconds, TimeUnit.SECONDS)) }.onFailure {
                log.warn("Unexpected exception", it)
            }
        }
    }

    fun poll(): ManagedWebDriver? {
        checkState()
        numWaiting.incrementAndGet()
        return freeDrivers.poll().also { numWaiting.decrementAndGet() }
    }

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(conf: VolatileConfig): ManagedWebDriver = poll(0, conf, POLLING_TIMEOUT.seconds, TimeUnit.SECONDS)

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(conf: VolatileConfig, timeout: Long, unit: TimeUnit): ManagedWebDriver = poll(0, conf, timeout, unit)

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Duration): ManagedWebDriver {
        return poll(0, conf, timeout.seconds, TimeUnit.SECONDS)
    }

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): ManagedWebDriver {
        return poll0(priority, conf, timeout, unit).also {
            numWorking.incrementAndGet()
            lastActiveTime = Instant.now()
        }
    }

    fun put(driver: ManagedWebDriver) {
        if (!isActive) {
            log.warn("Driver pool is already closed, quit driver immediately | {}", driver)
            driver.runCatching { quit().also { counterQuit.inc() } }.onFailure {
                log.warn("Unexpected exception quit $driver", it)
            }
            return
        }

        if (numWorking.decrementAndGet() == 0) {
            lock.withLock { notBusy.signalAll() }
        }

        // close open tabs to reduce memory usage
        if (availableMemory < BROWSER_DRIVER_INSTANCE_REQUIRED_MEMORY) {
            if (numOnline > 0.5 * capacity) {
                driver.retire()
            }
        }

        if (numOnline > capacity) {
            // use System.setProperty() to reduce capacity
            driver.retire()
        }

        if (driver.isRetired) retire(driver) else offer(driver)

        lastActiveTime = Instant.now()
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
        val status = if (verbose) {
            String.format("online: %d, free: %d, waiting: %d, working: %d, active: %d",
                    p.numOnline, p.numFree, p.numWaiting.get(), p.numWorking.get(), p.numActive)
        } else {
            String.format("%d/%d/%d/%d/%d (online/free/waiting/working/active)",
                    p.numOnline, p.numFree, p.numWaiting.get(), p.numWorking.get(), p.numActive)
        }

        if (isIdle) {
            val time = idleTime.readable()
            return "[Idle] $time | $status"
        }

        return status
    }

    override fun toString(): String = formatStatus(false)

    @Synchronized
    private fun offer(driver: ManagedWebDriver) {
        freeDrivers.offer(driver.apply { free() })
        lock.withLock { notEmpty.signalAll() }
    }

    @Synchronized
    private fun retire(driver: ManagedWebDriver, external: Boolean = true) {
        if (external && !isActive) {
            return
        }

        counterRetired.inc()
        freeDrivers.remove(driver.apply { retire() })
        driver.runCatching { quit().also { counterQuit.inc() } }.onFailure {
            log.warn("Unexpected exception quit $driver", it)
        }
        onlineDrivers.remove(driver)
    }

    @Throws(WebDriverPoolExhaustedException::class)
    private fun poll0(priority: Int, conf: VolatileConfig? = null, timeout: Long, unit: TimeUnit): ManagedWebDriver {
        checkState()

        if (conf != null) {
            createDriverIfNecessary(priority, conf)
        }

        numWaiting.incrementAndGet()
        val driver = freeDrivers.poll(timeout, unit)
        numWaiting.decrementAndGet()

        checkState()
        return driver?:throw WebDriverPoolExhaustedException("Driver pool is exhausted (" + formatStatus() + ")")
    }

    private fun createDriverIfNecessary(priority: Int, conf: VolatileConfig) {
        if (shouldCreateDriver()) {
            synchronized(driverFactory) {
                if (shouldCreateDriver()) {
                    // log.info("Creating the {}/{}th web driver for context {}", numCreate, capacity, browserInstanceId)
                    val driver = driverFactory.create(browserInstanceId, priority, conf)
                    lock.withLock {
                        freeDrivers.add(driver)
                        notEmpty.signalAll()
                        onlineDrivers.add(driver)
                    }
                    logDriverOnline(driver)
                }
            }
        }
    }

    private fun shouldCreateDriver(): Boolean {
        return isActive && availableMemory > BROWSER_DRIVER_INSTANCE_REQUIRED_MEMORY && onlineDrivers.size < capacity
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
        drivers.parallelStream().forEach { driver ->
            driver.quit().also { counterQuit.inc() }
            log.debug("Quit driver {}/{} | {}", i.incrementAndGet(), total, driver)
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
            throw WebDriverPoolException("Loading web driver pool is closed | $this | $browserInstanceId")
        }
    }
}
