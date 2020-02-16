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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class DriverStat(
        var pageViews: Int = 0
)

enum class DriverStatus {
    UNKNOWN, FREE, WORKING, PAUSED, RETIRED, CRASHED, QUIT
}

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollInterval: Duration
) {
    constructor(config: ImmutableConfig) : this(
            config.getDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, Duration.ofMinutes(3)),
            // wait page ready using script, so it can not smaller than pageLoadTimeout
            config.getDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(60)),
            config.getInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, 3),
            config.getDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))
    )
}

class ManagedWebDriver(
        val id: Int,
        val driver: WebDriver,
        val priority: Int = 1000
) {
    private val log = LoggerFactory.getLogger(ManagedWebDriver::class.java)

    val stat = DriverStat()
    var status = DriverStatus.UNKNOWN
        @Synchronized get
        @Synchronized set

    val isPaused
        @Synchronized
        get() = status == DriverStatus.PAUSED

    val isRetired
        @Synchronized
        get() = status == DriverStatus.RETIRED

    val isWorking
        @Synchronized
        get() = status == DriverStatus.WORKING

    val isQuit
        @Synchronized
        get() = status == DriverStatus.QUIT

    // The proxy entry ready to use
    val proxyEntry = AtomicReference<ProxyEntry>()

    val incognito = AtomicBoolean()

    val sessionId: String?
        @Synchronized
        get() {
            return if (driver is ChromeDevtoolsDriver) {
                driver.sessionId?.toString()
            } else {
                StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }
            }
        }

    val currentUrl: String
        get() = try {
            driver.currentUrl
        } catch (t: Throwable) {
            ""
        }

    val pageSource: String
        get() = try {
            driver.pageSource
        } catch (t: Throwable) {
            "(exception)"
        }

    val browserType: BrowserType =
            when (driver) {
                is ChromeDevtoolsDriver -> BrowserType.CHROME
                is ChromeDriver -> BrowserType.SELENIUM_CHROME
//            is HtmlUnitDriver -> page.lastBrowser = BrowserType.HTMLUNIT
                else -> {
                    log.warn("Actual browser is set to be NATIVE by selenium engine")
                    BrowserType.NATIVE
                }
            }

    fun navigate(url: String) {
        return driver.get(url)
    }

    fun evaluate(expression: String): Any? {
        return when (driver) {
            is ChromeDriver -> {
                driver.executeScript(expression)
            }
            is ChromeDevtoolsDriver -> {
                driver.evaluate(expression)
            }
            else -> null
        }
    }

    fun evaluateSilently(expression: String): Any? {
        try {
            return evaluate(expression)
        } catch (ignored: Throwable) {}

        return null
    }

    fun scrollDown() {

    }

    fun setTimeouts(driverConfig: DriverConfig) {
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

    @Synchronized
    fun pause() {
        status = DriverStatus.PAUSED
    }

    @Synchronized
    fun retire() {
        status = DriverStatus.RETIRED
    }

    /**
     * Quits this driver, close every associated window
     * */
    @Synchronized
    fun quit() {

        if (isQuit) {
            return
        }

        if (incognito.get()) {
            deleteAllCookiesSilently()
        }

        status = DriverStatus.QUIT
        driver.quit()
    }

    /**
     * Close redundant web drivers and keep only one to release resources
     * TODO: buggy
     * */
    @Synchronized
    fun closeIfNotOnly() {
        if (isQuit) return

        val handles = driver.windowHandles.size
        if (handles > 1) {
            // TODO:
            // driver.close()
        }
    }

    @Synchronized
    fun deleteAllCookiesSilently() {
        if (isQuit) return

        try {
            if (driver is ChromeDevtoolsDriver) {
                val names = driver.getCookieNames().joinToString(", ") { it }
                log.debug("Deleted cookies: $names")

                driver.deleteAllCookies()
            } else if (driver is RemoteWebDriver) {
                val names = driver.manage().cookies.map { it.name }
                names.forEach { name ->
                    driver.manage().deleteCookieNamed(name)
                }

                log.debug("Deleted cookies: $names")

                driver.manage().deleteAllCookies()
            }
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    @Synchronized
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

    @Synchronized
    override fun equals(other: Any?): Boolean {
        return other is ManagedWebDriver && other.id == this.id
    }

    @Synchronized
    override fun hashCode(): Int {
        return id
    }

    @Synchronized
    override fun toString(): String {
        return if (sessionId != null) "#$id-$sessionId" else "#$id(closed)"
    }
}
