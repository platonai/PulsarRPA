package ai.platon.pulsar.browser.driver

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.google.gson.GsonBuilder
import java.awt.Dimension
import java.time.Duration
import kotlin.random.Random

open class BrowserControl(
        parameters: Map<String, Any> = mapOf(),
        var jsDirectory: String = "js",
        conf: ImmutableConfig = ImmutableConfig()
) {
    companion object {
        // required
        var viewPort = Dimension(1920, 1080)
    }

    var headless = conf.getBoolean(CapabilityTypes.BROWSER_DRIVER_HEADLESS, true)
    var imagesEnabled = conf.getBoolean(CapabilityTypes.BROWSER_IMAGES_ENABLED, false)
    var jsInvadingEnabled = conf.getBoolean(CapabilityTypes.BROWSER_JS_INVADING_ENABLED, true)

    // We will wait for document ready manually using javascript
    var pageLoadStrategy = "none"

    // The javascript to execute by Web browsers
    var propertyNames = conf.getTrimmedStrings(
            CapabilityTypes.FETCH_CLIENT_JS_COMPUTED_STYLES, AppConstants.CLIENT_JS_PROPERTY_NAMES)

    var pageLoadTimeout = conf.getDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, Duration.ofMinutes(3))
    var scriptTimeout = conf.getDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(60))
    var scrollDownCount = conf.getInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, 5)
    var scrollInterval = conf.getDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))

    var clientJsVersion = "0.2.3"

    val scripts = mutableMapOf<String, String>()

    // Available user agents
    val userAgents = mutableListOf<String>()

    private val jsParameters = mutableMapOf<String, Any>()
    private var mainJs = ""
    private var libJs = ""

    constructor(immutableConfig: ImmutableConfig): this(mapOf(), "js", immutableConfig)

    init {
        mapOf(
                "propertyNames" to propertyNames,
                "viewPortWidth" to viewPort.width,
                "viewPortHeight" to viewPort.height
        ).also { jsParameters.putAll(it) }

        jsParameters.putAll(parameters)

        generateUserAgents()

        loadDefaultResource()
    }

    fun formatViewPort(delimiter: String = ","): String {
        return "${viewPort.width}$delimiter${viewPort.height}"
    }

    // also see https://github.com/arouel/uadetector
    fun generateUserAgents() {
        if (userAgents.isNotEmpty()) return

        ResourceLoader.readAllLines("ua/chrome-user-agents-linux.txt")
                .filter { it.startsWith("Mozilla/5.0") }
                .toCollection(userAgents)
    }

    fun buildChromeUserAgent(
            mozilla: String = "5.0",
            appleWebKit: String = "537.36",
            chrome: String = "70.0.3538.77",
            safari: String = "537.36"
    ): String {
        return "Mozilla/$mozilla (X11; Linux x86_64) AppleWebKit/$appleWebKit (KHTML, like Gecko) Chrome/$chrome Safari/$safari"
    }

    fun parseLibJs(reload: Boolean = false): String {
        if (reload || libJs.isEmpty()) {
            // Note: Json-2.6.2 does not recognize MutableMap, but knows Map
            val configs = GsonBuilder().create().toJson(jsParameters.toMap())

            val sb = StringBuilder()
            sb.append(";\n")
            // set predefined variables shared between javascript and jvm program
            sb.appendln("let META_INFORMATION_ID = \"${AppConstants.PULSAR_META_INFORMATION_ID}\";")
            sb.appendln("let SCRIPT_SECTION_ID = \"${AppConstants.PULSAR_SCRIPT_SECTION_ID}\";")
            sb.appendln("let ATTR_HIDDEN = \"${AppConstants.PULSAR_ATTR_HIDDEN}\";")
            sb.appendln("let ATTR_OVERFLOW_HIDDEN = \"${AppConstants.PULSAR_ATTR_OVERFLOW_HIDDEN}\";")
            sb.appendln("let ATTR_OVERFLOW_VISIBLE = \"${AppConstants.PULSAR_ATTR_OVERFLOW_VISIBLE}\";")
            sb.appendln("let PULSAR_CONFIGS = $configs;")
            scripts.values.joinTo(sb, ";\n")
            libJs = sb.toString()
        }

        return libJs
    }

    fun parseJs(reload: Boolean = false): String {
        if (reload || mainJs.isEmpty()) {
            val sb = StringBuilder(parseLibJs(reload))
            sb.append(";\n__utils__.emulate();")
            sb.append(";\nreturn JSON.stringify(document.pulsarData);")
            mainJs = sb.toString()
        }

        return mainJs
    }

    fun randomUserAgent(): String {
        if (userAgents.isNotEmpty()) {
            return userAgents[Random.nextInt(userAgents.size)]
        }
        return ""
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
