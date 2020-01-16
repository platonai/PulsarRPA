package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.persist.metadata.BrowserType
import org.apache.commons.lang.IllegalClassException
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class DriverStat(
        var pageViews: Int = 0
)

enum class DriverStatus {
    UNKNOWN, FREE, WORKING, PAUSED, RETIRED, CRASHED, QUIT
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

    val sessionId
        @Synchronized
        get() = StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }

    val currentUrlOrException: String
        get() = driver.currentUrl

    val currentUrl: String
        get() = try { driver.currentUrl } catch (t: Throwable) { "" }

    val pageSourceOrException: String
        get() = driver.pageSource

    val pageSource: String
        get() = try { driver.pageSource } catch (t: Throwable) { "(exception)" }

    val browserType: BrowserType =
            when (driver) {
                is ChromeDriver -> BrowserType.CHROME
//            is HtmlUnitDriver -> page.lastBrowser = BrowserType.HTMLUNIT
                else -> {
                    log.warn("Actual browser is set to be NATIVE by selenium engine")
                    BrowserType.NATIVE
                }
            }

    fun get(url: String) {
        return driver.get(url)
    }

    fun executeScript(script: String, vararg args: Any?): Any? {
        val jsExecutor = driver as? JavascriptExecutor
                ?: throw IllegalClassException("Web driver should be a JavascriptExecutor")

        return jsExecutor.executeScript(script, args)
    }

    fun executeScriptSilently(script: String, vararg args: Any?): Any? {
        try {
            return executeScript(script, args)
        } catch (ignored: Throwable) {}

        return null
    }

    @Synchronized
    fun pause() {
        status = DriverStatus.PAUSED
    }

    @Synchronized
    fun retire() {
        status = DriverStatus.RETIRED
    }

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

    @Synchronized
    fun closeRedundantTabs() {
        val handles = driver.windowHandles.size
        if (handles > 1) {
            driver.close()
        }
    }

    @Synchronized
    fun deleteAllCookiesSilently() {
        try {
            val names = driver.manage().cookies.map { it.name }
            names.forEach { name ->
                driver.manage().deleteCookieNamed(name)
            }

            log.debug("Deleted cookies: $names")

            driver.manage().deleteAllCookies()
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    @Synchronized
    fun deleteAllCookiesSilently(targetUrl: String) {
        try {
            driver.get(targetUrl)
            driver.manage().deleteAllCookies()
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

//    @Synchronized
//    fun deleteCookieNamedSilently(name: String) {
//        try {
//            driver.manage().deleteCookieNamed(name)
//        } catch (e: Throwable) {
//            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
//        }
//    }

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
