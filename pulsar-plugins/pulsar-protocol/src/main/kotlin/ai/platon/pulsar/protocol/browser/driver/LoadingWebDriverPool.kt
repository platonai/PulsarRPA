package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.AppConstants.BROWSER_TAB_REQUIRED_MEMORY
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_POOL_IDLE_TIMEOUT
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class LoadingWebDriverPool constructor(
    val browserId: BrowserId,
    val priority: Int = 0,
    val driverFactory: WebDriverFactory,
    val immutableConfig: ImmutableConfig
) {

    companion object {
        var CLOSE_ALL_TIMEOUT = Duration.ofSeconds(60)
        var POLLING_TIMEOUT = Duration.ofSeconds(60)
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)

    /**
     * The browser who create all drivers for this pool.
     * */
    private var _browser: Browser? = null
    /**
     * Keep standby drivers
     * */
    private val _standbyDrivers = ArrayBlockingQueue<WebDriver>(2 * capacity)
    /**
     * Keep working drivers
     * */
    private val _workingDrivers = ConcurrentLinkedQueue<WebDriver>()
    /**
     * Keep retired drivers
     * */
    private val _retiredDrivers = ConcurrentLinkedQueue<WebDriver>()
    /**
     * Lock for not-busy condition
     * */
    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available

    private val registry = AppMetrics.defaultMetricRegistry

    val id = instanceSequencer.incrementAndGet()
    val capacity get() = immutableConfig.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)
    /**
     * Standby drivers
     * */
    val standbyDrivers: Collection<WebDriver> get() = _standbyDrivers
    /**
     * Working drivers
     * */
    val workingDrivers: Collection<WebDriver> get() = _workingDrivers
    /**
     * Standby drivers and working drivers
     * */
    val onlineDrivers: Collection<WebDriver> get() = _standbyDrivers + _workingDrivers

    val counterRetired = registry.counter(this, "retired")
