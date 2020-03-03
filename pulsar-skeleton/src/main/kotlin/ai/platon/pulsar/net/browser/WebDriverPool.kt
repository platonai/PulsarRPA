package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import org.apache.commons.io.FileUtils
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
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
class WebDriverPool(
        val driverFactory: WebDriverFactory,
        val conf: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverPool::class.java)

    val onlineDrivers = ConcurrentSkipListSet<ManagedWebDriver>()
    val freeDrivers = ConcurrentHashMap<Int, ConcurrentLinkedQueue<ManagedWebDriver>>()
    val workingDrivers = ConcurrentHashMap<Int, ManagedWebDriver>()

    private val lock = ReentrantLock() // lock for containers
    private val notEmpty = lock.newCondition()
    private val notBusy = lock.newCondition()
    private val pollingTimeout = Duration.ofMillis(100)

    private val isHeadless = conf.getBoolean(BROWSER_DRIVER_HEADLESS, true)
    private val closed = AtomicBoolean()
    private val concurrency = conf.getInt(FETCH_CONCURRENCY, AppConstants.FETCH_THREADS)

    val capacity = conf.getInt(BROWSER_MAX_DRIVERS, concurrency)
    val isClosed get() = closed.get()
    val isAllEmpty get() = lock.withLock { onlineDrivers.isEmpty() && freeDrivers.isEmpty() && workingDrivers.isEmpty() }

    val numCrashed = AtomicInteger()
    val numRetired = AtomicInteger()
    val numQuit = AtomicInteger()
    val numWorking get() = workingDrivers.size
    val numFree get() = freeDrivers.values.sumBy { it.size }
    val numActive get() = numWorking + numFree
    val numOnline get() = onlineDrivers.size

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        return pollInternal(priority, conf)
    }

    fun put(driver: ManagedWebDriver) {
        if (driver.isRetired) {
            retire(driver, null)
        } else {
            offer(driver)
        }
    }

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        closeAllInternal(incognito, processExit)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAllInternal(incognito = true, processExit = true)
        }
    }

    override fun toString(): String {
        return String.format("%d/%d/%d/%d/%d/%d (free/working/active/online/crashed/retired)",
                numFree, numWorking, numActive, numOnline, numCrashed.get(), numRetired.get())
    }

    private fun offer(driver: ManagedWebDriver) {
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
            val status = driver.status.get().name.toLowerCase()
            if (e != null) {
                log.info("Quiting {} driver {} - {}", status, driver, StringUtil.simplifyException(e))
            } else {
                log.debug("Quiting {} driver {}", status, driver)
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

    private fun pollInternal(priority: Int, conf: ImmutableConfig): ManagedWebDriver? {
        var driver: ManagedWebDriver? = null
        var exception: Exception? = null

        try {
            driver = pollOrCreate(priority, conf)

            if (driver != null) {
                workingDrivers[driver.id] = driver
            }
        } catch (e: InterruptedException) {
            log.info("Interrupted, no web driver should return")
            exception = e
        } catch (e: Exception) {
            log.warn("Unexpected error - {}", StringUtil.simplifyException(e))
            exception = e
        }

        if (exception != null && driver != null) {
            retire(driver, exception)
            driver = null
        }

        return if (isClosed) null else driver
    }

    /**
     * TODO: conf is not really used if the queue is not empty
     * */
    private fun pollOrCreate(group: Int, conf: ImmutableConfig): ManagedWebDriver? {
        val queue = freeDrivers.computeIfAbsent(group) { ConcurrentLinkedQueue() }

        var nanos = pollingTimeout.toNanos()
        lock.withLock {
            while (!isClosed && queue.isEmpty() && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos)
            }

            if (queue.isEmpty()) {
                allocateTo(group, queue, conf)
            }
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
        if (numActive >= capacity) {
            log.warn("Too many web drivers. Cpu cores: {}, capacity: {}, {}", AppConstants.NCPU, capacity, this)
            return null
        }

        try {
            return driverFactory.create(priority, conf)
        } catch (e: Throwable) {
            log.error("Unexpected exception, failed to create a web driver", e)
        }

        return null
    }

    private fun closeAllInternal(incognito: Boolean = true, processExit: Boolean = false) {
        waitUntilIdle()

        closeAllDrivers()

        if (incognito) {
            // Force delete all browser data
            // TODO: delete data that might leak privacy only, cookies, sessions, local storage, etc
            FileUtils.deleteDirectory(AppPaths.BROWSER_TMP_DIR.toFile())
        }
    }

    private fun closeAllDrivers() {
        if (onlineDrivers.isEmpty()) {
            return
        }

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        freeDrivers.flatMap { it.value }.stream().parallel().forEach { retire(it, null, external = false) }
        freeDrivers.forEach { it.value.clear() }
        freeDrivers.clear()

        workingDrivers.map { it.value }.stream().parallel().forEach { retire(it, null, external = false) }
        workingDrivers.clear()

        onlineDrivers.stream().parallel().forEach { it.quit() }
        onlineDrivers.clear()
    }

    private fun waitUntilIdle() {
        lock.withLock {
            var i = 0
            while (!isClosed && numWorking > 0 && i++ < 120) {
                notBusy.await(1, TimeUnit.SECONDS)
                if (i >= 30 && i % 30 == 0) {
                    log.warn("Waited {}s for driver pool to be idle", i)
                }
            }
        }
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
