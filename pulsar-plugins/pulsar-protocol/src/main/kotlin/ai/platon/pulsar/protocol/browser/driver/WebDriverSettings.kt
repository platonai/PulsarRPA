package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeOptions
import ai.platon.pulsar.common.config.ImmutableConfig

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

    fun createGeneralOptions(): MutableMap<String, Any> {
        val generalOptions = mutableMapOf<String, Any>()

        // generalOptions.setCapability("browserLanguage", "zh_CN")
        // generalOptions.setCapability("resolution", "${viewPort.width}x${viewPort.height}")

        return generalOptions
    }

    fun createChromeOptions(generalOptions: Map<String, Any>): ChromeOptions {
        val chromeOptions = ChromeOptions()
        chromeOptions.merge(generalOptions)

        // rewrite proxy argument
        chromeOptions.removeArgument("proxy")
        chromeOptions.proxyServer = generalOptions["proxy"]?.toString()

        chromeOptions.headless = isHeadless
        chromeOptions.noSandbox = noSandbox

        chromeOptions.addArgument("window-size", formatViewPort())
            .addArgument("pageLoadStrategy", pageLoadStrategy)
            .addArgument("throwExceptionOnScriptError", "false")

        return chromeOptions
    }
}
