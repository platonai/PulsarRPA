package ai.platon.pulsar.protocol.browser.driver.test

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightDriver
import org.openqa.selenium.NoSuchSessionException
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class MockWebDriver(
    browserInstanceId: BrowserInstanceId,
    backupDriverCreator: () -> PlaywrightDriver,
) : AbstractWebDriver(browserInstanceId) {
    private val log = LoggerFactory.getLogger(MockWebDriver::class.java)!!

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
        get() = if (realDriver is PlaywrightDriver)
            BrowserType.CHROME else BrowserType.MOCK_CHROME

    override val supportJavascript: Boolean
        get() = when (realDriver) {
            is PlaywrightDriver -> true
            else -> false
        }

    override val isMockedPageSource: Boolean get() = mockPageSource != null

    override suspend fun setTimeouts(driverConfig: BrowserSettings) {
    }

    override suspend fun navigateTo(url: String) {
        log.info("Mock navigate to {}", url)

        if (lastSessionId == null) {
            lastSessionId = UUID.randomUUID().toString()
        }
        navigateUrl = url
        mockPageSource = loadMockPageSourceOrNull(url)
        if (mockPageSource == null) {
            log.info("Resource does not exist, fallback to PlaywrightDriver | {}", url)
        }

        backupDriverOrNull?.navigateTo(url)
    }

    override suspend fun stopLoading() {
        backupDriverOrNull?.stopLoading()
    }

    override suspend fun evaluate(expression: String): Any? {
        return backupDriverOrNull?.evaluate(expression)
    }

    override val sessionId: String?
        @Throws(NoSuchSessionException::class)
        get() {
            return backupDriverOrNull?.sessionId
        }

    override val currentUrl: String
        get() = backupDriverOrNull?.currentUrl ?: navigateUrl

    override suspend fun pageSource(): String = mockPageSource ?: (backupDriverOrNull?.pageSource()) ?: ""

    override suspend fun bringToFront() {
        backupDriverOrNull?.bringToFront()
    }

//    @Throws(ScreenshotException::class, NoSuchSessionException::class)
//    override fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X? {
//        return backupDriverOrNull?.getScreenshotAs(outputType)
//    }

    override fun toString() = "Mock driver ($lastSessionId)"

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

        log.info("Loading from path: \n{}", mockPath)
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
