package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.config.AppConstants.BROWSER_TAB_REQUIRED_MEMORY
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
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
    val capacity get() = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)
    val onlineDrivers = ConcurrentSkipListSet<WebDriver>()
    val freeDrivers = ArrayBlockingQueue<WebDriver>(2 * capacity)

    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()
    private val notEmpty = lock.newCondition()

    private val driverSettings get() = driverFactory.driverSettings
    private val closed = AtomicBoolean()
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available

    private val registry = AppMetrics.defaultMetricRegistry
    val counterRetired = registry.counter(this, "retired")
    val counterQuit = registry.counter(this, "quit")

    var isRetired = false
    val isActive get() = !isRetired && !closed.get()
    val numWaiting = AtomicInteger()
    val numWorking = AtomicInteger()
    val numTasks = AtomicInteger()
    val numSuccess = AtomicInteger()
    val numTimeout = AtomicInteger()
    val numDismissWarnings = AtomicInteger()
    val maxDismissWarnings = 5
    val shouldRetire get() = numDismissWarnings.get() > maxDismissWarnings
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

    fun poll(): WebDriver? {
        numWaiting.incrementAndGet()
        return freeDrivers.poll().also { numWaiting.decrementAndGet() }
    }

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(conf: VolatileConfig): WebDriver = poll(0, conf, POLLING_TIMEOUT.seconds, TimeUnit.SECONDS)

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver = poll(0, conf, timeout, unit)

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Duration): WebDriver {
        return poll(0, conf, timeout.seconds, TimeUnit.SECONDS)
    }

    @Throws(WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver {
        return poll0(priority, conf, timeout, unit).also {
            numWorking.incrementAndGet()
            lastActiveTime = Instant.now()
        }
    }

    fun put(driver: WebDriver) {
        if (numWorking.decrementAndGet() == 0) {
            lock.withLock { notBusy.signalAll() }
        }

        // close open tabs to reduce memory usage
        if (availableMemory < BROWSER_TAB_REQUIRED_MEMORY) {
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

    fun forEach(action: (WebDriver) -> Unit) = onlineDrivers.forEach(action)

    fun firstOrNull(predicate: (WebDriver) -> Boolean) = onlineDrivers.firstOrNull(predicate)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                doClose(CLOSE_ALL_TIMEOUT)
            } catch (e: InterruptedException) {
                log.warn("Thread interrupted when closing | {}", this)
                Thread.currentThread().interrupt()
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

        val time = idleTime.readable()
        return when {
            isIdle -> "[Idle] $time | $status"
            isRetired -> "[Retired] $time | $status"
            else -> status
        }
    }

    override fun toString(): String = formatStatus(false)

    @Synchronized
    private fun offer(driver: WebDriver) {
        freeDrivers.offer(driver.apply { free() })
        lock.withLock { notEmpty.signalAll() }
    }

    @Synchronized
    private fun retire(driver: WebDriver, external: Boolean = true) {
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

    @Throws(WebDriverPoolException::class)
    private fun poll0(priority: Int, conf: VolatileConfig? = null, timeout: Long, unit: TimeUnit): WebDriver {
        if (conf != null) {
            createDriverIfNecessary(priority, conf)
        }

        numWaiting.incrementAndGet()
        val driver = try {
            freeDrivers.poll(timeout, unit)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw WebDriverPoolException(e)
        } finally {
            numWaiting.decrementAndGet()
        }

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
        return isActive && availableMemory > BROWSER_TAB_REQUIRED_MEMORY && onlineDrivers.size < capacity
    }

    private fun doClose(timeToWait: Duration) {
        freeDrivers.clear()

        val nonSynchronized = onlineDrivers.toList().also { onlineDrivers.clear() }
        nonSynchronized.parallelStream().forEach { it.cancel() }

        waitUntilIdleOrTimeout(timeToWait)

        closeAllDrivers(nonSynchronized)
    }

    private fun closeAllDrivers(drivers: List<WebDriver>) {
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
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun logDriverOnline(driver: WebDriver) {
        if (log.isTraceEnabled) {
            val driverSettings = driverFactory.driverSettings
            log.trace("The {}th web driver is online, browser: {} pageLoadStrategy: {} capacity: {}",
                    numOnline, driver.name,
                driverSettings.pageLoadStrategy, capacity)
        }
    }

    private fun checkState() {
        if (!isActive) {
            throw WebDriverPoolException("Loading web driver pool is closed | $this | $browserInstanceId")
        }
    }
}
