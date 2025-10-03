package ai.platon.pulsar

import ai.platon.pulsar.browser.WebDriverService
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.SimpleScriptConfuser
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WebDriverTestBase : TestWebSiteAccess() {

    companion object {
        val isInitialized = AtomicBoolean(false)
        lateinit var browser: Browser

        @JvmStatic
        @AfterAll
        fun closeBrowser() {
            if (isInitialized.compareAndSet(true, false)) {
                browser.close()
            }
        }
    }

    @BeforeEach
    fun initBrowser() {
        synchronized(isInitialized) {
            if (isInitialized.compareAndSet(false, true)) {
                browser = browserFactory.launchRandomTempBrowser()
                browser.newDriver()
            }
        }
    }

    val browserFactory get() = context.getBeanOrNull(BrowserFactory::class) ?: DefaultBrowserFactory(session.configuration)

    open val webDriverService get() = WebDriverService(browserFactory)

    val settings get() = BrowserSettings(session.sessionConfig)
    val confuser get() = settings.confuser as SimpleScriptConfuser

    /**
     * Run webdriver test with the default browser.
     * */
    protected fun runWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(url, browser, block)

    /**
     * Run webdriver test with the default browser.
     * */
    protected fun runWebDriverTest(block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(browser, block)

    /**
     * Run webdriver test with a specified browser.
     * */
    protected fun runWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(url, browser, block)

    /**
     * Run webdriver test with a specified browser.
     * */
    protected fun runWebDriverTest(browser: Browser, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(browser, block)

    /**
     * Run webdriver test with a newly created browser with the given browser profile.
     * */
    protected fun runWebDriverTest(browserId: BrowserId, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(browserId, block)

    protected fun runResourceWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runResourceWebDriverTest(url, block)

    protected fun runResourceWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runResourceWebDriverTest(url, browser, block)

    protected suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 3) = webDriverService.open(url, driver, scrollCount)

    protected suspend fun openResource(url: String, driver: WebDriver, scrollCount: Int = 1) = webDriverService.openResource(url, driver, scrollCount)
}
