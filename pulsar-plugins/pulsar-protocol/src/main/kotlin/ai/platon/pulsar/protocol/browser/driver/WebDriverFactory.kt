package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.LauncherOptions
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.UnsupportedWebDriverException
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

open class WebDriverFactory(
    val driverSettings: WebDriverSettings,
    val browserInstanceManager: BrowserInstanceManager,
    val immutableConfig: ImmutableConfig,
) {
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
        val browserType = getBrowserType(conf)
        val driver = kotlin.runCatching {
            when {
                browserType == BrowserType.CHROME -> createPlaywrightDriver(browserInstanceId, capabilities)
                browserType == BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserInstanceId,
                    capabilities)
                else -> throw UnsupportedWebDriverException("Unsupported WebDriver: $browserType")
            }
        }.onFailure {
            log.error("Failed to create web driver $browserType", it)
        }.getOrElse {
            throw DriverLaunchException("Failed to create web driver | $browserType")
        }

        return WebDriverAdapter(browserInstanceId, driver, priority)
    }

    private fun createPlaywrightDriver(
        browserInstanceId: BrowserInstanceId, capabilities: DesiredCapabilities,
    ): PlaywrightDriver {
        val launcherOptions = LauncherOptions().apply {
            if (driverSettings.isSupervised) {
                supervisorProcess = driverSettings.supervisorProcess
                supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
            }
        }

        val launchOptions = driverSettings.createChromeOptions(capabilities)

        val userDataDir = driverSettings.userDataDir
        val browserInstance = browserInstanceManager.launchIfAbsent(userDataDir, launcherOptions, launchOptions)
        return PlaywrightDriver(driverSettings, browserInstance as PlaywrightBrowserInstance)
    }

    private fun createMockChromeDevtoolsDriver(
        browserInstanceId: BrowserInstanceId, capabilities: DesiredCapabilities,
    ): MockWebDriver {
        val backupDriverCreator = { createPlaywrightDriver(browserInstanceId, capabilities) }
        return MockWebDriver(browserInstanceId, backupDriverCreator)
    }

    private fun setProxy(capabilities: DesiredCapabilities, proxyServer: String) {
        val proxy = org.openqa.selenium.Proxy().apply {
            httpProxy = proxyServer
            sslProxy = proxyServer
            ftpProxy = proxyServer
        }
        capabilities.setCapability(CapabilityType.PROXY, proxy)
    }

    /**
     * Speed: native > htmlunit > chrome
     * Quality: chrome > htmlunit > native
     *
     * We support CHROME only now
     */
    private fun getBrowserType(volatileConfig: VolatileConfig?): BrowserType {
        return volatileConfig?.getEnum(CapabilityTypes.BROWSER_TYPE, BrowserType.CHROME)
            ?: immutableConfig.getEnum(CapabilityTypes.BROWSER_TYPE, BrowserType.CHROME)
    }
}
