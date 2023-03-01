package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
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
    fun create(conf: VolatileConfig, start: Boolean = true) = create(BrowserId.DEFAULT, 0, conf, start)

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    @Synchronized
    fun create(
        browserId: BrowserId, priority: Int, conf: VolatileConfig, start: Boolean = true
    ): WebDriver {
        return createBrowserAndDriver(browserId, priority, conf, start).second
    }

    /**
     * Create a WebDriver
     */
    @Throws(BrowserLaunchException::class)
    @Synchronized
    fun createBrowserAndDriver(
        browserId: BrowserId, priority: Int, conf: VolatileConfig, start: Boolean = true
    ): Pair<Browser, WebDriver> {
        logger.debug("Creating web driver #{} | {}", numDrivers.incrementAndGet(), browserId)

        val capabilities = driverSettings.createGeneralOptions()
        browserId.proxyServer?.let { setProxy(capabilities, it) }

        // Choose the WebDriver
        val browserType = browserId.browserType

        try {
            val (browser, driver) = when (browserType) {
                BrowserType.PULSAR_CHROME -> createChromeDevtoolsDriver(browserId, capabilities)
//                BrowserType.PLAYWRIGHT_CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserId, capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported WebDriver: $browserType")
            }

            if (start) {
                driver.startWork()
            }

            return browser to WebDriverAdapter(driver, priority)
        } catch (e: BrowserLaunchException) {
            logger.error("Can not launch browser $browserType | {}", e.message)
            throw e
        }
    }

    @Throws(BrowserLaunchException::class)
    private fun createChromeDevtoolsDriver(
        browserId: BrowserId, capabilities: Map<String, Any>,
    ): Pair<ChromeDevtoolsBrowser, ChromeDevtoolsDriver> {
        require(browserId.browserType == BrowserType.PULSAR_CHROME)
        val browser = browserManager.launch(browserId, driverSettings, capabilities) as ChromeDevtoolsBrowser
        return browser to browser.newDriver()
    }

//    private fun createPlaywrightDriver(
//        instanceId: BrowserId, capabilities: Map<String, Any>,
//    ): PlaywrightDriver {
//        require(instanceId.browserType == BrowserType.PLAYWRIGHT_CHROME)
//        val browser = createBrowserInstance(instanceId, capabilities)
//        return PlaywrightDriver(driverSettings, browser as PlaywrightBrowserInstance)
//    }

    @Throws(BrowserLaunchException::class)
    private fun createMockChromeDevtoolsDriver(
        instanceId: BrowserId, capabilities: Map<String, Any>,
    ): Pair<MockBrowser, MockWebDriver> {
        require(instanceId.browserType == BrowserType.MOCK_CHROME)
        val browser = browserManager.launch(instanceId, driverSettings, capabilities) as MockBrowser
        val fingerprint = instanceId.fingerprint.copy(browserType = BrowserType.PULSAR_CHROME)
        val backupInstanceId = BrowserId(instanceId.contextDir, fingerprint)
        val backupDriverCreator = { createChromeDevtoolsDriver(backupInstanceId, capabilities).second }
        return browser to MockWebDriver(browser, backupDriverCreator)
    }

    private fun setProxy(capabilities: MutableMap<String, Any>, proxyServer: String) {
//        val proxy = org.openqa.selenium.Proxy().apply {
//            httpProxy = proxyServer
//            sslProxy = proxyServer
//            ftpProxy = proxyServer
//        }
        capabilities["proxy"] = proxyServer
    }
}
