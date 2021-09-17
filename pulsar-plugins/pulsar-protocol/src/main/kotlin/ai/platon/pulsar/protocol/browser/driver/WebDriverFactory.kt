package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.LauncherConfig
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.openqa.selenium.Capabilities
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

open class WebDriverFactory(
    val driverSettings: WebDriverSettings,
    val browserInstanceManager: BrowserInstanceManager,
    val immutableConfig: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(WebDriverFactory::class.java)
    private val defaultWebDriverClass = immutableConfig.getClass(
            CapabilityTypes.BROWSER_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val localForwardServerEnabled =
            immutableConfig.getBoolean(CapabilityTypes.PROXY_ENABLE_LOCAL_FORWARD_SERVER, false)
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
                browserType == BrowserType.MOCK_CHROME -> createMockChromeDevtoolsDriver(browserInstanceId, capabilities)
                browserType == BrowserType.CHROME -> createChromeDevtoolsDriver(browserInstanceId, capabilities)
                browserType == BrowserType.SELENIUM_CHROME -> ChromeDriver(driverSettings.createChromeOptions(capabilities))
                RemoteWebDriver::class.java.isAssignableFrom(defaultWebDriverClass) ->
                    defaultWebDriverClass.getConstructor(Capabilities::class.java).newInstance(capabilities)
                else -> defaultWebDriverClass.getConstructor().newInstance()
            }
        }.onFailure {
            log.error("Failed to create web driver $browserType", it)
        }.getOrElse {
            throw DriverLaunchException("Failed to create web driver $browserType")
        }

        if (driver is ChromeDriver) {
//            val fakeAgent = driverControl.randomUserAgent()
//            val devTools = driver.devTools
//            devTools.createSession()
//            devTools.send(Log.enable())
//            devTools.addListener(Log.entryAdded()) { e -> log.error(e.text) }
//            devTools.send(Network.setUserAgentOverride(fakeAgent, Optional.empty(), Optional.empty()))
//
//             devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));
//             devTools.send(emulateNetworkConditions(false,100,200000,100000, Optional.of(ConnectionType.cellular4g)));
        }

        return WebDriverAdapter(browserInstanceId, driver, priority)
    }

    private fun createChromeDevtoolsDriver(
        browserInstanceId: BrowserInstanceId, capabilities: DesiredCapabilities): ChromeDevtoolsDriver {
        val launcherConfig = LauncherConfig().apply {
            supervisorProcess = driverSettings.supervisorProcess
            supervisorProcessArgs.addAll(driverSettings.supervisorProcessArgs)
        }
        val launchOptions = driverSettings.createChromeDevtoolsOptions(capabilities).apply {
            userDataDir = browserInstanceId.dataDir
        }
        return ChromeDevtoolsDriver(launcherConfig, launchOptions, driverSettings, browserInstanceManager)
    }

    private fun createMockChromeDevtoolsDriver(
            browserInstanceId: BrowserInstanceId, capabilities: DesiredCapabilities): MockWebDriver {
        val backupDriverCreator = { createChromeDevtoolsDriver(browserInstanceId, capabilities) }
        return MockWebDriver(backupDriverCreator)
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
