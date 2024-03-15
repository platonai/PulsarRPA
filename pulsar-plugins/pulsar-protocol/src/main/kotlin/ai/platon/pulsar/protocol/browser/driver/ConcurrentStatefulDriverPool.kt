package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.warnInterruptible
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.runBlocking
import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class ConcurrentStatefulDriverPool(
    private val browserManager: BrowserManager,
    private val capacity: Int
): AutoCloseable {
    private val logger = getLogger(this)

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
     * Keep closed drivers
     * */
    private val _closedDrivers = ConcurrentLinkedQueue<WebDriver>()

    /**
     * Keep standby drivers
     * */
    val standbyDrivers: Queue<WebDriver> get() = _standbyDrivers
    /**
     * Keep working drivers
     * */
    val workingDrivers: Queue<WebDriver> get() = _workingDrivers
    /**
     * Keep retired drivers
     * */
    val retiredDrivers: Queue<WebDriver> get() = _retiredDrivers
    /**
     * Keep closed drivers
     * */
    val closedDrivers: Queue<WebDriver> get() = _closedDrivers

    @get:Synchronized
    val activeDriverCount get() = workingDrivers.size + standbyDrivers.size
    
    @Throws(InterruptedException::class)
    @Synchronized
    fun poll(timeout: Long, unit: TimeUnit): WebDriver? {
        val driver = _standbyDrivers.poll(timeout, unit)
        if (driver != null) {
            driver.startWork()
            _workingDrivers.add(driver)
        }
        return driver
    }

    @Synchronized
    fun offer(driver: WebDriver) {
        driver.free()
        _workingDrivers.remove(driver)
        _standbyDrivers.offer(driver)
    }

    @Synchronized
    fun close(driver: WebDriver) {
        driver.retire()
        _standbyDrivers.remove(driver)
        _workingDrivers.remove(driver)
        _retiredDrivers.remove(driver)
        _closedDrivers.add(driver)

        runCatching { browserManager.closeDriver(driver) }.onFailure { warnInterruptible(this, it) }
        
        // require(driver.isQuit)
    }

    @Synchronized
    fun retire() {
        val drivers = _standbyDrivers + _workingDrivers
        _standbyDrivers.clear()
        _workingDrivers.clear()
        _retiredDrivers.addAll(drivers)

        drivers.forEach { driver ->
            // cancel the driver so the fetch task return immediately
            driver.cancel()
            // retire the driver, so it should not be used to fetch pages anymore
            driver.retire()
            // stop the driver so no more resource it uses
            kotlin.runCatching { runBlocking { driver.stop() } }
        }
    }

    @Synchronized
    fun cancelAll() {
        _workingDrivers.forEach { it.cancel() }
    }

    override fun close() {
        // do not clear, keep the state
//        standbyDrivers.clear()
//        workingDrivers.clear()
//        retiredDrivers.clear()
//        closedDrivers.clear()
    }
}
