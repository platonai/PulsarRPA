package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class WebDriverAdapter(
    val driver: WebDriver,
    val priority: Int = 1000,
) : AbstractWebDriver(driver.browser) {

    private val driverOrNull get() = driver.takeIf { isWorking }

    override var idleTimeout: Duration
        get() = driver.idleTimeout
        set(value) { driver.idleTimeout = value }

    override var waitForTimeout: Duration
        get() = driver.waitForTimeout
        set(value) { driver.waitForTimeout = value }

    override val status get() = driver.status

    override var navigateEntry: NavigateEntry
        get() = driver.navigateEntry
        set(value) { driver.navigateEntry = value }

    override val navigateHistory: MutableList<NavigateEntry> get() = driver.navigateHistory

    /**
     * The browser type
     * */
    override val browserType get() = driver.browserType

    override val supportJavascript get() = driver.supportJavascript

    override val isMockedPageSource get() = driver.isMockedPageSource

    /**
     * The id of the session to the browser
     * */
    override val sessionId get() = driver.sessionId

    @Throws(WebDriverException::class)
    override suspend fun addInitScript(script: String) = driverOrNull?.addInitScript(script) ?: Unit

    override suspend fun addBlockedURLs(urls: List<String>) = driverOrNull?.addBlockingUrls(urls) ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(entry: NavigateEntry) = driverOrNull?.navigateTo(entry) ?: Unit

    /**
     * The actual url return by the browser
     * */
    @Throws(WebDriverException::class)
    override suspend fun currentUrl() = driver.currentUrl()

    /**
     * The real time page source return by the browser
     * */
    @Throws(WebDriverException::class)
    override suspend fun pageSource() = driver.pageSource()

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String) = driverOrNull?.waitForSelector(selector) ?: 0

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeoutMillis: Long) = driverOrNull?.waitForSelector(selector, timeoutMillis) ?: 0

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeout: Duration) = driverOrNull?.waitForSelector(selector, timeout) ?: 0

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation() = driverOrNull?.waitForNavigation() ?: 0

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(timeoutMillis: Long) = driverOrNull?.waitForNavigation(timeoutMillis) ?: 0

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(timeout: Duration) = driverOrNull?.waitForNavigation(timeout) ?: 0

    @Throws(WebDriverException::class)
    override suspend fun exists(selector: String) = driverOrNull?.exists(selector) ?: false

    @Throws(WebDriverException::class)
    override suspend fun visible(selector: String) = driverOrNull?.visible(selector) ?: false

    @Throws(WebDriverException::class)
    override suspend fun click(selector: String, count: Int) = driverOrNull?.click(selector, count) ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun clickMatches(selector: String, pattern: String, count: Int) {
        driverOrNull?.clickMatches(selector, pattern, count)
    }

    @Throws(WebDriverException::class)
    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        driverOrNull?.clickMatches(selector, attrName, pattern, count)
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollTo(selector: String) = driverOrNull?.scrollTo(selector) ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun type(selector: String, text: String) = driverOrNull?.type(selector, text) ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        driverOrNull?.mouseWheelDown(count, deltaX, deltaY, delayMillis)
    }

    @Throws(WebDriverException::class)
    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        driverOrNull?.mouseWheelUp(count, deltaX, deltaY, delayMillis)
    }

    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(x: Double, y: Double) {
        driverOrNull?.moveMouseTo(x, y)
    }

    @Throws(WebDriverException::class)
    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        driverOrNull?.dragAndDrop(selector, deltaX, deltaY)
    }

    @Throws(WebDriverException::class)
    override suspend fun clickablePoint(selector: String) = driverOrNull?.clickablePoint(selector)

    @Throws(WebDriverException::class)
    override suspend fun boundingBox(selector: String) = driverOrNull?.boundingBox(selector)

    @Throws(WebDriverException::class)
    override suspend fun evaluate(expression: String) = driver.evaluate(expression)

    @Throws(WebDriverException::class)
    override suspend fun mainRequestHeaders() = driverOrNull?.mainRequestHeaders() ?: mapOf()

    @Throws(WebDriverException::class)
    override suspend fun mainRequestCookies() = driverOrNull?.mainRequestCookies() ?: listOf()

    @Throws(WebDriverException::class)
    override suspend fun getCookies() = driverOrNull?.getCookies() ?: listOf()

    @Throws(WebDriverException::class)
    override suspend fun bringToFront() {
        driverOrNull?.bringToFront()
    }

    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(selector: String) = driver.captureScreenshot(selector)

    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(rect: RectD) = driver.captureScreenshot(rect)

    @Throws(WebDriverException::class)
    override suspend fun stop() = driverOrNull?.stop() ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun stopLoading() = driverOrNull?.stopLoading() ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun terminate() = driverOrNull?.terminate() ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
        driverOrNull?.setTimeouts(browserSettings)
    }

    @Throws(WebDriverException::class)
    override fun awaitTermination() = driverOrNull?.awaitTermination() ?: Unit

    /**
     * Quits this driver, close every associated window
     * */
    @Throws(Exception::class)
    override fun quit() = driverOrNull?.quit() ?: Unit

    @Throws(Exception::class)
    override fun close() = driverOrNull?.close() ?: Unit
}
