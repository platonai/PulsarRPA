package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.UnsupportedWebDriverException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

open class WebDriverFactory(
    val driverSettings: WebDriverSettings,
    val browserInstanceManager: BrowserInstanceManager,
    val immutableConfig: ImmutableConfig,
): AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverFactory::class.java)
    val numDrivers = AtomicInteger()

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(DriverLaunchException::class)
    @Synchronized
    fun create(browserInstanceId: BrowserInstanceId, priority: Int, conf: VolatileConfig): WebDriverAdapter {
        log.debug("Creating web driver #{} | {}", numDrivers.incrementAndGet(), browserInstanceId)

        val capabilities = driverSettings.createGeneralOptions()
        browserInstanceId.proxyServer?.let { setProxy(capabilities, it) }

        // Choose the WebDriver
        val browserType = browserInstanceId.browserType
        val driver = kotlin.runCatching {
            when (browserType) {
                BrowserType.CHROME -> createChromeDevtoolsDriver(browserInstanceId, capabilities)
                BrowserType.PLAYWRIGHT_CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserInstanceId, capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported WebDriver: $browserType")
            }
        }.onFailure {
            log.error("Failed to create web driver $browserType", it)
        }.getOrElse {
            throw DriverLaunchException("Failed to create web driver | $browserType")
        }

        return WebDriverAdapter(browserInstanceId, driver, priority)
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
        val browserInstance = createBrowserInstance(instanceId, capabilities)
        return ChromeDevtoolsDriver(driverSettings, browserInstance as ChromeDevtoolsBrowserInstance)
    }

    private fun createPlaywrightDriver(
        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
    ): PlaywrightDriver {
        val browserInstance = createBrowserInstance(instanceId, capabilities)
        return PlaywrightDriver(driverSettings, browserInstance as PlaywrightBrowserInstance)
    }

    private fun createMockChromeDevtoolsDriver(
        instanceId: BrowserInstanceId, capabilities: Map<String, Any>,
    ): MockWebDriver {
        val backupDriverCreator = { createChromeDevtoolsDriver(instanceId, capabilities) }
        return MockWebDriver(instanceId, backupDriverCreator)
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
