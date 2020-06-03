package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import org.openqa.selenium.Capabilities
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory

class WebDriverFactory(
        val driverControl: WebDriverControl,
        val proxyPoolMonitor: ProxyPoolMonitor,
        val conf: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(WebDriverFactory::class.java)
    private val defaultWebDriverClass = conf.getClass(
            CapabilityTypes.BROWSER_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)
    private val localForwardServerEnabled =
            conf.getBoolean(CapabilityTypes.PROXY_ENABLE_LOCAL_FORWARD_SERVER, false)

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(DriverLaunchException::class)
    fun create(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        val capabilities = driverControl.createGeneralOptions()

        if (proxyPoolMonitor.isEnabled) {
            setProxy(capabilities)
        }

        // Choose the WebDriver
        val browserType = getBrowserType(conf)

        val driver = kotlin.runCatching {
            when {
                browserType == BrowserType.CHROME -> {
                    val options = driverControl.createChromeDevtoolsOptions(capabilities)
                    ChromeDevtoolsDriver(driverControl.randomUserAgent(), driverControl, options)
                }
                browserType == BrowserType.SELENIUM_CHROME -> {
                    ChromeDriver(driverControl.createChromeOptions(capabilities))
                }
                RemoteWebDriver::class.java.isAssignableFrom(defaultWebDriverClass) -> {
                    defaultWebDriverClass.getConstructor(Capabilities::class.java).newInstance(capabilities)
                }
                else -> defaultWebDriverClass.getConstructor().newInstance()
            }
        }.onFailure {
            log.error("Failed to create web driver $browserType", it)
        }.getOrElse {
            throw DriverLaunchException("Failed to create web driver $browserType")
        }

        if (driver is ChromeDriver) {
            val fakeAgent = driverControl.randomUserAgent()
//            val devTools = driver.devTools
//            devTools.createSession()
//            devTools.send(Log.enable())
//            devTools.addListener(Log.entryAdded()) { e -> log.error(e.text) }
//            devTools.send(Network.setUserAgentOverride(fakeAgent, Optional.empty(), Optional.empty()))

            // devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));
            // devTools.send(emulateNetworkConditions(false,100,200000,100000, Optional.of(ConnectionType.cellular4g)));
        }

        return ManagedWebDriver(driver, priority)
    }

    private fun setProxy(capabilities: DesiredCapabilities): ProxyEntry? {
        var proxyEntry: ProxyEntry? = null
        var hostPort: String? = null
        val proxy = org.openqa.selenium.Proxy()

        if (proxyPoolMonitor.waitUntilOnline()) {
            val port = proxyPoolMonitor.localPort
            if (port > 0) {
                proxyEntry = proxyPoolMonitor.currentProxyEntry
                hostPort = "127.0.0.1:${proxyPoolMonitor.localPort}".takeIf { localForwardServerEnabled }?:proxyEntry?.hostPort
            } else {
                log.info("Invalid port for proxy connector, proxy is disabled")
            }
        }

        proxy.httpProxy = hostPort
        proxy.sslProxy = hostPort
        proxy.ftpProxy = hostPort

        capabilities.setCapability(CapabilityType.PROXY, proxy)

        return proxyEntry
    }

    /**
     * TODO: choose a best browser automatically: which one is faster yet still have good result
     * Speed: native > htmlunit > chrome
     * Quality: chrome > htmlunit > native
     */
    private fun getBrowserType(mutableConfig: ImmutableConfig?): BrowserType {
        return mutableConfig?.getEnum(CapabilityTypes.BROWSER_TYPE, BrowserType.CHROME)
                ?: conf.getEnum(CapabilityTypes.BROWSER_TYPE, BrowserType.CHROME)
    }
}
