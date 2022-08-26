package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.driver.BrowserInstance
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.UnsupportedWebDriverException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

open class WebDriverFactory(
    val driverSettings: WebDriverSettings,
    val browserInstanceManager: BrowserInstanceManager,
    val immutableConfig: ImmutableConfig,
): AutoCloseable {
    private val logger = LoggerFactory.getLogger(WebDriverFactory::class.java)
    val numDrivers = AtomicInteger()

    /**
     * Create a WebDriver
     */
    @Throws(DriverLaunchException::class)
    fun create() = create(immutableConfig.toVolatileConfig())

    /**
     * Create a WebDriver
     */
    @Throws(DriverLaunchException::class)
    fun create(conf: VolatileConfig) = create(BrowserInstanceId.DEFAULT, 0, conf)

    /**
     * Create a WebDriver
     */
    @Throws(DriverLaunchException::class)
    @Synchronized
    fun create(browserInstanceId: BrowserInstanceId, priority: Int, conf: VolatileConfig): WebDriver {
        logger.debug("Creating web driver #{} | {}", numDrivers.incrementAndGet(), browserInstanceId)

        val capabilities = driverSettings.createGeneralOptions()
        browserInstanceId.proxyServer?.let { setProxy(capabilities, it) }

        // Choose the WebDriver
        val browserType = browserInstanceId.browserType

        try {
            val driver = when (browserType) {
                BrowserType.PULSAR_CHROME -> createChromeDevtoolsDriver(browserInstanceId, capabilities)
//                BrowserType.PLAYWRIGHT_CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserInstanceId, capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported WebDriver: $browserType")
            }

            // driver.startWork()
            return WebDriverAdapter(driver, priority)
        } catch (e: DriverLaunchException) {
            logger.error("Can not launch browser $browserType | {}", e.message)
            throw e
        }
    }

    override fun close() {
        browserInstanceManager.close()
    }

    @Throws(DriverLaunchException::class)
    private fun createBrowserInstance(
        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
    ): BrowserInstance {
        val launcherOptions = LauncherOptions(driverSettings)
        if (driverSettings.isSupervised) {
            launcherOptions.supervisorProcess = driverSettings.supervisorProcess
            launcherOptions.supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
        }

        val launchOptions = driverSettings.createChromeOptions(capabilities)
        return browserInstanceManager.launchIfAbsent(instanceId, launcherOptions, launchOptions)
    }

    @Throws(DriverLaunchException::class)
    private fun createChromeDevtoolsDriver(
        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
    ): ChromeDevtoolsDriver {
        require(instanceId.browserType == BrowserType.PULSAR_CHROME)
        val browserInstance = createBrowserInstance(instanceId, capabilities) as ChromeDevtoolsBrowserInstance

        return browserInstance.createDriver(driverSettings)
    }

//    private fun createPlaywrightDriver(
//        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
//    ): PlaywrightDriver {
//        require(instanceId.browserType == BrowserType.PLAYWRIGHT_CHROME)
//        val browserInstance = createBrowserInstance(instanceId, capabilities)
//        return PlaywrightDriver(driverSettings, browserInstance as PlaywrightBrowserInstance)
//    }

    @Throws(DriverLaunchException::class)
    private fun createMockChromeDevtoolsDriver(
        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
    ): MockWebDriver {
        require(instanceId.browserType == BrowserType.MOCK_CHROME)
        val browserInstance = createBrowserInstance(instanceId, capabilities) as MockBrowserInstance
        val fingerprint = instanceId.fingerprint.copy(browserType = BrowserType.PULSAR_CHROME)
        val backupInstanceId = BrowserInstanceId(instanceId.contextDir, fingerprint)
        val backupDriverCreator = { createChromeDevtoolsDriver(backupInstanceId, capabilities) }
        return MockWebDriver(browserInstance, backupDriverCreator)
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
