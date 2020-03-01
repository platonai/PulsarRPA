package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.persist.metadata.BrowserType
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

data class DriverStat(
        var pageViews: Int = 0
)

enum class DriverStatus {
    UNKNOWN, FREE, WORKING, CANCELED, RETIRED, CRASHED, QUIT;

    val isFree get() = this == FREE
    val isWorking get() = this == WORKING
    val isCanceled get() = this == CANCELED
    val isRetired get() = this == RETIRED
    val isCrashed get() = this == CRASHED
    val isQuit get() = this == QUIT
}

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollInterval: Duration
) {
    constructor(conf: ImmutableConfig) : this(
            conf.getDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, Duration.ofMinutes(3)),
            // wait page ready using script, so it can not smaller than pageLoadTimeout
            conf.getDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(90)),
            conf.getInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, 3),
            conf.getDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))
    )
}

class ManagedWebDriver(
        val driver: WebDriver,
        val priority: Int = 1000
): Comparable<ManagedWebDriver> {
    companion object {
        val instanceSequence = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(ManagedWebDriver::class.java)

    /**
     * The driver id
     * */
    val id = instanceSequence.incrementAndGet()

    val name = driver.javaClass.simpleName

    /**
     * Driver statistics
     * */
    val stat = DriverStat()

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

    var incognito = false

    /**
     * The loading page url
     * The browser might redirect, so it might not be the same to [currentUrl]
     * */
    var url: String = ""

    /**
     * The actual url return by the browser
     * */
    val currentUrl: String get() = try {
        if (isQuit) "" else driver.currentUrl
    } catch (t: Throwable) { "" }

    /**
     * The real time page source return by the browser
     * */
    val pageSource: String get() = driver.pageSource

    /**
     * The id of the session to the browser
     * */
    val sessionId: String? get() = when {
        isQuit -> null
        driver is ChromeDevtoolsDriver -> driver.sessionId?.toString()
        else -> StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }
    }

    /**
     * The browser type
     * */
    val browserType: BrowserType get() =
        when (driver) {
            is ChromeDevtoolsDriver -> BrowserType.CHROME
            is ChromeDriver -> BrowserType.SELENIUM_CHROME
            else -> BrowserType.CHROME
        }

    init {
        setLogLevel()
    }

    fun startWork() {
        status.set(DriverStatus.WORKING)
    }

    fun cancel() {
        status.set(DriverStatus.CANCELED)
        stopLoading()
    }

    fun retire() {
        status.set(DriverStatus.RETIRED)
    }

    /**
     * Navigate to the url
     * The browser might redirect, so it might not be the same to [currentUrl]
     * */
    fun navigateTo(url: String) {
        if (isWorking) {
            this.url = url
            driver.get(url)
        }
    }

    fun evaluate(expression: String): Any? {
        return when {
            isNotWorking -> null
            driver is ChromeDevtoolsDriver -> driver.evaluate(expression)
            driver is ChromeDriver -> driver.executeScript(expression)
            else -> null
        }
    }

    fun evaluateSilently(expression: String): Any? {
        if (isWorking) {
            try {
                return evaluate(expression)
            } catch (ignored: Throwable) {}
        }

        return null
    }

    fun stopLoading() {
        if (isNotWorking) {
            return
        }

        try {
            if (driver is ChromeDevtoolsDriver) {
                driver.stopLoading()
            } else {
                evaluateSilently(";window.stop();")
            }
        } catch (e: Throwable) {
            log.info("Failed to stop loading - {}", StringUtil.simplifyException(e))
        }
    }

    fun scrollDown() {
        if (isQuit) {
            return
        }
    }

    fun setTimeouts(driverConfig: DriverConfig) {
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
                    driver.quit()
                }
            }
        }
    }

    /**
     * Close redundant web drivers and keep only one to release resources
     * TODO: buggy
     * */
    fun closeOtherTabs() {
        if (isQuit) return

        val handles = driver.windowHandles.size
        if (handles > 1) {
            // TODO:
            // driver.close()
        }
    }

    fun removeFootprint() {
        deleteAllCookiesSilently()
        // delete local session data
        // detete local storage data
    }

    /**
     * Delete all cookies
     * TODO: delete data directory directly
     * */
    fun deleteAllCookiesSilently() {
        if (isQuit) return

        try {
            if (driver is ChromeDevtoolsDriver) {
                val cookies = driver.getCookieNames()
                if (cookies.isNotEmpty()) {
                    val names = cookies.joinToString(", ") { it }
                    log.debug("Deleted cookies: $names")
                }

                // delete all cookies, this can be ignored
                driver.deleteAllCookies()
            } else if (driver is RemoteWebDriver) {
                val names = driver.manage().cookies.map { it.name }
                if (names.isNotEmpty()) {
                    names.forEach { name ->
                        driver.manage().deleteCookieNamed(name)
                    }

                    log.debug("Deleted cookies: $names")
                }

                // delete all cookies, this can be ignored
                driver.manage().deleteAllCookies()
            }
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    fun deleteAllCookiesSilently(targetUrl: String) {
        if (isQuit) return

        try {
            when (driver) {
                is RemoteWebDriver -> {
                    driver.get(targetUrl)
                    driver.manage().deleteAllCookies()
                }
                is ChromeDevtoolsDriver -> {
                    driver.get(targetUrl)
                    driver.deleteAllCookies()
                }
                else -> {

                }
            }
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

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

    override fun equals(other: Any?): Boolean {
        return other is ManagedWebDriver && other.id == this.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun compareTo(other: ManagedWebDriver): Int {
        return id - other.id
    }

    override fun toString(): String {
        return if (sessionId != null) "#$id-$sessionId" else "#$id(closed)"
    }
}
