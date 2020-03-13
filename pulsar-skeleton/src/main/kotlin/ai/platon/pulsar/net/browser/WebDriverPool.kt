package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import org.apache.commons.io.FileUtils
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import java.time.Duration
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
    val freeDrivers = ConcurrentLinkedQueue<ManagedWebDriver>()
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
    val numFree get() = freeDrivers.size
    val numActive get() = numWorking + numFree
    val numOnline get() = onlineDrivers.size

    fun poll(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        lock.withLock {
            val driver = pollOrCreate(priority, conf)

            workingDrivers[driver.id] = driver
            notEmpty.signal()

            return driver
        }
    }

    fun put(driver: ManagedWebDriver) {
        lock.withLock {
            workingDrivers.remove(driver.id)
            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }

            if (driver.isRetired) {
                retire(driver)
            } else {
                offer(driver)
            }
        }
    }

    fun closeAll(incognito: Boolean = true, processExit: Boolean = false) {
        if (!processExit) {
            waitUntilIdle()
        }

        closeAllDrivers(processExit)

        if (incognito) {
            // Force delete all browser data
            // TODO: delete data that might leak privacy only, cookies, sessions, local storage, etc
            FileUtils.deleteDirectory(AppPaths.BROWSER_TMP_DIR.toFile())
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(incognito = true, processExit = true)
        }
    }

    override fun toString(): String {
        return String.format("%d/%d/%d/%d/%d/%d (free/working/active/online/crashed/retired)",
                numFree, numWorking, numActive, numOnline, numCrashed.get(), numRetired.get())
    }

    private fun offer(driver: ManagedWebDriver) {
        lock.withLock {
            driver.status.set(DriverStatus.FREE)
            // it can be retired by close all
            freeDrivers.add(driver)
            // TODO: every queue should have a separate signal
            notEmpty.signal()
        }
    }

    private fun retire(driver: ManagedWebDriver, external: Boolean = true) {
        if (external && isClosed) {
            return
        }

        lock.withLock {
            driver.retire()
            freeDrivers.remove(driver)
            workingDrivers.remove(driver.id)
            if (workingDrivers.isEmpty()) {
                notBusy.signalAll()
            }
        }

        checkState(driver)

        when {
            driver.isRetired -> numRetired.incrementAndGet()
            driver.isCrashed -> numCrashed.incrementAndGet()
            else -> {}
        }

        try {
            // Quits this driver, close every associated window
            driver.quit()
            numQuit.incrementAndGet()
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.info("WebDriver is already quit {} - {}", driver, Strings.simplifyException(e))
        } catch (e: WebDriverException) {
            log.warn("Quit WebDriver {} - {}", driver, Strings.simplifyException(e))
        } catch (e: Throwable) {
            log.error("Unknown error - {}", Strings.stringifyException(e))
        }
    }

    /**
     * TODO: conf is not really used if the queue is not empty
     * */
    private fun pollOrCreate(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        // wait until not empty
        var nanos = pollingTimeout.toNanos()
        while (!isClosed && freeDrivers.isEmpty() && nanos > 0) {
            nanos = notEmpty.awaitNanos(nanos)
        }

        if (freeDrivers.isEmpty()) {
            val driver = driverFactory.create(priority, conf)
            freeDrivers.add(driver)
            onlineDrivers.add(driver)
            logDriverOnline(driver)
        }

        return freeDrivers.remove()
    }

    private fun closeAllDrivers(processExit: Boolean = false) {
        if (onlineDrivers.isEmpty()) {
            return
        }

        if (!isHeadless) {
            // should close the browsers by hand
            return
        }

        freeDrivers.clear()
        workingDrivers.clear()

        // create a non-synchronized list for quitting all drivers in parallel
        val nonSyncDriverList = onlineDrivers.toList()
        onlineDrivers.clear()

        nonSyncDriverList.parallelStream().forEach {
            log.info("Quit driver $it")
            it.quit()
            numQuit.incrementAndGet()
        }
    }

    private fun waitUntilIdle() {
        lock.withLock {
            var i = 0
            while (!isClosed && numWorking > 0 && i++ < 120 && !Thread.currentThread().isInterrupted) {
                notBusy.await(1, TimeUnit.SECONDS)
                if (i >= 30 && i % 30 == 0) {
                    log.warn("Waited {}s for driver pool to be idle | {}", i, this)
                }
            }
        }
    }

    private fun checkState(driver: ManagedWebDriver) {
        if (driver.isQuit) {
            if (freeDrivers.contains(driver)) {
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
