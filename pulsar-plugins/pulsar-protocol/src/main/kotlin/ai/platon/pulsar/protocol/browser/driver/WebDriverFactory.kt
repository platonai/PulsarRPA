package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.LauncherOptions
import ai.platon.pulsar.common.config.CapabilityTypes
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

    private fun createChromeDevtoolsDriver(
        browserInstanceId: BrowserInstanceId, capabilities: DesiredCapabilities,
    ): ChromeDevtoolsDriver {
        val launcherOptions = LauncherOptions(BrowserType.CHROME.name, driverSettings)
        if (driverSettings.isSupervised) {
            launcherOptions.supervisorProcess = driverSettings.supervisorProcess
            launcherOptions.supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
        }

        val launchOptions = driverSettings.createChromeOptions(capabilities)
        val browserInstance = browserInstanceManager.launchIfAbsent(browserInstanceId, launcherOptions, launchOptions)
        return ChromeDevtoolsDriver(driverSettings, browserInstance as ChromeDevtoolsBrowserInstance)
    }

    private fun createPlaywrightDriver(
        instanceId: BrowserInstanceId, capabilities: DesiredCapabilities,
    ): PlaywrightDriver {
        val launcherOptions = LauncherOptions(BrowserType.PLAYWRIGHT_CHROME.name, driverSettings)
        if (driverSettings.isSupervised) {
            launcherOptions.supervisorProcess = driverSettings.supervisorProcess
            launcherOptions.supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
        }

        val launchOptions = driverSettings.createChromeOptions(capabilities)
        val browserInstance = browserInstanceManager.launchIfAbsent(instanceId, launcherOptions, launchOptions)
        return PlaywrightDriver(driverSettings, browserInstance as PlaywrightBrowserInstance)
    }

    private fun createMockChromeDevtoolsDriver(
        instanceId: BrowserInstanceId, capabilities: DesiredCapabilities,
    ): MockWebDriver {
        val backupDriverCreator = { createPlaywrightDriver(instanceId, capabilities) }
        return MockWebDriver(instanceId, backupDriverCreator)
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
     *
     */
    private fun getBrowserType(volatileConfig: VolatileConfig?): BrowserType {
        val defaultType = BrowserType.CHROME
        return volatileConfig?.getEnum(CapabilityTypes.BROWSER_TYPE, defaultType)
            ?: immutableConfig.getEnum(CapabilityTypes.BROWSER_TYPE, defaultType)
    }
}
