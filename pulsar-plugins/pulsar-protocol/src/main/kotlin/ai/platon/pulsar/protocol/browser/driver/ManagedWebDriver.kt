package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.chrome.ChromeDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

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
        override val browserInstanceId: BrowserInstanceId,
        val driver: org.openqa.selenium.WebDriver,
        val priority: Int = 1000,
        override val proxyEntry: ProxyEntry? = null
): AbstractWebDriver() {
    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(ManagedWebDriver::class.java)
    private val wsRequestTimeout = Duration.ofSeconds(15)

    /**
     * The driver id
     * */
    override val id = instanceSequencer.incrementAndGet()
    /**
     * The driver name
     * */
    override val name = driver.javaClass.simpleName + "-" + id
    /**
     * Driver status
     * */
    val status = AtomicReference<DriverStatus>(DriverStatus.UNKNOWN)

    val pageViews = AtomicInteger()

    val isFree get() = status.get().isFree
    val isWorking get() = status.get().isWorking
    val isNotWorking get() = !isWorking
    val isCrashed get() = status.get().isCrashed
    override val isRetired get() = status.get().isRetired
    override val isCanceled get() = status.get().isCanceled
    override val isQuit get() = status.get().isQuit

    /**
     * The current loading page url
     * The browser might redirect, so it might not be the same with [currentUrl]
     * */
    override var url: String = ""

    /**
     * The actual url return by the browser
     * */
    val currentUrl: String get() = "".takeIf { isQuit }?:driver.runCatching { currentUrl }
            .onFailure { log.warn("Unexpected exception", it) }
            .getOrDefault("")

    /**
     * The real time page source return by the browser
     * */
    override val pageSource: String get() = driver.runCatching { pageSource }.getOrThrow()

    /**
     * The id of the session to the browser
     * */
    override val sessionId: String? get() = when {
        isQuit -> null
        driver is ChromeDevtoolsDriver -> driver.runCatching { sessionId }.getOrNull()?.toString()
        else -> StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }
    }

    /**
     * The browser type
     * */
    override val browserType: BrowserType get() = when (driver) {
        is ChromeDevtoolsDriver -> BrowserType.CHROME
        is ChromeDriver -> BrowserType.SELENIUM_CHROME
        else -> BrowserType.CHROME
    }

    init {
        setLogLevel()
    }

    override fun free() = status.set(DriverStatus.FREE)
    override fun startWork() = status.set(DriverStatus.WORKING)
    override fun retire() = status.set(DriverStatus.RETIRED)
    override fun cancel() {
        if (isCanceled) {
            return
        }

        if (status.compareAndSet(DriverStatus.WORKING, DriverStatus.CANCELED)) {
            log.debug("Canceling driver $this")
            stopLoading()
        }
    }

    /**
     * Navigate to the url
     * The browser might redirect, so it might not be the same to [currentUrl]
     * */
    @Throws(NoSuchSessionException::class)
    override fun navigateTo(url: String) {
        if (isWorking) {
            driver.get(url)
            this.url = url
            pageViews.incrementAndGet()
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun evaluate(expression: String): Any? {
        return when {
            isNotWorking -> null
            driver is ChromeDevtoolsDriver -> driver.evaluate(expression)
            driver is ChromeDriver -> driver.executeScript(expression)
            else -> null
        }
    }

    override fun evaluateSilently(expression: String): Any? = takeIf { isWorking }?.runCatching { evaluate(expression) }

    override fun stopLoading() {
        if (driver is ChromeDevtoolsDriver) {
            driver.takeIf { isWorking }?.runCatching { stopLoading() }
        } else {
            evaluateSilently(";window.stop();")
        }
    }

    override fun setTimeouts(driverConfig: BrowserControl) {
        if (isNotWorking) {
            return
        }

        if (driver is ChromeDriver) {
            val timeouts = driver.manage().timeouts()
            timeouts.pageLoadTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
            timeouts.setScriptTimeout(driverConfig.scriptTimeout.seconds, TimeUnit.SECONDS)
        } else if (driver is ChromeDevtoolsDriver) {
            // not implemented
        }
    }

    /**
     * Quits this driver, close every associated window
     * */
    override fun quit() {
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

    override fun compareTo(other: AbstractWebDriver): Int = id - other.id

    override fun toString(): String = sessionId?.let { "#$id-$sessionId" }?:"#$id(closed)"

    private fun setLogLevel() {
        // Set log level
        if (driver is org.openqa.selenium.remote.RemoteWebDriver) {
            val logger = LoggerFactory.getLogger(org.openqa.selenium.WebDriver::class.java)
            val level = when {
                logger.isDebugEnabled -> Level.FINER
                logger.isTraceEnabled -> Level.ALL
                else -> Level.FINE
            }

            driver.setLogLevel(level)
        }
    }
}
