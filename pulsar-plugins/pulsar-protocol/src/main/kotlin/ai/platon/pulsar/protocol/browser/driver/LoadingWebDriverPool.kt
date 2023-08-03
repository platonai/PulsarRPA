package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_POOL_IDLE_TIMEOUT
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.crawl.BrowseEvent
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class LoadingWebDriverPool constructor(
    val browserId: BrowserId,
    val priority: Int = 0,
    val driverPoolManager: WebDriverPoolManager,
    val driverFactory: WebDriverFactory,
    val immutableConfig: ImmutableConfig
) {
    companion object {
        var CLOSE_ALL_TIMEOUT = Duration.ofSeconds(60)
        var POLLING_TIMEOUT = Duration.ofSeconds(60)
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(LoadingWebDriverPool::class.java)

    val id = instanceSequencer.incrementAndGet()

    private val registry = AppMetrics.defaultMetricRegistry

    /**
     * The max number of drivers the pool can hold
     * */
    val capacity get() = immutableConfig.getInt(BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)

    /**
     * The browser who create all drivers for this pool.
     * */
    private var _browser: Browser? = null

    /**
     * The consistent stateful driver
     * */
    private val statefulDriverPool = ConcurrentStatefulDriverPool(driverPoolManager.browserManager, capacity)

    /**
     * Standby drivers and working drivers
     * */
    private val activeDrivers: Collection<WebDriver> get() = statefulDriverPool.standbyDrivers + statefulDriverPool.workingDrivers

    val meterClosed = registry.meter(this, "closed")
    val meterOffer = registry.meter(this, "offer")

    /**
     * Retired but not closed yet.
     * */
    private var isRetired = false
    val isActive get() = !isRetired && AppContext.isActive
    private val launchEventsEmitted = AtomicBoolean()
    private val _numCreatedDrivers = AtomicInteger()
    private val _numWaitingTasks = AtomicInteger()

    /**
     * Number of created drivers, should be equal to numStandby + numWorking + numRetired.
     * */
    val numCreated get() = _numCreatedDrivers.get()
    val numWaiting get() = _numWaitingTasks.get()

    /**
     * Number of drivers on standby.
     * */
    val numStandby get() = statefulDriverPool.standbyDrivers.size
    /**
     * Number of all possible working drivers
     * */
    val numAvailable get() = numStandby + numDriverSlots

    /**
     * Number of drivers at work.
     * */
    val numWorking get() = statefulDriverPool.workingDrivers.size

    /**
     * Number of retired drivers.
     * */
    val numRetired get() = statefulDriverPool.retiredDrivers.size

    /**
     * Number of closed drivers.
     * */
    val numClosed get() = statefulDriverPool.closedDrivers.size

    /**
     * Number of active drivers.
     * */
    val numActive get() = numWorking + numStandby

    /**
     * Number of available slots to allocate new drivers
     * */
    val numDriverSlots get() = capacity - numActive

    var lastActiveTime: Instant = Instant.now()
        private set
    val idleTimeout get() = immutableConfig.getDuration(BROWSER_DRIVER_POOL_IDLE_TIMEOUT, Duration.ofMinutes(20))
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())

    /**
     * Check if the pool is idle.
     *
     * TODO: why numWorking == 0 is needed?
     * */
    val isIdle get() = (numWorking == 0 && idleTime > idleTimeout)

    class Snapshot(
        val numActive: Int,
        val numStandby: Int,
        val numWaiting: Int,
        val numWorking: Int,
        val numDriverSlots: Int,
        val numRetired: Int,
        val numClosed: Int,
        val isRetired: Boolean,
        val isIdle: Boolean,
        val idleTime: Duration,
        val lackOfResources: Boolean = AppSystemInfo.isCriticalResources
    ) {
        fun format(verbose: Boolean): String {
            val status = if (verbose) {
                String.format(
                    "active: %d, standby: %d, waiting: %d, working: %d, slots: %d, retired: %d, closed: %d",
                    numActive, numStandby, numWaiting, numWorking, numDriverSlots, numRetired, numClosed
                )
            } else {
                String.format(
                    "%d/%d/%d/%d/%d/%d/%d (active/standby/waiting/working/slots/retired/closed)",
                    numActive, numStandby, numWaiting, numWorking, numDriverSlots, numRetired, numClosed
                )
            }

            val time = idleTime.readable()
            return when {
                lackOfResources -> "[Lack of resource] | $status"
                isIdle -> "[Idle] $time | $status"
                isRetired -> "[Retired] | $status"
                else -> status
            }
        }

        override fun toString() = format(false)
    }

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
        return poll(priority, conf, timeout.seconds, TimeUnit.SECONDS)
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    fun poll(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver {
        val driver = pollWebDriver(priority, conf, timeout, unit)
        if (driver == null) {
            val snapshot = takeSnapshot()
            val message = String.format("Driver pool is exhausted | %s", snapshot.format(true))
            logger.warn(message)
            throw WebDriverPoolExhaustedException("Driver pool is exhausted ($snapshot)")
        }

        return driver
    }

    /**
     * Poll a [WebDriver]. If it's the browser is not launched yet, launch it and emit launch events
     * */
    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    suspend fun poll(priority: Int, conf: VolatileConfig, event: BrowseEvent?, page: WebPage): WebDriver {
        val timeout = driverFactory.driverSettings.pollingDriverTimeout

        // NOTE: concurrency note - if multiple threads come to the code snippet,
        // only one goes to pollWithEvents, others wait in poll
        val notEmitted = launchEventsEmitted.compareAndSet(false, true)
        return if (notEmitted) {
            pollWithEvents(priority, conf, event, page, timeout)
        } else {
            poll(priority, conf, timeout)
        }
    }

    fun put(driver: WebDriver) {
        lastActiveTime = Instant.now()
        offerOrDismiss(driver)
    }

    private fun offerOrDismiss(driver: WebDriver) {
        if (driver.isWorking) {
            statefulDriverPool.offer(driver)
            meterOffer.mark()
        } else {
            statefulDriverPool.close(driver)
            meterClosed.mark()
        }
    }

    /**
     * Force the page stop all navigations.
     * Mark the driver pool be retired, but not closed yet.
     * */
    fun retire() {
        isRetired = true
        statefulDriverPool.retire()
    }

    fun close() {
        statefulDriverPool.close()
    }

    fun forEach(action: (WebDriver) -> Unit) = activeDrivers.forEach(action)

    fun firstOrNull(predicate: (WebDriver) -> Boolean) = activeDrivers.firstOrNull(predicate)

    fun cancel(url: String): WebDriver? {
        return activeDrivers.lastOrNull { it.navigateEntry.pageUrl == url }?.also {
            it.cancel()
        }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages, the execution will throw a CancellationException.
     *
     * */
    fun cancelAll() {
        // mark each driver be canceled, so the execution will throw a CancellationException
        statefulDriverPool.cancelAll()
    }

    override fun toString(): String = takeSnapshot().format(false)

    /**
     * Take an imprecise snapshot
     * */
    fun takeSnapshot(): Snapshot {
        return Snapshot(
            numActive,
            numStandby,
            numWaiting,
            numWorking,
            numDriverSlots,
            numRetired,
            numClosed,
            isRetired,
            isIdle,
            idleTime,
        )
    }

    @Throws(BrowserLaunchException::class, WebDriverPoolExhaustedException::class)
    private suspend fun pollWithEvents(
        priority: Int, conf: VolatileConfig, event: BrowseEvent?, page: WebPage, timeout: Duration
    ): WebDriver {
        // TODO: is it better to handle launch events in browser?
        dispatchEvent("onWillLaunchBrowser") { event?.onWillLaunchBrowser?.invoke(page) }

        return poll(priority, conf, timeout).also { driver ->
            dispatchEvent("onBrowserLaunched") { event?.onBrowserLaunched?.invoke(page, driver) }
        }
    }

    @Throws(BrowserLaunchException::class)
    private fun pollWebDriver(priority: Int, conf: VolatileConfig, timeout: Long, unit: TimeUnit): WebDriver? {
        _numWaitingTasks.incrementAndGet()

        val driver = try {
            resourceSafeCreateDriverIfNecessary(priority, conf)
            statefulDriverPool.poll(timeout, unit)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } finally {
            _numWaitingTasks.decrementAndGet()
            lastActiveTime = Instant.now()
        }

        return driver
    }

    /**
     * Create a driver if necessary.
     * Web drivers are open in sequence, so memory and CPU usage will not skyrocket.
     * */
    @Throws(BrowserLaunchException::class)
    private fun resourceSafeCreateDriverIfNecessary(priority: Int, conf: VolatileConfig) {
        synchronized(driverFactory) {
            if (!shouldCreateWebDriver()) {
                return
            }

            computeBrowserAndDriver(priority, conf)
        }
    }

    @Throws(BrowserLaunchException::class)
    private fun computeBrowserAndDriver(priority: Int, conf: VolatileConfig) {
        try {
            computeBrowserAndDriver0(conf)
        } catch (e: BrowserLaunchException) {
            logger.debug("[Unexpected]", e)

            if (isActive) {
                throw e
            }
        }
    }

    /**
     * Check if we should create a new web driver.
     * */
    private fun shouldCreateWebDriver(): Boolean {
        // Using the count of non-quit drivers can better match the memory consumption,
        // but it's easy to wrongly count the quit drivers, a tiny bug can lead to a big mistake.
        // We leave a debug log here for diagnosis purpose.
        val activeDriversInBrowser = _browser?.drivers?.values?.count { !it.isQuit } ?: 0
        // Number of active drivers in this driver pool
        val activeDriversInPool = statefulDriverPool.workingDrivers.size + statefulDriverPool.standbyDrivers.size
        if (activeDriversInBrowser != activeDriversInPool) {
            logger.warn("Inconsistent online driver status: {}/{}/{} (slots/activeP/activeB)",
                numDriverSlots, activeDriversInPool, activeDriversInBrowser)
        }

        val isCriticalResources = AppSystemInfo.isCriticalResources
        if (activeDriversInPool >= capacity) {
            // should also: numDriverSlots > 0
            logger.debug("Enough online drivers: {}/{}/{} (slots/activeP/activeB), will not create new one",
                numDriverSlots, activeDriversInPool, activeDriversInBrowser)
        } else if (AppSystemInfo.isCriticalMemory) {
            logger.info("Critical memory: {}, {}/{}/{} (slots/activeP/activeB), will not create new driver",
                AppSystemInfo.formatAvailableMemory(),
                numDriverSlots, activeDriversInPool, activeDriversInBrowser
            )
        }

        return isActive && !isCriticalResources && activeDriversInPool < capacity
    }

    @Throws(BrowserLaunchException::class)
    private fun computeBrowserAndDriver0(conf: VolatileConfig) {
        val browser = driverFactory.launchBrowser(browserId, conf)
        val driver = browser.newDriver()

        _browser = browser
        _numCreatedDrivers.incrementAndGet()
        statefulDriverPool.offer(driver)

        if (logger.isDebugEnabled) {
            logDriverOnline(driver)
        }
    }

    private suspend fun dispatchEvent(name: String, action: suspend () -> Unit) {
        if (!isActive) {
            return
        }

        try {
            action()
        } catch (e: WebDriverCancellationException) {
            logger.info("Web driver is cancelled")
        } catch (e: WebDriverException) {
            logger.warn(e.brief("[Ignored][$name] "))
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }
    }

    private fun logDriverOnline(driver: WebDriver) {
        val driverSettings = driverFactory.driverSettings
        logger.trace("The {}th web driver is active, browser: {} pageLoadStrategy: {} capacity: {}",
            numActive, driver.name,
            driverSettings.pageLoadStrategy, capacity)
    }
}
