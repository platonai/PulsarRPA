package ai.platon.pulsar.common

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
        var jsDirectory: String = "js"
) {
    companion object {
        val log = LoggerFactory.getLogger(BrowserControl::class.java)!!
        var viewPort = Dimension(1920, 1080)

        var headless = true
        var imagesEnabled = false
        // var pageLoadStrategy = "normal"
        var pageLoadStrategy = "none"

        // var mobileEmulationEnabled = true
        //
    }

    val generalOptions = DesiredCapabilities()
    val chromeOptions = ChromeOptions()
    private val jsParameters = mutableMapOf<String, Any>()
    private var mainJs = ""
    private var libJs = ""
    val scripts = mutableMapOf<String, String>()

    init {
        generalOptions.setCapability(SUPPORTS_JAVASCRIPT, true)
        generalOptions.setCapability(TAKES_SCREENSHOT, false)
        generalOptions.setCapability("downloadImages", imagesEnabled)
        generalOptions.setCapability("browserLanguage", "zh_CN")
        generalOptions.setCapability("throwExceptionOnScriptError", false)
        generalOptions.setCapability("resolution", viewPort.width.toString() + "x" + viewPort.height)
        generalOptions.setCapability("pageLoadStrategy", pageLoadStrategy)

        // see https://peter.sh/experiments/chromium-command-line-switches/
        chromeOptions.merge(generalOptions)
        // Use headless mode by default, GUI mode can be used for debugging
        chromeOptions.setHeadless(headless)
        chromeOptions.addArguments("--disable-extensions")
        chromeOptions.addArguments("--window-size=" + viewPort.width + "," + viewPort.height)
        chromeOptions.addArguments(String.format("--blink-settings=imagesEnabled=%b", imagesEnabled))
        chromeOptions.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE)

        mapOf(
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

            val sb = StringBuilder(";\nlet PULSAR_CONFIGS = $configs;\n")
            scripts.values.joinTo(sb, ";\n")
            libJs = sb.toString()
        }

        return libJs
    }

    fun parseJs(reload: Boolean = false): String {
        if (reload || mainJs.isEmpty()) {
            val sb = StringBuilder(parseLibJs(reload))
            sb.append("\n")
                    .append(";\nwindow.stop();")
                    .append(";\n__utils__.scrollToBottom();")
                    .append(";\n__utils__.scrollToTop();")
                    .append(";\n__utils__.visualizeHumanize();")
                    .append(";\nwindow.stop();")
                    .append(";\n")

            mainJs = sb.toString()
        }

        return mainJs
    }

    private fun loadDefaultResource() {
        arrayOf(
                "__utils__.js",
                "node_ext.js",
                "node_traversor.js",
                "node_visitor.js",
                "humanize.js"
        ).associateTo(scripts) {
            it to ResourceLoader().readAllLines("$jsDirectory/$it").joinToString("\n") { it }
        }
    }
}
