package ai.platon.pulsar.protocol.browser.driver.selenium

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.PointD
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.NavigateEntry
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.interactions.Actions
import org.slf4j.LoggerFactory
import java.time.Duration

class SeleniumDriver(
    private val browserSettings: BrowserSettings,
    browser: SeleniumBrowser,
) : AbstractWebDriver(browser) {

    private val logger = LoggerFactory.getLogger(SeleniumDriver::class.java)!!

    override val browserType: BrowserType
        get() = TODO("Not yet implemented")

    private val driver = ChromeDriver()

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {

    }

    override suspend fun addInitScript(script: String) {
        // driver.executeAsyncScript()
    }

    override suspend fun addBlockedURLs(urls: List<String>) {

    }

    override suspend fun navigateTo(entry: NavigateEntry) {
        driver.get(entry.url)
    }

    override suspend fun currentUrl(): String {
        return driver.currentUrl
    }

    override suspend fun pageSource(): String? {
        return driver.pageSource
    }

    override suspend fun mainRequestCookies(): List<Map<String, String>> {

        TODO("Not yet implemented")
    }

    override suspend fun mainRequestHeaders(): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun evaluate(expression: String): Any? {
        return driver.executeScript(expression)
    }

    override suspend fun exists(selector: String): Boolean {
        return select(selector) != null
    }

    override suspend fun isVisible(selector: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        TODO("Not yet implemented")
    }

    override suspend fun waitForNavigation(timeout: Duration): Long {
        TODO("Not yet implemented")
    }

    override suspend fun click(selector: String, count: Int) {
        select(selector)?.click()
    }

    override suspend fun type(selector: String, text: String) {
        select(selector)?.sendKeys(text)
    }

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        val mouseAction = Actions(driver)
            .scrollByAmount(deltaX.toInt(), deltaY.toInt())
            .build()
        mouseAction.perform()
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        mouseWheelDown(count, -deltaX, -deltaY, delayMillis)
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        val mouseAction = Actions(driver)
            .moveByOffset(x.toInt(), y.toInt())
            .build()
        mouseAction.perform()
    }

    override suspend fun scrollTo(selector: String) {
        TODO("Not yet implemented")
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun clickablePoint(selector: String): PointD? {
        TODO("Not yet implemented")
    }

    override suspend fun boundingBox(selector: String): RectD? {
        TODO("Not yet implemented")
    }

    override suspend fun captureScreenshot(selector: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun captureScreenshot(rect: RectD): String? {
        TODO("Not yet implemented")
    }

    override suspend fun bringToFront() {
        TODO("Not yet implemented")
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    override suspend fun terminate() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun awaitTermination() {
        TODO("Not yet implemented")
    }

    private fun select(selector: String): WebElement? {
        val element = driver.findElement(By.cssSelector(selector))


        return element
    }
}
