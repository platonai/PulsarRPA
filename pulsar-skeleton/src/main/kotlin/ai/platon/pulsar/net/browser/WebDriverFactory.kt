package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.proxy.ProxyManager
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.devtools.log.Log
import org.openqa.selenium.devtools.network.Network
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.util.*

class WebDriverFactory(
        val driverControl: WebDriverControl,
        val proxyManager: ProxyManager,
        val conf: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(WebDriverFactory::class.java)
    private val defaultWebDriverClass = conf.getClass(
            CapabilityTypes.BROWSER_WEB_DRIVER_CLASS, ChromeDriver::class.java, RemoteWebDriver::class.java)

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    @Throws(NoSuchMethodException::class,
            IllegalAccessException::class,
            InvocationTargetException::class,
            InstantiationException::class
    )
    fun create(priority: Int, conf: ImmutableConfig): ManagedWebDriver {
        val capabilities = driverControl.createGeneralOptions()

        if (ProxyPool.isProxyEnabled()) {
            setProxy(capabilities)
        }

        // Choose the WebDriver
        val browserType = getBrowserType(conf)
        val driver: WebDriver = when {
            browserType == BrowserType.CHROME -> {
                val options = driverControl.createChromeDevtoolsOptions(capabilities)
                ChromeDevtoolsDriver(driverControl.randomUserAgent(), driverControl, options)
            }
            browserType == BrowserType.SELENIUM_CHROME -> {
                // System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");
                ChromeDriver(driverControl.createChromeOptions(capabilities))
            }
            RemoteWebDriver::class.java.isAssignableFrom(defaultWebDriverClass) -> {
                defaultWebDriverClass.getConstructor(Capabilities::class.java).newInstance(capabilities)
            }
            else -> defaultWebDriverClass.getConstructor().newInstance()
        }

        if (driver is ChromeDriver) {
            val fakeAgent = driverControl.randomUserAgent()
            val devTools = driver.devTools
            devTools.createSession()
            devTools.send(Log.enable())
            devTools.addListener(Log.entryAdded()) { e -> log.error(e.text) }
            devTools.send(Network.setUserAgentOverride(fakeAgent, Optional.empty(), Optional.empty()))

            // devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));
            // devTools.send(emulateNetworkConditions(false,100,200000,100000, Optional.of(ConnectionType.cellular4g)));
        }

        return ManagedWebDriver(driver, priority)
    }

    private fun setProxy(capabilities: DesiredCapabilities): ProxyEntry? {
        var proxyEntry: ProxyEntry? = null
        var hostPort: String? = null
        val proxy = org.openqa.selenium.Proxy()
        if (proxyManager.ensureOnline()) {
            // TODO: internal proxy server can be run at another host
            proxyEntry = proxyManager.currentProxyEntry
            hostPort = "127.0.0.1:${proxyManager.port}"
        }

        if (hostPort == null) {
            // internal proxy server is not available, set proxy to the browser directly
            proxyEntry = proxyManager.proxyPool.poll()
            hostPort = proxyEntry?.hostPort
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
