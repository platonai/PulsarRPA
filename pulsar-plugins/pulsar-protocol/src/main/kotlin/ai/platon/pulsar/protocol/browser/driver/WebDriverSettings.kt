package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.common.config.ImmutableConfig
import org.openqa.selenium.UnexpectedAlertBehaviour
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.CapabilityType.SUPPORTS_JAVASCRIPT
import org.openqa.selenium.remote.DesiredCapabilities

/**
 * A general chrome option set:
 * Capabilities {
        acceptInsecureCerts: false,
        acceptSslCerts: false,
        applicationCacheEnabled: false,
        browserConnectionEnabled: false,
        browserName: chrome,
        chrome: {
            chromedriverVersion: 2.44.609551 (5d576e9a44fe4c...,
            userDataDir: /tmp/.org.chromium.Chromium...
        },
        cssSelectorsEnabled: true,
        databaseEnabled: false,
        goog:chromeOptions: {debuggerAddress: localhost:43001},
        handlesAlerts: true,
        hasTouchScreen: false,
        javascriptEnabled: true,
        locationContextEnabled: true,
        mobileEmulationEnabled: false,
        nativeEvents: true,
        networkConnectionEnabled: false,
        pageLoadStrategy: none,
        platform: LINUX,
        platformName: LINUX,
        rotatable: false,
        setWindowRect: true,
        takesHeapSnapshot: true,
        takesScreenshot: true,
        unexpectedAlertBehaviour: ignore,
        unhandledPromptBehavior: ignore,
        version: 69.0.3497.100,
        webStorageEnabled: true
    }
 * */
open class WebDriverSettings(
        parameters: Map<String, Any> = mapOf(),
        jsDirectory: String = "js",
        immutableConfig: ImmutableConfig
): BrowserSettings(parameters, jsDirectory, immutableConfig) {

    constructor(immutableConfig: ImmutableConfig): this(mapOf(), "js", immutableConfig)

    // Special
    // var mobileEmulationEnabled = true

    fun createGeneralOptions(): DesiredCapabilities {
        val generalOptions = DesiredCapabilities()

        generalOptions.setCapability(SUPPORTS_JAVASCRIPT, true)
        // generalOptions.setCapability(TAKES_SCREENSHOT, false)
        generalOptions.setCapability("downloadImages", imagesEnabled)
        // generalOptions.setCapability("browserLanguage", "zh_CN")
        generalOptions.setCapability("throwExceptionOnScriptError", false)
        // generalOptions.setCapability("resolution", "${viewPort.width}x${viewPort.height}")
        generalOptions.setCapability("pageLoadStrategy", pageLoadStrategy)

        return generalOptions
    }

    fun createChromeOptions(generalOptions: DesiredCapabilities = createGeneralOptions()): ChromeOptions {
        val chromeOptions = ChromeOptions()

        // see https://peter.sh/experiments/chromium-command-line-switches/
        chromeOptions.merge(generalOptions)
        chromeOptions.setHeadless(isHeadless)
        // chromeOptions.addArguments("--incognito") // may cause anti-spider
        chromeOptions.addArguments("--window-size=${formatViewPort()}")
        /**
         * TODO: seems not work, we might just use network.setBlockedURLs
         * */
        chromeOptions.addArguments(String.format("--blink-settings=imagesEnabled=%b", imagesEnabled))
        chromeOptions.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE)

        return chromeOptions
    }

    fun createChromeDevtoolsOptions(generalOptions: DesiredCapabilities): ChromeDevtoolsOptions {
        val chromeOptions = ChromeDevtoolsOptions()
        chromeOptions.merge(generalOptions.asMap())

        // rewrite proxy argument
        val proxy = generalOptions.getCapability(CapabilityType.PROXY)
        if (proxy is org.openqa.selenium.Proxy) {
            chromeOptions.removeArguments(CapabilityType.PROXY)
            chromeOptions.proxyServer = proxy.httpProxy
        }

        chromeOptions.userDataDir = userDataDir
        chromeOptions.headless = isHeadless
        chromeOptions.addArguments("window-size", formatViewPort())

        return chromeOptions
    }
}
