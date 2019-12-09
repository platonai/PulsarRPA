package ai.platon.pulsar.common

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CLIENT_JS_COMPUTED_STYLES
import ai.platon.pulsar.common.config.ImmutableConfig
import com.google.gson.GsonBuilder
import org.openqa.selenium.UnexpectedAlertBehaviour
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType.SUPPORTS_JAVASCRIPT
import org.openqa.selenium.remote.CapabilityType.TAKES_SCREENSHOT
import org.openqa.selenium.remote.DesiredCapabilities
import org.slf4j.LoggerFactory
import java.awt.Dimension

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
open class BrowserControl(
        parameters: Map<String, Any> = mapOf(),
        var jsDirectory: String = "js",
        immutableConfig: ImmutableConfig
) {
    companion object {
        val log = LoggerFactory.getLogger(BrowserControl::class.java)!!
        var viewPort = Dimension(1920, 1080)

        var headless = true
        // There are too many exceptions if images are disabled
        var imagesEnabled = true
        // We will wait for document ready manually using javascript
        var pageLoadStrategy = "none"

        // Special
        // var mobileEmulationEnabled = true

        fun createGeneralOptions(): DesiredCapabilities {
            val generalOptions = DesiredCapabilities()

            generalOptions.setCapability(SUPPORTS_JAVASCRIPT, true)
            generalOptions.setCapability(TAKES_SCREENSHOT, false)
            generalOptions.setCapability("downloadImages", imagesEnabled)
            generalOptions.setCapability("browserLanguage", "zh_CN")
            generalOptions.setCapability("throwExceptionOnScriptError", false)
            generalOptions.setCapability("resolution", viewPort.width.toString() + "x" + viewPort.height)
            generalOptions.setCapability("pageLoadStrategy", pageLoadStrategy)

            return generalOptions
        }

        fun createChromeOptions(generalOptions: DesiredCapabilities = createGeneralOptions()): ChromeOptions {
            val chromeOptions = ChromeOptions()

            // see https://peter.sh/experiments/chromium-command-line-switches/
            chromeOptions.merge(generalOptions)
            // Use headless mode by default, GUI mode can be used for debugging
            chromeOptions.setHeadless(headless)
            // some web sites have technology to detect whether the browser is controlled by web driver
            // chromeOptions.addArguments("disable-infobars")
            chromeOptions.addArguments("--disable-extensions")
            // chromeOptions.addArguments("--incognito") // may cause anti-spider
            chromeOptions.addArguments("--window-size=" + viewPort.width + "," + viewPort.height)
            chromeOptions.addArguments(String.format("--blink-settings=imagesEnabled=%b", imagesEnabled))
            chromeOptions.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE)

            return chromeOptions
        }
    }

    constructor(immutableConfig: ImmutableConfig): this(mapOf(), "js", immutableConfig)

    private val jsParameters = mutableMapOf<String, Any>()
    private var mainJs = ""
    private var libJs = ""
    val scripts = mutableMapOf<String, String>()

    init {
        // The javascript to execute by Web browsers
        val propertyNames = immutableConfig.getTrimmedStrings(
                FETCH_CLIENT_JS_COMPUTED_STYLES, CLIENT_JS_PROPERTY_NAMES)

        mapOf(
                "version" to PulsarEnv.clientJsVersion,
                "propertyNames" to propertyNames,
                "viewPortWidth" to viewPort.width,
                "viewPortHeight" to viewPort.height
        ).also { jsParameters.putAll(it) }

        jsParameters.putAll(parameters)

        loadDefaultResource()
    }

    fun parseLibJs(reload: Boolean = false): String {
        if (reload || libJs.isEmpty()) {
            // Note: Json-2.6.2 does not recognize MutableMap, but knows Map
            val configs = GsonBuilder().create().toJson(jsParameters.toMap())

            val sb = StringBuilder()
            sb.append(";\n")
            // set predefined variables shared between javascript and jvm program
            sb.appendln("let META_INFORMATION_ID = \"$PULSAR_META_INFORMATION_ID\";")
            sb.appendln("let SCRIPT_SECTION_ID = \"$PULSAR_SCRIPT_SECTION_ID\";")
            sb.appendln("let ATTR_HIDDEN = \"$PULSAR_ATTR_HIDDEN\";")
            sb.appendln("let ATTR_OVERFLOW_HIDDEN = \"$PULSAR_ATTR_OVERFLOW_HIDDEN\";")
            sb.appendln("let ATTR_OVERFLOW_VISIBLE = \"$PULSAR_ATTR_OVERFLOW_VISIBLE\";")
            sb.appendln("let PULSAR_CONFIGS = $configs;")
            scripts.values.joinTo(sb, ";\n")
            libJs = sb.toString()
        }

        return libJs
    }

    fun parseJs(reload: Boolean = false): String {
        if (reload || mainJs.isEmpty()) {
            val sb = StringBuilder(parseLibJs(reload))
            sb.append(";\n__utils__.visualizeHumanize();")
            sb.append(";\nreturn JSON.stringify(document.pulsarData);")
            mainJs = sb.toString()
        }

        return mainJs
    }

    private fun loadDefaultResource() {
        arrayOf(
                "configs.js",
                "__utils__.js",
                "node_ext.js",
                "node_traversor.js",
                "feature_calculator.js",
                "humanize.js"
        ).associateTo(scripts) {
            it to ResourceLoader.readAllLines("$jsDirectory/$it").joinToString("\n") { it }
        }
    }
}
