package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.protocol.browser.impl.BrowserManager
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLaunchException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

/**
 * A factory to create WebDriver.
 */
open class WebDriverFactory constructor(
    val browserManager: BrowserManager,
    private val config: ImmutableConfig,
) {
    private val logger = LoggerFactory.getLogger(WebDriverFactory::class.java)

    /**
     * The number of drivers created.
     */
    private val numDrivers = AtomicInteger()

    /**
     * Launch a browser.
     */
    @Deprecated("Use BrowserFactory.launchBrowser() instead", ReplaceWith("browserFactory.launchBrowser(browserId, conf)"))
    @Throws(BrowserLaunchException::class)
    fun launchBrowser(browserId: BrowserId, conf: MutableConfig = config.toMutableConfig()): Browser {
        numDrivers.incrementAndGet()

        logger.debug("Creating browser #{} | {}", numDrivers, browserId)

        val browserType = browserId.browserType
        val driverSettings = BrowserSettings(conf)
        val capabilities = driverSettings.createGeneralOptions()
        setProxy(capabilities, browserId.fingerprint.proxyURI)

        return browserManager.launch(browserId, driverSettings, capabilities)
    }

    private fun setProxy(capabilities: MutableMap<String, Any>, proxyURI: URI?) {
        if (proxyURI == null) {
            return
        }

        capabilities["proxy"] = proxyURI
    }








    /********************************************************************************
     * The code below are deprecated and will be removed in the future.
     * ******************************************************************************/








    /**
     * Create a WebDriver.
     */
    @Deprecated("Never used")
    @Throws(BrowserLaunchException::class)
    fun create(start: Boolean = true) = create(config.toVolatileConfig(), start)
    
    /**
     * Create a WebDriver.
     */
    @Deprecated("Never used")
    @Throws(BrowserLaunchException::class)
    fun create(conf: VolatileConfig, start: Boolean = true) = create(BrowserId.RANDOM_TEMP, 0, conf, start)
    
    /**
     * Create a WebDriver.
     */
    @Deprecated("Never used")
    @Throws(BrowserLaunchException::class)
    fun create(
        browserId: BrowserId,
        priority: Int,
        conf: VolatileConfig,
        start: Boolean
    ) = launchBrowserAndDriver(browserId, priority, conf, start).second

    /**
     * Launch a browser with a random fingerprint.
     */
    @Deprecated("Use BrowserFactory.launchRandomTempBrowser() instead", ReplaceWith("browserFactory.launchRandomTempBrowser()"))
    @Throws(BrowserLaunchException::class)
    fun launchTempBrowser() = launchBrowser(BrowserId.RANDOM_TEMP)

    /**
     * Launch a [Browser] with a [WebDriver].
     */
    @Deprecated("Never used")
    @Throws(BrowserLaunchException::class)
    private fun launchBrowserAndDriver(
        browserId: BrowserId, priority: Int, conf: VolatileConfig, start: Boolean = true
    ): Pair<Browser, WebDriver> {
        val browser = launchBrowser(browserId, conf)
        val driver = browser.newDriver() as AbstractWebDriver

        if (start) {
            driver.startWork()
        }

        return browser to driver
    }
}
