package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.proxy.ProxyManager
import org.apache.commons.io.FileUtils
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverPool(
        val driverControl: WebDriverControl,
        val proxyPool: ProxyPool,
        val proxyManager: ProxyManager,
        val conf: ImmutableConfig
): Parameterized, Freezable(), AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverPool::class.java)

    companion object {
        private val onlineDrivers = Collections.synchronizedSet(HashSet<ManagedWebDriver>())
        private val freeDrivers: MutableMap<Int, ConcurrentLinkedQueue<ManagedWebDriver>> = ConcurrentHashMap()
        private val workingDrivers: MutableMap<Int, ManagedWebDriver> = ConcurrentHashMap()

        private val lock = ReentrantLock() // lock for containers
        private val notEmpty = lock.newCondition()
        private val notBusy = lock.newCondition()
        private val pollingTimeout = Duration.ofMillis(100)

        val startTime = Instant.now()
        val numCrashed = AtomicInteger()
        val numRetired = AtomicInteger()
        val numQuit = AtomicInteger()
        val numReset = AtomicInteger()
        val pageViews = AtomicInteger()
        val elapsedTime get() = Duration.between(startTime, Instant.now())
        val speed get() = 1.0 * pageViews.get() / elapsedTime.seconds
    }

    private val isHeadless = conf.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val concurrency = conf.getInt(FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)
    private var lastActiveTime = Instant.now()
    private var idleTimeout = Duration.ofMinutes(5)

    val capacity = conf.getInt(BROWSER_MAX_DRIVERS, concurrency)
    val isClosed get() = closed.get()
    val isIdle get() = nWorking == 0 && idleTime > idleTimeout
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    val isAllEmpty get() = lock.withLock { onlineDrivers.isEmpty() && freeDrivers.isEmpty() && workingDrivers.isEmpty() }
    val nWorking get() = workingDrivers.size
    val nFree get() = freeDrivers.values.sumBy { it.size }
    val nActive get() = nWorking + nFree
    val nOnline get() = onlineDrivers.size

    /**
     * Allocate [n] drivers with priority [priority]
     * */
    fun allocate(priority: Int, n: Int, conf: ImmutableConfig) {
        whenUnfrozen {
            repeat(n) { poll(priority, conf)?.let { put(it) } }
        }
    }

    /**
     * Run an action in this pool
     * */
    fun <R> run(priority: Int, volatileConfig: VolatileConfig, action: (driver: ManagedWebDriver) -> R): R {
        return whenUnfrozen {
            val driver = poll(priority, volatileConfig)?: throw WebDriverPoolExhaust(formatStatus(verbose = true))
            try {
                action(driver)
            } finally {
                put(driver)
            }
        }
    }

    /**
     * Cancel the fetch task specified by [url] remotely
     * */
    fun cancel(url: String): ManagedWebDriver? {
        log.debug("About to cancel task - 1 | {} {} | {}", numFreezers, numTasks, url)
        return freeze {
            log.debug("About to cancel task - 2 | {} {} | {}", numFreezers, numTasks, url)
            cancelInternal(url).also { log.debug("Task is cancel - 3 | {} {} | {}", numFreezers, numTasks, url) }
        }
    }

    /**
     * Cancel all the fetch tasks remotely
     * */
    fun cancelAll() {
        freeze {
            cancelAllInternal()
        }
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun reset() {
        freeze {
            numReset.incrementAndGet()
            cancelAllInternal()
            closeAllInternal(incognito = true)
        }
    }

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        return whenUnfrozen {
            pollInternal(priority, conf)
        }
    }

    fun put(driver: ManagedWebDriver) {
        whenUnfrozen {
            if (driver.isRetired) {
                retire(driver, null)
            } else {
                offer(driver)
            }
        }
    }

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        freeze {
            closeAllInternal(incognito, processExit)
        }
    }

    fun report() {
        log.info(formatStatus(verbose = true))

        val sb = StringBuilder()

        lock.withLock {
            onlineDrivers.forEach { driver ->
                driver.driver.manage().cookies.joinTo(sb, "Cookies in driver #${driver.id}: ") { it.toString() }
            }
        }

        if (sb.isNotBlank()) {
            log.info("Cookies: \n{}", sb)
        } else {
            log.info("All drivers have no cookie")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAllInternal(incognito = true, processExit = true)
        }
    }

    override fun toString(): String {
        return formatStatus(true)
    }

    private fun pollInternal(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        var driver: ManagedWebDriver? = null
        var exception: Exception? = null

        try {
            lock.lockInterruptibly()

            driver = getOrCreate(priority, conf)
            var nanos = pollingTimeout.toNanos()
            while (!isClosed && driver == null && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos)
                driver = getOrCreate(priority, conf)
            }

            if (driver != null) {
                driver.startWork()
                workingDrivers[driver.id] = driver
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.info("Interrupted, no web driver should return")
            exception = e
        } catch (e: Exception) {
            log.warn("Unexpected error - {}", StringUtil.simplifyException(e))
            exception = e
        } finally {
            lock.unlock()
        }

        if (exception != null && driver != null) {
            retire(driver, exception)
            driver = null
        }

        return if (isClosed) null else driver
    }

    private fun cancelInternal(url: String): ManagedWebDriver? {
        lock.withLock {
            return workingDrivers.values.firstOrNull { it.url == url }?.also { it.cancel() }
        }
    }

    private fun cancelAllInternal() {
        lock.withLock {
            workingDrivers.values.forEach { it.cancel() }
        }
    }

    /**
     * TODO: conf is not really used if the queue is not empty
     * */
    private fun getOrCreate(group: Int, conf: ImmutableConfig): ManagedWebDriver? {
        val queue = freeDrivers.computeIfAbsent(group) { ConcurrentLinkedQueue() }

        if (queue.isEmpty()) {
            allocateTo(group, queue, conf)
        }

        return if (queue.isEmpty()) null else queue.remove()
    }

    private fun allocateTo(group: Int, queue: Queue<ManagedWebDriver>, conf: ImmutableConfig): ManagedWebDriver? {
        val driver = allocate(group, conf)
        if (driver != null) {
            queue.add(driver)
            onlineDrivers.add(driver)
            logDriverOnline(driver)
        }

        return driver
    }

    private fun allocate(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        if (nActive >= capacity) {
            log.warn("Too many web drivers. Cpu cores: {}, capacity: {}, {}",
                    AppConstants.NCPU, capacity, formatStatus(verbose = false))
            return null
        }

        try {
            val factory = WebDriverFactory(driverControl, proxyPool, proxyManager, conf)
            return factory.create(priority, conf)
        } catch (e: Throwable) {
            log.error("Unexpected exception, failed to create a web driver", e)
        }

        return null
    }

    private fun logDriverOnline(driver: ManagedWebDriver) {
        if (log.isTraceEnabled) {
            log.trace("The {}th web driver is online, " +
                    "browser: {} imagesEnabled: {} pageLoadStrategy: {} capacity: {}",
                    nOnline, driver.name,
                    driverControl.imagesEnabled, driverControl.pageLoadStrategy, capacity)
        }
    }

    private fun offer(driver: ManagedWebDriver) {
        lastActiveTime = Instant.now()

        // a driver is always hold by a single thread, so it's OK to use it without locks
        driver.stat.pageViews++
        pageViews.incrementAndGet()

        lock.withLock {
            workingDrivers.remove(driver.id)
            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }

            driver.status.set(DriverStatus.FREE)

            // it can be retired by close all
            val queue = freeDrivers[driver.priority]
            if(queue == null) {
                log.error("Unexpected driver, no free queue for priority {} | {}", driver.priority, driver)
            } else {
                queue.add(driver)
                // TODO: every queue should have a separate signal
                notEmpty.signal()
            }
        }
    }

    private fun retire(driver: ManagedWebDriver, e: Exception?) {
        return retire(driver, e, true)
    }

    private fun retire(driver: ManagedWebDriver, e: Exception?, external: Boolean = true) {
        if (external && isClosed) {
            return
        }

        checkState(driver)

        lock.withLock {
            driver.retire()

            freeDrivers[driver.priority]?.remove(driver)
            workingDrivers.remove(driver.id)

            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }
        }

        when (e) {
            is org.openqa.selenium.NoSuchSessionException -> driver.status.set(DriverStatus.CRASHED)
            is org.apache.http.conn.HttpHostConnectException -> driver.status.set(DriverStatus.CRASHED)
        }

        when {
            driver.isRetired -> numRetired.incrementAndGet()
            driver.isCrashed -> numCrashed.incrementAndGet()
            else -> {}
        }

        try {
            if (e != null) {
                log.info("Quiting {} driver {} - {}", driver.status.get().name.toLowerCase(), driver, StringUtil.simplifyException(e))
            } else {
                log.debug("Quiting {} driver {}", driver.status.get().name.toLowerCase(), driver)
            }
            // Quits this driver, close every associated window
            driver.quit()
            numQuit.incrementAndGet()
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.info("WebDriver is already quit {} - {}", driver, StringUtil.simplifyException(e))
        } catch (e: WebDriverException) {
            log.warn("Quit WebDriver {} - {}", driver, StringUtil.simplifyException(e))
        } catch (e: Throwable) {
            log.error("Unknown error - {}", StringUtil.stringifyException(e))
        } finally {
        }
    }

    private fun closeAllInternal(incognito: Boolean = true, processExit: Boolean = false) {
        lock.withLock {
            var i = 0
            while (!isClosed && nWorking > 0 && i++ < 120) {
                notBusy.await(1, TimeUnit.SECONDS)
                if (i >= 30 && i % 30 == 0) {
                    log.warn("Waited {}s for driver pool to be idle", i)
                }
            }
        }

        closeAllDrivers()

        if (incognito) {
            // Force delete all browser data
            // TODO: delete data that might leak privacy only, cookies, sessions, local storage, etc
            FileUtils.deleteDirectory(AppPaths.BROWSER_TMP_DIR.toFile())
        }
    }

    private fun closeAllDrivers() {
        if (onlineDrivers.isEmpty()) {
            if (nActive != 0) {
                log.info("Illegal status - {}", formatStatus(verbose = true))
            }

            return
        }

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        log.info("Closing all web drivers ... {}", formatStatus(verbose = true))

        freeDrivers.flatMap { it.value }.stream().parallel().forEach { retire(it, null, external = false) }
        freeDrivers.forEach { it.value.clear() }
        freeDrivers.clear()

        workingDrivers.map { it.value }.stream().parallel().forEach { retire(it, null, external = false) }
        workingDrivers.clear()

        onlineDrivers.stream().parallel().forEach { it.quit() }
        onlineDrivers.clear()

        log.info("Total $numQuit drivers are quit | {}", formatStatus(true))
    }

    private fun checkState(driver: ManagedWebDriver) {
        if (driver.isQuit) {
            if (freeDrivers[driver.priority]?.contains(driver) == true) {
                log.warn("Driver is quit, should not be in free driver list | {}", driver)
            }
            if (workingDrivers.containsKey(driver.id)) {
                log.warn("Driver is quit, should not be in working driver list | {}", driver)
            }
        }
    }

    private fun formatStatus(verbose: Boolean = false): String {
        return if (verbose) {
            String.format("total: %d free: %d working: %d online: %d" +
                    " crashed: %d retired: %d quit: %d reset: %d" +
                    " pageViews: %d speed: elapsed: %s speed: %.2f page/s",
                    nOnline, nFree, nWorking, nActive,
                    numCrashed.get(), numRetired.get(), numQuit.get(), numReset.get(),
                    pageViews.get(), DateTimeUtil.readableDuration(elapsedTime), speed
            )
        } else {
            String.format("%d/%d/%d/%d/%d/%d (free/working/active/online/crashed/retired)",
                    nFree, nWorking, nActive, nOnline, numCrashed.get(), numRetired.get())
        }
    }
}
