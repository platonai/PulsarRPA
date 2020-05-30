package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.persist.metadata.BrowserType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import kotlin.system.measureTimeMillis

enum class DriverStatus {
    UNKNOWN, FREE, WORKING, CANCELED, RETIRED, CRASHED, QUIT;

    val isFree get() = this == FREE
    val isWorking get() = this == WORKING
    val isCanceled get() = this == CANCELED
    val isRetired get() = this == RETIRED
    val isCrashed get() = this == CRASHED
    val isQuit get() = this == QUIT
}

class ManagedWebDriver(
        val driver: WebDriver,
        val priority: Int = 1000
): Comparable<ManagedWebDriver> {
    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(ManagedWebDriver::class.java)
    private val wsRequestTimeout = Duration.ofSeconds(15)

    /**
     * The driver id
     * */
    val id = instanceSequencer.incrementAndGet()
    /**
     * The driver name
     * */
    val name = driver.javaClass.simpleName + "-" + id
    /**
     * Driver status
     * */
    val status = AtomicReference<DriverStatus>(DriverStatus.UNKNOWN)

    val isFree get() = status.get().isFree
    val isWorking get() = status.get().isWorking
    val isNotWorking get() = !isWorking
    val isCanceled get() = status.get().isCanceled
    val isRetired get() = status.get().isRetired
    val isCrashed get() = status.get().isCrashed
    val isQuit get() = status.get().isQuit

    /**
     * The current loading page url
     * The browser might redirect, so it might not be the same with [currentUrl]
     * */
    var url: String = ""

    /**
     * The actual url return by the browser
     * */
    val currentUrl: String get() = "".takeIf { isQuit }?:driver.runCatching { currentUrl }
            .onFailure { log.warn("Unexpected exception", it) }
            .getOrDefault("")

    /**
     * The real time page source return by the browser
     * */
    val pageSource: String get() = driver.runCatching { pageSource }.getOrThrow()

    /**
     * The id of the session to the browser
     * */
    val sessionId: String? get() = when {
        isQuit -> null
        driver is ChromeDevtoolsDriver -> driver.runCatching { sessionId }.getOrNull()?.toString()
        else -> StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }
    }

    /**
     * The browser type
     * */
    val browserType: BrowserType get() = when (driver) {
        is ChromeDevtoolsDriver -> BrowserType.CHROME
        is ChromeDriver -> BrowserType.SELENIUM_CHROME
        else -> BrowserType.CHROME
    }

    init {
        setLogLevel()
    }

    fun free() = status.set(DriverStatus.FREE)
    fun startWork() = status.set(DriverStatus.WORKING)
    fun retire() = status.set(DriverStatus.RETIRED)
    fun cancel() {
        if (isCanceled) {
            return
        }

        if (status.compareAndSet(DriverStatus.WORKING, DriverStatus.CANCELED)) {
            log.info("Canceling driver $this")
            runBlocking {
                withTimeout(wsRequestTimeout.toMillis()) { stopLoading() }
            }
        }
    }

    /**
     * Navigate to the url
     * The browser might redirect, so it might not be the same to [currentUrl]
     * */
    @Throws(NoSuchSessionException::class)
    fun navigateTo(url: String) = driver.takeIf { isWorking }?.get(url.also { this.url = it })

    @Throws(NoSuchSessionException::class)
    fun evaluate(expression: String): Any? {
        return when {
            isNotWorking -> null
            driver is ChromeDevtoolsDriver -> driver.evaluate(expression)
            driver is ChromeDriver -> driver.executeScript(expression)
            else -> null
        }
    }

    fun evaluateSilently(expression: String): Any? = takeIf { isWorking }?.runCatching { evaluate(expression) }

    fun stopLoading() {
        if (driver is ChromeDevtoolsDriver) {
            driver.takeIf { isWorking }?.runCatching { stopLoading() }
        } else {
            evaluateSilently(";window.stop();")
        }
    }

    fun setTimeouts(driverConfig: BrowserControl) {
        if (isNotWorking) {
            return
        }

        if (driver is ChromeDriver) {
            val timeouts = driver.manage().timeouts()
            timeouts.pageLoadTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
            timeouts.setScriptTimeout(driverConfig.scriptTimeout.seconds, TimeUnit.SECONDS)
        } else if (driver is ChromeDevtoolsDriver) {
            driver.pageLoadTimeout = driverConfig.pageLoadTimeout
            driver.scriptTimeout = driverConfig.scriptTimeout
            driver.scrollDownCount = driverConfig.scrollDownCount
            driver.scrollInterval = driverConfig.scrollInterval
        }
    }

    /**
     * Quits this driver, close every associated window
     * */
    fun quit() {
        if (!isQuit) {
            synchronized(status) {
                if (!isQuit) {
                    status.set(DriverStatus.QUIT)
                    driver.runCatching { quit() }.onFailure { log.warn("Unexpected exception", it) }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is ManagedWebDriver && other.id == this.id

    override fun hashCode(): Int = id

    override fun compareTo(other: ManagedWebDriver): Int = id - other.id

    override fun toString(): String = sessionId?.let { "#$id-$sessionId" }?:"#$id(closed)"

    private fun setLogLevel() {
        // Set log level
        if (driver is RemoteWebDriver) {
            val logger = LoggerFactory.getLogger(WebDriver::class.java)
            val level = when {
                logger.isDebugEnabled -> Level.FINER
                logger.isTraceEnabled -> Level.ALL
                else -> Level.FINE
            }

            driver.setLogLevel(level)
        }
    }
}
