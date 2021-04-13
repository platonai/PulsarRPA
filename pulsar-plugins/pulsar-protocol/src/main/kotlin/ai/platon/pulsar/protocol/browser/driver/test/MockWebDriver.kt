package ai.platon.pulsar.protocol.browser.driver.test

import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.LauncherConfig
import ai.platon.pulsar.browser.driver.chrome.util.ScreenshotException
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.protocol.browser.driver.BrowserInstanceManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*

/**
 * TODO: inherit from ai.platon.pulsar.crawl.fetch.driver.WebDriver
 * */
class MockWebDriver(
    launcherConfig: LauncherConfig,
    launchOptions: ChromeDevtoolsOptions,
    browserControl: WebDriverControl,
    browserInstanceManager: BrowserInstanceManager,
) : RemoteWebDriver() {
    private val log = LoggerFactory.getLogger(MockWebDriver::class.java)!!

    private val backupDriver by lazy {
        ChromeDevtoolsDriver(launcherConfig, launchOptions, browserControl, browserInstanceManager)
    }

    private var lastSessionId: SessionId? = null
    private var navigateUrl = ""
    private var mockPageSource: String? = null

    private val backupDriverOrNull get() = if (mockPageSource == null) backupDriver else null

    @Throws(NoSuchSessionException::class)
    override fun get(url: String) {
        log.info("Mock navigate to {}", url)

        lastSessionId = SessionId(UUID.randomUUID().toString())
        navigateUrl = url
        mockPageSource = loadMockPageSource(url)
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

    private fun loadMockPageSource(url: String): String? {
        val path = AppPaths.testDataPath(url)
        log.info("Load from path: \n{}", path)
        return path.takeIf { Files.exists(it) }?.let { Files.readString(it) }
    }
}