//    val counterClosed = registry.counter(this, "closed")

    /**
     * Retired but not closed yet.
     * */
    private var isRetired = false
    val isActive get() = !isRetired && AppContext.isActive
    val launched = AtomicBoolean()
    private val _numDrivers = AtomicInteger()
    private val _numWaiting = AtomicInteger()
    val numWaiting get() = _numWaiting.get()
    /**
     * Number of drivers on standby.
     * */
    val numStandby get() = standbyDrivers.size
    /**
     * Number of drivers at work.
     * */
    val numWorking get() = workingDrivers.size
    /**
     * Number of retired drivers.
     * */
    val numRetired get() = _retiredDrivers.size
    /**
     * Number of active drivers.
     * */
    val numActive get() = numWorking + numStandby
    /**
     * Number of created drivers, should be equal to numStandby + numWorking + numRetired.
     * */
    val numCreated get() = _numDrivers.get()
    /**
     * Number of available slots to allocate new drivers
     * */
    val numAvailable get() = capacity - numStandby - numWorking
    /**
     * Number of online drivers, including both working drivers and standby drivers.
     * */
    val numOnline get() = onlineDrivers.size

    var lastActiveTime: Instant = Instant.now()
        private set
    val idleTimeout get() = immutableConfig.getDuration(BROWSER_DRIVER_POOL_IDLE_TIMEOUT, Duration.ofMinutes(10))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    /**
     * Check if the pool is idle.
     *
     * TODO: why numWorking == 0 is required?
     * */
    val isIdle get() = (numWorking == 0 && idleTime > idleTimeout)

    /**
     * Allocate [capacity] drivers
     * */
    fun allocate(conf: VolatileConfig) {
        repeat(capacity) {
            runCatching { put(poll(priority, conf, POLLING_TIMEOUT.seconds, TimeUnit.SECONDS)) }.onFailure {
                logger.warn("Unexpected exception", it)
            }
        }
    }

    /**
     * Retrieves and removes the head of this free driver queue,
     * or returns {@code null} if there is no free drivers.
     *
     * @return the head of the free driver queue, or {@code null} if this queue is empty
     */
    fun poll(): WebDriver = poll(VolatileConfig.UNSAFE)

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    fun poll(conf: VolatileConfig): WebDriver = poll(0, conf, POLLING_TIMEOUT.seconds, TimeUnit.SECONDS)

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    fun poll(conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver = poll(0, conf, timeout, unit)

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Duration): WebDriver {
        return poll(0, conf, timeout.seconds, TimeUnit.SECONDS)
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver {
        val driver = poll0(priority, conf, timeout, unit)
        return driver ?: throw WebDriverPoolExhaustedException("Driver pool is exhausted (" + report() + ")")
    }

    fun put(driver: WebDriver) {
        lastActiveTime = Instant.now()

        try {
            _workingDrivers.remove(driver)
            put0(driver)
        } finally {
            if (_workingDrivers.isEmpty()) {
                lock.withLock { notBusy.signalAll() }
            }
        }
    }

    private fun put0(driver: WebDriver) {
        // close open tabs to reduce memory usage
        if (availableMemory < BROWSER_TAB_REQUIRED_MEMORY) {
            if (numOnline > 0.5 * capacity) {
                driver.retire()
            }
        }

        if (numOnline > capacity) {
            driver.retire()
        }

        if (driver.isWorking) {
            offer(driver)
        } else {
            dismiss(driver)
        }
    }

    fun forEach(action: (WebDriver) -> Unit) = onlineDrivers.forEach(action)

    fun firstOrNull(predicate: (WebDriver) -> Boolean) = onlineDrivers.firstOrNull(predicate)

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        _standbyDrivers.clear()
        onlineDrivers.forEach { it.cancel() }
    }

    fun report(verbose: Boolean = false): String {
        val p = this
        val status = if (verbose) {
            String.format("online: %d, free: %d, waiting: %d, working: %d, active: %d",
                    p.numOnline, p.numStandby, p.numWaiting, p.numWorking, p.numActive)
        } else {
            String.format("%d/%d/%d/%d/%d (online/free/waiting/working/active)",
                    p.numOnline, p.numStandby, p.numWaiting, p.numWorking, p.numActive)
        }

        val time = idleTime.readable()
        return when {
            isIdle -> "[Idle] $time | $status"
            isRetired -> "[Retired] | $status"
            else -> status
        }
    }

    /**
     * Force the page stop all navigations.
     * Mark the driver pool be retired, but not closed yet.
     * */
    @Synchronized
    fun retire() {
        isRetired = true

        val drivers = _standbyDrivers + _workingDrivers
        _standbyDrivers.clear()
        _workingDrivers.clear()
        _retiredDrivers.addAll(drivers)

        drivers.forEach { driver ->
            driver.cancel()
            driver.retire()
            kotlin.runCatching { runBlocking { driver.stop() } }
        }
    }

    @Synchronized
    fun close() {
        _standbyDrivers.clear()
        _workingDrivers.clear()
        _retiredDrivers.clear()
    }

    @Synchronized
    override fun toString(): String = report(false)

    @Synchronized
    private fun offer(driver: WebDriver) {
        _standbyDrivers.offer(driver.apply { free() })
    }

    @Synchronized
    private fun dismiss(driver: WebDriver, external: Boolean = true) {
        if (external && !isActive) {
            return
        }

        _standbyDrivers.remove(driver)
        _workingDrivers.remove(driver)
        _retiredDrivers.add(driver)

        counterRetired.inc()
//        runCatching { driver.close() }.onFailure { logger.warn(it.brief("[Unexpected] Quit $driver")) }
//        counterClosed.inc()
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    private fun poll0(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver? {
        createDriverIfNecessary(priority, conf)

        _numWaiting.incrementAndGet()
        val driver = try {
            _standbyDrivers.poll(timeout, unit)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } finally {
            _numWaiting.decrementAndGet()
        }

        if (driver != null) {
            driver.startWork()
            _workingDrivers.add(driver)
        }

        lastActiveTime = Instant.now()

        return driver
    }

    @Throws(BrowserLaunchException::class)
    private fun createDriverIfNecessary(priority: Int, volatileConfig: VolatileConfig) {
        synchronized(driverFactory) {
            try {
                // create driver from remote unmanaged tabs, close unmanaged idle drivers, etc
                if (shouldCreateDriver()) {
                    createWebDriver(volatileConfig)
                }
            } catch (e: BrowserLaunchException) {
                logger.debug("[Unexpected]", e)

                if (isActive) {
                    throw e
                }
            }
        }
    }

    private fun shouldCreateDriver(): Boolean {
        return isActive && availableMemory > BROWSER_TAB_REQUIRED_MEMORY && numActive < capacity
    }

    @Throws(BrowserLaunchException::class)
    private fun createWebDriver(volatileConfig: VolatileConfig) {
        // TODO: the code below might be better
        // val b = browserManager.launch(browserId, driverSettings, capabilities)
        // val driver = b.newDriver()

        val driver = driverFactory.create(browserId, priority, volatileConfig, start = false)
        _numDrivers.incrementAndGet()

        lock.withLock {
            _browser = driver.browser
            _standbyDrivers.add(driver)
        }

        if (logger.isDebugEnabled) {
            logDriverOnline(driver)
        }
    }

    private fun logDriverOnline(driver: WebDriver) {
        val driverSettings = driverFactory.driverSettings
        logger.trace("The {}th web driver is online, browser: {} pageLoadStrategy: {} capacity: {}",
            numOnline, driver.name,
            driverSettings.pageLoadStrategy, capacity)
    }
}
