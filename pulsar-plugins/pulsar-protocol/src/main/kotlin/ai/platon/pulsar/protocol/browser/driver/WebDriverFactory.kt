package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.UnsupportedWebDriverException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowser
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowser
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

open class WebDriverFactory(
    val driverSettings: WebDriverSettings,
    val browserManager: BrowserManager,
    val immutableConfig: ImmutableConfig,
) {
    private val logger = LoggerFactory.getLogger(WebDriverFactory::class.java)
    private val numDrivers = AtomicInteger()

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    fun create(start: Boolean = true) = create(immutableConfig.toVolatileConfig(), start)

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    fun create(conf: VolatileConfig, start: Boolean = true) = create(BrowserId.RANDOM, 0, conf, start)

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    fun create(browserId: BrowserId, priority: Int = 0, conf: VolatileConfig = VolatileConfig.UNSAFE, start: Boolean = true) =
        launchBrowserAndDriver(browserId, priority, conf, start).second

    @Throws(BrowserLaunchException::class)
    fun launchBrowser() = launchBrowser(BrowserId.DEFAULT)

    @Throws(BrowserLaunchException::class)
    fun launchTempBrowser() = launchBrowser(BrowserId.RANDOM)

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    fun launchBrowser(browserId: BrowserId, conf: VolatileConfig = VolatileConfig.UNSAFE): Browser {
        numDrivers.incrementAndGet()

        logger.debug("Creating browser #{} | {}", numDrivers, browserId)

        val browserType = browserId.browserType
        val capabilities = driverSettings.createGeneralOptions()
        setProxy(capabilities, browserId.fingerprint.proxyServer)

        try {
            val browser = when (browserType) {
                BrowserType.PULSAR_CHROME -> launchChrome(browserId, capabilities)
//                BrowserType.PLAYWRIGHT_CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                BrowserType.MOCK_CHROME -> launchMockChrome(browserId, capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported browser type: $browserType")
            }

            return browser
        } catch (e: BrowserLaunchException) {
            logger.error("Failed to launch browser {} | {}", browserType, e.message)
            throw e
        }
    }

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    private fun launchBrowserAndDriver(
        browserId: BrowserId, priority: Int, conf: VolatileConfig, start: Boolean = true
    ): Pair<Browser, WebDriver> {
        try {
            val browser = launchBrowser(browserId, conf)
            val driver = browser.newDriver()

            if (start) {
                driver.startWork()
            }

            return browser to WebDriverAdapter(driver, priority)
        } catch (e: BrowserLaunchException) {
            logger.error("Can not launch browser | {}", e.message)
            throw e
        }
    }

    @Throws(BrowserLaunchException::class)
    fun launchChrome(
        browserId: BrowserId, capabilities: Map<String, Any>,
    ): ChromeDevtoolsBrowser {
        require(browserId.browserType == BrowserType.PULSAR_CHROME)
        return browserManager.launch(browserId, driverSettings, capabilities) as ChromeDevtoolsBrowser
    }

//    private fun createPlaywrightDriver(
//        instanceId: BrowserId, capabilities: Map<String, Any>,
//    ): PlaywrightDriver {
//        require(instanceId.browserType == BrowserType.PLAYWRIGHT_CHROME)
//        val browser = createBrowserInstance(instanceId, capabilities)
//        return PlaywrightDriver(driverSettings, browser as PlaywrightBrowserInstance)
//    }

    @Throws(BrowserLaunchException::class)
    private fun launchMockChrome(browserId: BrowserId, capabilities: Map<String, Any>): MockBrowser {
        require(browserId.browserType == BrowserType.MOCK_CHROME)
        return browserManager.launch(browserId, driverSettings, capabilities) as MockBrowser
    }

    private fun setProxy(capabilities: MutableMap<String, Any>, proxyServer: String?) {
        if (proxyServer == null) {
            return
        }

//        val proxy = org.openqa.selenium.Proxy().apply {
//            httpProxy = proxyServer
//            sslProxy = proxyServer
//            ftpProxy = proxyServer
//        }

        capabilities["proxy"] = proxyServer
    }
}
