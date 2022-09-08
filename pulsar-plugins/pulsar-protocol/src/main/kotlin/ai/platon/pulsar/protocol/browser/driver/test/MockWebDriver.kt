package ai.platon.pulsar.protocol.browser.driver.test

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.geometric.RectD
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
//import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightDriver
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.util.*

class MockBrowser(
    id: BrowserInstanceId,
    launcherOptions: LauncherOptions
): AbstractBrowser(id, launcherOptions.browserSettings) {
    override fun newDriver(): WebDriver {
        TODO("not implemented")
    }
}

class MockWebDriver(
    browserInstance: MockBrowser,
    backupDriverCreator: () -> WebDriver,
) : AbstractWebDriver(browserInstance) {
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

    override suspend fun addInitScript(script: String) {
        backupDriverOrNull?.addInitScript(script)
    }

    override suspend fun addBlockingUrls(urls: List<String>) = backupDriverOrNull?.addBlockingUrls(urls) ?: Unit

    override suspend fun navigateTo(entry: NavigateEntry) {
        backupDriverOrNull?.navigateTo(entry)
    }

    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        return backupDriverOrNull?.waitForSelector(selector, timeout) ?: 0
    }

    override suspend fun waitForNavigation(timeout: Duration): Long {
        return backupDriverOrNull?.waitForNavigation(timeout) ?: 0
    }

    override suspend fun setTimeouts(browserSettings: BrowserSettings) {
    }

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

    override suspend fun mainRequestHeaders(): Map<String, Any> {
        return backupDriverOrNull?.mainRequestHeaders() ?: mapOf()
    }

    override suspend fun mainRequestCookies(): List<Map<String, String>> {
        return backupDriverOrNull?.mainRequestCookies() ?: listOf()
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        return backupDriverOrNull?.getCookies() ?: listOf()
    }

    override suspend fun captureScreenshot(selector: String): String? {
        return backupDriverOrNull?.captureScreenshot(selector)
    }

    override suspend fun captureScreenshot(rect: RectD): String? {
        return backupDriverOrNull?.captureScreenshot(rect)
    }

    override suspend fun evaluate(expression: String): Any? {
        return backupDriverOrNull?.evaluate(expression)
    }

    override suspend fun scrollTo(selector: String) {
        backupDriverOrNull?.scrollTo(selector)
    }

    override val sessionId: String?
        get() = backupDriverOrNull?.sessionId

    override suspend fun currentUrl(): String = backupDriverOrNull?.currentUrl() ?: navigateUrl

    override suspend fun pageSource(): String = mockPageSource ?: (backupDriverOrNull?.pageSource()) ?: ""

    override suspend fun bringToFront() {
    }

    override suspend fun exists(selector: String) = backupDriverOrNull?.exists(selector) ?: false

    override suspend fun visible(selector: String) = backupDriverOrNull?.visible(selector) ?: false

    override suspend fun type(selector: String, text: String) {
        backupDriverOrNull?.type(selector, text)
    }

    override suspend fun click(selector: String, count: Int) {
        backupDriverOrNull?.click(selector, count)
    }

    override suspend fun clickMatches(selector: String, pattern: String, count: Int) {
        backupDriverOrNull?.clickMatches(selector, pattern, count)
    }

    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        backupDriverOrNull?.clickMatches(selector, attrName, pattern, count)
    }

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        backupDriverOrNull?.mouseWheelDown(count, deltaX, deltaY, delayMillis)
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        backupDriverOrNull?.mouseWheelUp(count, deltaX, deltaY, delayMillis)
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        backupDriverOrNull?.moveMouseTo(x, y)
    }

    override suspend fun dragAndDrop(selector: String, deltaX: Int, deltaY: Int) {
        backupDriverOrNull?.dragAndDrop(selector, deltaX, deltaY)
    }

    override suspend fun clickablePoint(selector: String) = backupDriverOrNull?.clickablePoint(selector)

    override suspend fun boundingBox(selector: String) = backupDriverOrNull?.boundingBox(selector)

    override fun toString() = "Mock driver ($lastSessionId)"

    override suspend fun stop() {
        backupDriverOrNull?.stop()
    }

    override suspend fun terminate() {
        backupDriverOrNull?.terminate()
    }

    override suspend fun stopLoading() {
        backupDriverOrNull?.stopLoading()
    }

    override fun awaitTermination() {
        backupDriverOrNull?.awaitTermination()
    }
    
    /**
     * Quit the browser instance
     * */
    override fun quit() {
        close()
    }

    /**
     * Close the tab hold by this driver
     * */
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
