package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class WebDriverAdapter(
    val driver: WebDriver,
    val priority: Int = 1000,
) : AbstractWebDriver(driver.browser, instanceSequencer.incrementAndGet()) {
    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(WebDriverAdapter::class.java)

    val pageViews = AtomicInteger()

    override val status get() = driver.status

    private val driverOrNull get() = driver.takeIf { isWorking }

    /**
     * The actual url return by the browser
     * */
    override suspend fun currentUrl(): String {
        return if (isQuit) "" else
            kotlin.runCatching { driver.currentUrl() }
                .onFailure { logger.warn("Unexpected exception", it) }
                .getOrElse { "" }
    }

    /**
     * The real time page source return by the browser
     * */
    override suspend fun pageSource(): String? = driver.pageSource()

    /**
     * The id of the session to the browser
     * */
    override val sessionId: String?
        get() = when {
            isQuit -> null
            else -> driver.sessionId
        }

    /**
     * The browser type
     * */
    override val browserType get() = driver.browserType

    override val supportJavascript get() = driver.supportJavascript

    override val isMockedPageSource get() = driver.isMockedPageSource

    /**
     * Navigate to the url
     * The browser might redirect, so it might not be the same to [currentUrl]
     * */
    override suspend fun navigateTo(url: String) = driverOrNull?.navigateTo(url) ?: Unit

    override suspend fun navigateTo(entry: NavigateEntry) = driverOrNull?.navigateTo(entry) ?: Unit

    override suspend fun waitForSelector(selector: String) = driverOrNull?.waitForSelector(selector) ?: 0

    override suspend fun waitForSelector(selector: String, timeoutMillis: Long) = driverOrNull?.waitForSelector(selector, timeoutMillis) ?: 0

    override suspend fun waitForSelector(selector: String, timeout: Duration) = driverOrNull?.waitForSelector(selector, timeout) ?: 0

    override suspend fun waitForNavigation() = driverOrNull?.waitForNavigation() ?: 0

    override suspend fun waitForNavigation(timeoutMillis: Long) = driverOrNull?.waitForNavigation(timeoutMillis) ?: 0

    override suspend fun waitForNavigation(timeout: Duration) = driverOrNull?.waitForNavigation(timeout) ?: 0

    override suspend fun exists(selector: String) = driverOrNull?.exists(selector) ?: false

    override suspend fun visible(selector: String) = driverOrNull?.visible(selector) ?: false

    override suspend fun click(selector: String, count: Int) = driverOrNull?.click(selector, count) ?: Unit

    override suspend fun clickMatches(selector: String, pattern: String, count: Int) {
        driverOrNull?.clickMatches(selector, pattern, count)
    }

    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        driverOrNull?.clickMatches(selector, attrName, pattern, count)
    }

    override suspend fun scrollTo(selector: String) = driverOrNull?.scrollTo(selector) ?: Unit

    override suspend fun type(selector: String, text: String) = driverOrNull?.type(selector, text) ?: Unit

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        driverOrNull?.mouseWheelDown(count, deltaX, deltaY, delayMillis)
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        driverOrNull?.mouseWheelUp(count, deltaX, deltaY, delayMillis)
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        driverOrNull?.moveMouseTo(x, y)
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        driverOrNull?.dragAndDrop(selector, deltaX, deltaY)
    }

    override suspend fun clickablePoint(selector: String) = driverOrNull?.clickablePoint(selector)

    override suspend fun boundingBox(selector: String) = driverOrNull?.boundingBox(selector)

    override suspend fun evaluate(expression: String): Any? {
        return when {
            isNotWorking -> null
            else -> driver.evaluate(expression)
        }
    }

    override suspend fun mainRequestHeaders() = driverOrNull?.mainRequestHeaders() ?: mapOf()

    override suspend fun mainRequestCookies() = driverOrNull?.mainRequestCookies() ?: listOf()

    override suspend fun getCookies() = driverOrNull?.getCookies() ?: listOf()

    override suspend fun bringToFront() {
        driverOrNull?.bringToFront()
    }

    override suspend fun captureScreenshot(selector: String): String? {
        return when (driver) {
            is ChromeDevtoolsDriver -> driver.captureScreenshot(selector)
            else -> null // Not implemented currently
        }
    }

    override suspend fun captureScreenshot(rect: RectD): String? {
        return when (driver) {
            is ChromeDevtoolsDriver -> driver.captureScreenshot(rect)
            else -> null // Not implemented currently
        }
    }

    override suspend fun stop() = driverOrNull?.stop() ?: Unit

    override suspend fun stopLoading() = driverOrNull?.stopLoading() ?: Unit

    override suspend fun terminate() = driverOrNull?.terminate() ?: Unit

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
        driverOrNull?.setTimeouts(browserSettings)
    }

    override fun awaitTermination() {
        driverOrNull?.awaitTermination()
    }

    /**
     * Quits this driver, close every associated window
     * */
    override fun quit() {
        if (!isQuit) {
            synchronized(status) {
                if (!isQuit) {
                    status.set(WebDriver.Status.QUIT)
                    driver.runCatching { quit() }.onFailure { logger.warn("Unexpected exception", it) }
                }
            }
        }
    }

    override fun close() = driver.close()
}
