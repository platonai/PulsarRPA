package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.driver.BrowserInstance
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
    @Synchronized
    fun create(browserInstanceId: BrowserInstanceId, priority: Int, conf: VolatileConfig): WebDriverAdapter {
        logger.debug("Creating web driver #{} | {}", numDrivers.incrementAndGet(), browserInstanceId)

        val capabilities = driverSettings.createGeneralOptions()
        browserInstanceId.proxyServer?.let { setProxy(capabilities, it) }

        // Choose the WebDriver
        val browserType = browserInstanceId.browserType
        val driver = kotlin.runCatching {
            when (browserType) {
                BrowserType.PULSAR_CHROME -> createChromeDevtoolsDriver(browserInstanceId, capabilities)
//                BrowserType.PLAYWRIGHT_CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserInstanceId, capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported WebDriver: $browserType")
            }
        }.onFailure {
            logger.error("Failed to create web driver $browserType")
        }.getOrElse {
            throw DriverLaunchException("Failed to create web driver | $browserType")
        }

        return WebDriverAdapter(driver, priority)
    }

    override fun close() {
        browserInstanceManager.close()
    }

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

    private fun createChromeDevtoolsDriver(
        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
    ): ChromeDevtoolsDriver {
        require(instanceId.browserType == BrowserType.PULSAR_CHROME)
        val browserInstance = createBrowserInstance(instanceId, capabilities)
        return ChromeDevtoolsDriver(driverSettings, browserInstance as ChromeDevtoolsBrowserInstance)
    }

//    private fun createPlaywrightDriver(
//        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
//    ): PlaywrightDriver {
//        require(instanceId.browserType == BrowserType.PLAYWRIGHT_CHROME)
//        val browserInstance = createBrowserInstance(instanceId, capabilities)
//        return PlaywrightDriver(driverSettings, browserInstance as PlaywrightBrowserInstance)
//    }

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
