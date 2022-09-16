package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import com.microsoft.playwright.Page
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CountDownLatch

class PlaywrightDriver(
    private val settings: BrowserSettings,
    browser: PlaywrightBrowser,
) : AbstractWebDriver(browser) {

    private val logger = LoggerFactory.getLogger(PlaywrightDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PLAYWRIGHT_CHROME

    private val _browser = browser as PlaywrightBrowser
    private val actualBrowser get() = _browser.actualBrowser
    private val page = actualBrowser.newPage()
    private val closeCountDown = CountDownLatch(1)

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {

    }

    override suspend fun addInitScript(script: String) {
        page.addInitScript(script)
    }

    override suspend fun addBlockedURLs(urls: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun navigateTo(entry: NavigateEntry) {
        page.onClose {
            closeCountDown.countDown()
        }

        page.onResponse { response ->
            response.allHeaders()
            response.request().headers()
        }

        page.navigate(entry.url)
    }

    override suspend fun currentUrl(): String {
        return page.url()
    }

    override suspend fun pageSource(): String? {
        return page.content()
    }

    override suspend fun mainRequestHeaders(): Map<String, Any> {
        return mapOf()
    }

    override suspend fun mainRequestCookies(): List<Map<String, String>> {
        return listOf()
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        return listOf()
    }

    override suspend fun evaluate(expression: String): Any? {
        return page.evaluate(expression)
    }

    override suspend fun exists(selector: String): Boolean {
        return page.locator(selector).first() != null
    }

    override suspend fun visible(selector: String): Boolean {
        return page.isVisible(selector)
    }

    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        val options = Page.WaitForSelectorOptions().setTimeout(timeout.toMillis().toDouble())
        page.waitForSelector(selector, options)
        return 0
    }

    override suspend fun waitForNavigation(timeout: Duration): Long {
        val options = Page.WaitForNavigationOptions().setTimeout(timeout.toMillis().toDouble())
        page.waitForNavigation(options) {

        }
        return 0
    }

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        page.mouse().wheel(deltaX, deltaY)
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        page.mouse().wheel(-deltaX, -deltaY)
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        page.mouse().move(x, y)
    }

    override suspend fun click(selector: String, count: Int) {
        page.click(selector)
    }

    override suspend fun type(selector: String, text: String) {
        page.type(selector, text)
    }

    override suspend fun scrollTo(selector: String) {
        page.locator(selector).scrollIntoViewIfNeeded()
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun clickablePoint(selector: String): PointD? {
        // page.locator(selector).boundingBox()
        TODO("Not yet implemented")
    }

    override suspend fun boundingBox(selector: String): RectD? {
        val box = page.locator(selector).boundingBox() ?: return null
        return RectD(box.x, box.y, box.width, box.height)
    }

    override suspend fun captureScreenshot(selector: String): String? {
        val bytes = page.locator(selector).screenshot()
        // convert to base64
        TODO("Not yet implemented")
    }

    override suspend fun captureScreenshot(clip: RectD): String? {
        TODO("Not yet implemented")
    }

    override suspend fun bringToFront() {
        page.bringToFront()
    }

    override suspend fun stopLoading() {
        page.pause()
    }

    override suspend fun stop() {
        page.pause()
    }

    override suspend fun terminate() {
        page.close()
    }

    override fun close() {
        page.close()
    }

    override fun awaitTermination() {
        closeCountDown.await()
    }
}
