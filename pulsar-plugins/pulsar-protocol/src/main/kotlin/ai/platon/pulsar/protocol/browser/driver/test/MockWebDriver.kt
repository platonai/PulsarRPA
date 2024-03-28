package ai.platon.pulsar.protocol.browser.driver.test

//import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightDriver
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.*
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import org.slf4j.LoggerFactory
import java.awt.SystemColor.text
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.util.*

class MockBrowser(
    id: BrowserId,
    browserSettings: BrowserSettings,
    private val backupBrowser: Browser,
): AbstractBrowser(id, browserSettings) {
    
    override fun newDriver(): WebDriver {
        return MockWebDriver(this) { backupBrowser.newDriver() }
    }
}

class MockWebDriver(
    browser: MockBrowser,
    backupDriverCreator: () -> WebDriver,
) : AbstractWebDriver(browser) {
    private val logger = LoggerFactory.getLogger(MockWebDriver::class.java)!!

    private val backupDriver by lazy { backupDriverCreator() }

    private var lastSessionId: String? = null
    private var navigateUrl = ""

    var mockPageSource: String? = null
        private set

    private val backupDriverOrNull
        @Synchronized
        get() = if (mockPageSource == null) backupDriver else null

    val realDriver: WebDriver get() = backupDriverOrNull ?: this

    override val browserType: BrowserType
        get() = if (realDriver == this) BrowserType.MOCK_CHROME else realDriver.browserType

    override val supportJavascript: Boolean
        get() = when (realDriver) {
            is ChromeDevtoolsDriver -> true
            else -> false
        }

    override val isMockedPageSource: Boolean get() = mockPageSource != null

    @Throws(WebDriverException::class)
    override suspend fun addInitScript(script: String) {
        backupDriverOrNull?.addInitScript(script)
    }

    override suspend fun addBlockedURLs(urls: List<String>) = backupDriverOrNull?.addBlockedURLs(urls) ?: Unit

    override suspend fun addProbabilityBlockedURLs(urlPatterns: List<String>) =
        backupDriverOrNull?.addProbabilityBlockedURLs(urlPatterns) ?: Unit

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(entry: NavigateEntry) {
        backupDriverOrNull?.navigateTo(entry)
    }

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        return backupDriverOrNull?.waitForSelector(selector, timeout) ?: 0
    }

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(timeout: Duration): Long {
        return backupDriverOrNull?.waitForNavigation(timeout) ?: 0
    }

    @Throws(WebDriverException::class)
    override suspend fun waitForPage(url: String, timeout: Duration): WebDriver? {
        return backupDriverOrNull?.waitForPage(url, timeout)
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(url: String) {
        logger.info("Mock navigate to {}", url)

        if (lastSessionId == null) {
            lastSessionId = UUID.randomUUID().toString()
        }
        navigateUrl = url
        mockPageSource = loadMockPageSourceOrNull(url)
        if (mockPageSource == null) {
            logger.info("Resource does not exist, fallback to backup driver | {}", url)
        }

        backupDriverOrNull?.navigateTo(url)
    }

    @Throws(WebDriverException::class)
    override suspend fun getCookies(): List<Map<String, String>> {
        return backupDriverOrNull?.getCookies() ?: listOf()
    }

    @Throws(WebDriverException::class)
    override suspend fun clearBrowserCookies() {
        backupDriverOrNull?.clearBrowserCookies()
    }

    @Throws(WebDriverException::class)
    override suspend fun deleteCookies(name: String) {
        backupDriverOrNull?.deleteCookies(name)
    }

    @Throws(WebDriverException::class)
    override suspend fun deleteCookies(name: String, url: String?, domain: String?, path: String?) {
        backupDriverOrNull?.deleteCookies(name, url, domain, path)
    }

    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(selector: String): String? {
        return backupDriverOrNull?.captureScreenshot(selector)
    }

    @Throws(WebDriverException::class)
    override suspend fun captureScreenshot(rect: RectD): String? {
        return backupDriverOrNull?.captureScreenshot(rect)
    }

    @Throws(WebDriverException::class)
    override suspend fun evaluate(expression: String): Any? {
        return backupDriverOrNull?.evaluate(expression)
    }

    @Throws(WebDriverException::class)
    override suspend fun evaluateDetail(expression: String): JsEvaluation? {
        return backupDriverOrNull?.evaluateDetail(expression)
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollTo(selector: String) {
        backupDriverOrNull?.scrollTo(selector)
    }

    @Throws(WebDriverException::class)
    override suspend fun currentUrl(): String = backupDriverOrNull?.currentUrl() ?: navigateUrl

    override suspend fun baseURI() = backupDriverOrNull?.baseURI() ?: ""

    override suspend fun location() = backupDriverOrNull?.location() ?: ""

    @Throws(WebDriverException::class)
    override suspend fun pageSource(): String = mockPageSource ?: (backupDriverOrNull?.pageSource()) ?: ""

    @Throws(WebDriverException::class)
    override suspend fun bringToFront() {
    }

    @Throws(WebDriverException::class)
    override suspend fun exists(selector: String) = backupDriverOrNull?.exists(selector) ?: false

    @Throws(WebDriverException::class)
    override suspend fun isVisible(selector: String) = backupDriverOrNull?.isVisible(selector) ?: false

    @Throws(WebDriverException::class)
    override suspend fun focus(selector: String) {
        backupDriverOrNull?.focus(selector)
    }

    @Throws(WebDriverException::class)
    override suspend fun type(selector: String, text: String) {
        backupDriverOrNull?.type(selector, text)
    }
    
    @Throws(WebDriverException::class)
    override suspend fun fill(selector: String, text: String) {
        backupDriverOrNull?.fill(selector, text)
    }
    
    @Throws(WebDriverException::class)
    override suspend fun press(selector: String, key: String) {
        backupDriverOrNull?.press(selector, key)
    }
    
    @Throws(WebDriverException::class)
    override suspend fun click(selector: String, count: Int) {
        backupDriverOrNull?.click(selector, count)
    }

    override suspend fun clickTextMatches(selector: String, pattern: String, count: Int) {
        backupDriverOrNull?.clickTextMatches(selector, pattern, count)
    }

    @Throws(WebDriverException::class)
    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        backupDriverOrNull?.clickMatches(selector, attrName, pattern, count)
    }

    @Throws(WebDriverException::class)
    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        backupDriverOrNull?.mouseWheelDown(count, deltaX, deltaY, delayMillis)
    }

    @Throws(WebDriverException::class)
    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        backupDriverOrNull?.mouseWheelUp(count, deltaX, deltaY, delayMillis)
    }

    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(x: Double, y: Double) {
        backupDriverOrNull?.moveMouseTo(x, y)
    }

    @Throws(WebDriverException::class)
    override suspend fun moveMouseTo(selector: String, deltaX: Int, deltaY: Int) {
        backupDriverOrNull?.moveMouseTo(selector, deltaX, deltaY)
    }

    @Throws(WebDriverException::class)
    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        backupDriverOrNull?.dragAndDrop(selector, deltaX, deltaY)
    }

    @Throws(WebDriverException::class)
    override suspend fun clickablePoint(selector: String) = backupDriverOrNull?.clickablePoint(selector)

    @Throws(WebDriverException::class)
    override suspend fun boundingBox(selector: String) = backupDriverOrNull?.boundingBox(selector)

    override fun toString() = "Mock driver ($lastSessionId)"

    @Throws(WebDriverException::class)
    override suspend fun stop() {
        backupDriverOrNull?.stop()
    }

    @Throws(WebDriverException::class)
    override suspend fun terminate() {
        backupDriverOrNull?.terminate()
    }

    @Throws(WebDriverException::class)
    override suspend fun pause() {
        backupDriverOrNull?.pause()
    }

    @Throws(WebDriverException::class)
    override fun awaitTermination() {
        backupDriverOrNull?.awaitTermination()
    }

    /**
     * Close the tab hold by this driver
     * */
    @Throws(Exception::class)
    override fun close() {
        backupDriverOrNull?.close()
    }

    private fun loadMockPageSourceOrNull(url: String): String? {
        val mockPath = AppPaths.mockPagePath(url)
        if (!Files.exists(mockPath)) {
            val path = searchExport(url)
            if (path != null) {
                Files.copy(path, mockPath)
            }
        }

        logger.info("Loading from path: \n{}", mockPath)
        return mockPath.takeIf { Files.exists(it) }?.let { Files.readString(it) }
    }

    private fun searchExport(url: String): Path? {
        val fileId = AppPaths.fileId(url)
        val searchPath = AppPaths.WEB_CACHE_DIR.resolve("original")
        if (!Files.exists(searchPath)) {
            return null
        }

        val matcher = { path: Path, attr: BasicFileAttributes ->
            attr.isRegularFile && path.toAbsolutePath().toString().let { fileId in it && "OK" in it }
        }
        return Files.find(searchPath, 10, matcher).findFirst().orElse(null)
    }
}
