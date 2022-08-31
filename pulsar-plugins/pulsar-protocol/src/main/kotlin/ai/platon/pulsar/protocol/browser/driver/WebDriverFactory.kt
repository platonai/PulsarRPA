package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.UnsupportedWebDriverException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowser
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowser
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

open class WebDriverFactory(
    val driverSettings: WebDriverSettings,
    val browserContext: BrowserContext,
    val immutableConfig: ImmutableConfig,
): AutoCloseable {
    private val logger = LoggerFactory.getLogger(WebDriverFactory::class.java)
    private val numDrivers = AtomicInteger()

    /**
     * Create a WebDriver
     */
    @Throws(DriverLaunchException::class)
    fun create(start: Boolean = true) = create(immutableConfig.toVolatileConfig(), start)

    /**
     * Create a WebDriver
     */
    @Throws(DriverLaunchException::class)
    fun create(conf: VolatileConfig, start: Boolean = true) = create(BrowserId.DEFAULT, 0, conf, start)

    /**
     * Create a WebDriver
     */
    @Throws(DriverLaunchException::class)
    @Synchronized
    fun create(
        browserId: BrowserId, priority: Int, conf: VolatileConfig, start: Boolean = true
    ): WebDriver {
        logger.debug("Creating web driver #{} | {}", numDrivers.incrementAndGet(), browserId)

        val capabilities = driverSettings.createGeneralOptions()
        browserId.proxyServer?.let { setProxy(capabilities, it) }

        // Choose the WebDriver
        val browserType = browserId.browserType

        try {
            val driver = when (browserType) {
                BrowserType.PULSAR_CHROME -> createChromeDevtoolsDriver(browserId, capabilities)
//                BrowserType.PLAYWRIGHT_CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserId, capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported WebDriver: $browserType")
            }

            if (start) {
                driver.startWork()
            }

            return WebDriverAdapter(driver, priority)
        } catch (e: DriverLaunchException) {
            logger.error("Can not launch browser $browserType | {}", e.message)
            throw e
        }
    }

    override fun close() {
        browserContext.close()
    }

    @Throws(DriverLaunchException::class)
    private fun newBrowser(
        instanceId: BrowserId, capabilities: Map<String, Any>,
    ): Browser {
        val launcherOptions = LauncherOptions(driverSettings)
        if (driverSettings.isSupervised) {
            launcherOptions.supervisorProcess = driverSettings.supervisorProcess
            launcherOptions.supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
        }

        val launchOptions = driverSettings.createChromeOptions(capabilities)
        return browserContext.launchIfAbsent(instanceId, launcherOptions, launchOptions)
    }

    @Throws(DriverLaunchException::class)
    private fun createChromeDevtoolsDriver(
        instanceId: BrowserId, capabilities: Map<String, Any>,
    ): ChromeDevtoolsDriver {
        require(instanceId.browserType == BrowserType.PULSAR_CHROME)
        val browser = newBrowser(instanceId, capabilities) as ChromeDevtoolsBrowser
        return browser.newDriver()
    }

//    private fun createPlaywrightDriver(
//        instanceId: BrowserId, capabilities: Map<String, Any>,
//    ): PlaywrightDriver {
//        require(instanceId.browserType == BrowserType.PLAYWRIGHT_CHROME)
//        val browser = createBrowserInstance(instanceId, capabilities)
//        return PlaywrightDriver(driverSettings, browser as PlaywrightBrowserInstance)
//    }

    @Throws(DriverLaunchException::class)
    private fun createMockChromeDevtoolsDriver(
        instanceId: BrowserId, capabilities: Map<String, Any>,
    ): MockWebDriver {
        require(instanceId.browserType == BrowserType.MOCK_CHROME)
        val browser = newBrowser(instanceId, capabilities) as MockBrowser
        val fingerprint = instanceId.fingerprint.copy(browserType = BrowserType.PULSAR_CHROME)
        val backupInstanceId = BrowserId(instanceId.contextDir, fingerprint)
        val backupDriverCreator = { createChromeDevtoolsDriver(backupInstanceId, capabilities) }
        return MockWebDriver(browser, backupDriverCreator)
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
