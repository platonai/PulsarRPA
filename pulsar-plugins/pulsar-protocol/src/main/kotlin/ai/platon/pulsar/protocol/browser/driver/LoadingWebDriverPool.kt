package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.AppConstants.BROWSER_TAB_REQUIRED_MEMORY
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_POOL_IDLE_TIMEOUT
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
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
): Parameterized, AutoCloseable {

    companion object {
        var CLOSE_ALL_TIMEOUT = Duration.ofSeconds(60)
        var POLLING_TIMEOUT = Duration.ofSeconds(60)
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)

    private var _browser: Browser? = null
    private val _onlineDrivers = ConcurrentLinkedQueue<WebDriver>()
    private val _freeDrivers = ArrayBlockingQueue<WebDriver>(2 * capacity)

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val closed = AtomicBoolean()
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available

    private val registry = AppMetrics.defaultMetricRegistry

    val id = instanceSequencer.incrementAndGet()
    val capacity get() = immutableConfig.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)
    val onlineDrivers: Collection<WebDriver> get() = _onlineDrivers
    val freeDrivers: Collection<WebDriver> get() = _freeDrivers

    val counterRetired = registry.counter(this, "retired")
    val counterQuit = registry.counter(this, "quit")

    var isRetired = false
    val isActive get() = !isRetired && !closed.get() && AppContext.isActive
    val launched = AtomicBoolean()
    val numWaiting = AtomicInteger()
    val numWorking = AtomicInteger()
    val numFree get() = freeDrivers.size
    val numActive get() = numWorking.get() + numFree
    val numAvailable get() = capacity - numWorking.get()
    val numOnline get() = onlineDrivers.size

    var lastActiveTime = Instant.now()
    var idleTimeout = immutableConfig.getDuration(BROWSER_DRIVER_POOL_IDLE_TIMEOUT, Duration.ofMinutes(10))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isIdle get() = (numWorking.get() == 0 && idleTime > idleTimeout)

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
    fun poll(): WebDriver? {
        numWaiting.incrementAndGet()
        return _freeDrivers.poll().also { numWaiting.decrementAndGet() }
    }

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
        return poll0(priority, conf, timeout, unit).also {
            numWorking.incrementAndGet()
            lastActiveTime = Instant.now()
        }
    }

    fun put(driver: WebDriver) {
        try {
            numWorking.decrementAndGet()

            // close open tabs to reduce memory usage
            if (availableMemory < BROWSER_TAB_REQUIRED_MEMORY) {
                if (numOnline > 0.5 * capacity) {
                    driver.retire()
                }
            }

            if (numOnline > capacity) {
                driver.retire()
            }

            if (driver.isWorking) offer(driver) else dismiss(driver)
        } finally {
            lastActiveTime = Instant.now()

            if (numWorking.get() == 0) {
                lock.withLock { notBusy.signalAll() }
            }
        }
    }

    fun forEach(action: (WebDriver) -> Unit) = onlineDrivers.forEach(action)

    fun firstOrNull(predicate: (WebDriver) -> Boolean) = onlineDrivers.firstOrNull(predicate)

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        _freeDrivers.clear()
        onlineDrivers.forEach { it.cancel() }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeGracefully(Duration.ofSeconds(5))
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

        val time = idleTime.readable()
        return when {
            isIdle -> "[Idle] $time | $status"
            isRetired -> "[Retired] | $status"
            else -> status
        }
    }

    override fun toString(): String = formatStatus(false)

    @Synchronized
    private fun offer(driver: WebDriver) {
        _freeDrivers.offer(driver.apply { free() })
    }

    @Synchronized
    private fun dismiss(driver: WebDriver, external: Boolean = true) {
        if (external && !isActive) {
            return
        }

        counterRetired.inc()
        _freeDrivers.remove(driver)
        driver.runCatching { quit().also { counterQuit.inc() } }.onFailure {
            logger.warn("[Unexpected] Quit $driver", it)
        }
        _onlineDrivers.remove(driver)
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    private fun poll0(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver {
        createDriverIfNecessary(priority, conf)

        numWaiting.incrementAndGet()
        val driver = try {
            _freeDrivers.poll(timeout, unit)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } finally {
            numWaiting.decrementAndGet()
        }

        return driver ?: throw WebDriverPoolExhaustedException("Driver pool is exhausted (" + formatStatus() + ")")
    }

    @Throws(BrowserLaunchException::class)
    private fun createDriverIfNecessary(priority: Int, volatileConfig: VolatileConfig) {
        synchronized(driverFactory) {
            try {
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
        return isActive && availableMemory > BROWSER_TAB_REQUIRED_MEMORY && onlineDrivers.size < capacity
    }

    @Throws(BrowserLaunchException::class)
    private fun createWebDriver(volatileConfig: VolatileConfig) {
        // val b = browserManager.launch(browserId, driverSettings, capabilities) as ChromeDevtoolsBrowser
        // val driver = b.newDriver()

        val (browser, driver) = driverFactory.create(browserId, priority, volatileConfig, start = false)

        lock.withLock {
            _browser = browser
            _freeDrivers.add(driver)
            _onlineDrivers.add(driver)
        }

        if (logger.isDebugEnabled) {
            logDriverOnline(driver)
        }
    }

    private fun closeGracefully(timeToWait: Duration) {
        _freeDrivers.clear()
        _onlineDrivers.forEach { it.cancel() }
        _onlineDrivers.clear()

//        val nonSynchronized = onlineDrivers.toList().also { _onlineDrivers.clear() }
//        nonSynchronized.forEach { it.cancel() }

        waitUntilIdle(timeToWait)

        // close drivers by browser
        // closeAllDrivers(nonSynchronized)
    }

    /**
     * Wait until idle.
     * @see [ArrayBlockingQueue#take]
     * @throws InterruptedException if the current thread is interrupted
     * */
    private fun waitUntilIdle(timeout: Duration) {
        try {
            lock.lockInterruptibly()
            notBusy.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
        } finally {
            lock.unlock()
        }
    }

    private fun logDriverOnline(driver: WebDriver) {
        val driverSettings = driverFactory.driverSettings
        logger.trace("The {}th web driver is online, browser: {} pageLoadStrategy: {} capacity: {}",
            numOnline, driver.name,
            driverSettings.pageLoadStrategy, capacity)
    }
}
