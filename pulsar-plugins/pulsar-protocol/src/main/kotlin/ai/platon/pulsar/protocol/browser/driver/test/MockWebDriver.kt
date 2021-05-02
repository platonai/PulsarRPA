package ai.platon.pulsar.protocol.browser.driver.test

import ai.platon.pulsar.browser.driver.chrome.util.ScreenshotException
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class MockWebDriver(
    backupDriverCreator: () -> ChromeDevtoolsDriver,
) : RemoteWebDriver() {
    private val log = LoggerFactory.getLogger(MockWebDriver::class.java)!!

    private val backupDriver by lazy { backupDriverCreator() }

    private var lastSessionId: SessionId? = null
    private var navigateUrl = ""

    var mockPageSource: String? = null
        private set

    private val backupDriverOrNull
        @Synchronized
        get() = if (mockPageSource == null) backupDriver else null

    val realDriver: RemoteWebDriver get() = backupDriverOrNull ?: this

    val browserType: BrowserType
        get() = if (realDriver is ChromeDevtoolsDriver)
            BrowserType.CHROME else BrowserType.MOCK_CHROME

    val supportJavascript: Boolean
        get() = when (realDriver) {
            is ChromeDevtoolsDriver -> true
            else -> false
        }

    val isMockedPageSource: Boolean get() = mockPageSource != null

    @Throws(NoSuchSessionException::class)
    override fun get(url: String) {
        log.info("Mock navigate to {}", url)

        lastSessionId = SessionId(UUID.randomUUID().toString())
        navigateUrl = url
        mockPageSource = loadMockPageSourceOrNull(url)
        if (mockPageSource == null) {
            log.info("Resource does not exist, fallback to ChromeDevtoolsDriver | {}", url)
        }

        backupDriverOrNull?.get(url)
    }

    @Throws(NoSuchSessionException::class)
    fun stopLoading() {
        backupDriverOrNull?.stopLoading()
    }

    @Throws(NoSuchSessionException::class)
    fun evaluate(expression: String): Any? {
        return backupDriverOrNull?.evaluate(expression)
    }

    @Throws(NoSuchSessionException::class)
    override fun executeScript(script: String, vararg args: Any): Any? {
        return backupDriverOrNull?.executeScript(script, args)
    }

    @Throws(NoSuchSessionException::class)
    override fun getSessionId(): SessionId? {
        return backupDriverOrNull?.getSessionId()
    }

    @Throws(NoSuchSessionException::class)
    override fun getCurrentUrl(): String {
        return backupDriverOrNull?.currentUrl ?: navigateUrl
    }

    @Throws(NoSuchSessionException::class)
    override fun getWindowHandles(): Set<String> {
        return backupDriverOrNull?.windowHandles ?: setOf()
    }

    @Throws(NoSuchSessionException::class)
    override fun getPageSource(): String {
        return mockPageSource ?: (backupDriverOrNull?.pageSource) ?: ""
    }

    @Throws(NoSuchSessionException::class)
    fun bringToFront() {
        backupDriverOrNull?.bringToFront()
    }

    @Throws(ScreenshotException::class, NoSuchSessionException::class)
    override fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X? {
        return backupDriverOrNull?.getScreenshotAs(outputType)
    }

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
