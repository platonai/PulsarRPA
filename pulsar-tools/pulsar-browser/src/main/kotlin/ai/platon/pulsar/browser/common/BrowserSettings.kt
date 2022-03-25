package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import com.github.kklisura.cdt.protocol.types.network.ResourceType
import com.google.gson.GsonBuilder
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.isReadable
import kotlin.io.path.listDirectoryEntries
import kotlin.random.Random


/**
 * The chrome display mode
 * SUPERVISED: supervised by other programs
 * GUI: open as a normal browser
 * HEADLESS: open in headless mode
 * */
enum class DisplayMode { SUPERVISED, GUI, HEADLESS }

/**
 * The emulation settings
 * */
data class EmulateSettings(
    var scrollCount: Int = 10,
    var scrollInterval: Duration = Duration.ofMillis(500),
    var scriptTimeout: Duration = Duration.ofMinutes(1),
    var pageLoadTimeout: Duration = Duration.ofMinutes(3)
) {
    constructor(conf: ImmutableConfig) : this(
        scrollCount = conf.getInt(FETCH_SCROLL_DOWN_COUNT, 5),
        scrollInterval = conf.getDuration(FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500)),
        scriptTimeout = conf.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofMinutes(1)),
        pageLoadTimeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofMinutes(3)),
    )

    fun toSystemProperties() {
        Systems.setProperty(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        Systems.setProperty(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        Systems.setProperty(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        Systems.setProperty(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }

    fun toConf(conf: MutableConfig) {
        conf.setInt(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        conf.setDuration(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        conf.setDuration(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }

    companion object {
        val DEFAULT = EmulateSettings()

        var goodNetSettings = EmulateSettings()

        var worseNetSettings = EmulateSettings(
            scrollCount = 10,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofMinutes(2),
            Duration.ofMinutes(3),
        )

        var worstNetSettings = EmulateSettings(
            scrollCount = 15,
            scrollInterval = Duration.ofSeconds(3),
            scriptTimeout = Duration.ofMinutes(3),
            Duration.ofMinutes(4),
        )
    }
}

/**
 * The block rules of urls and resources
 * */
open class BlockRules {

    open val blockingResourceTypes: MutableList<ResourceType>
        get() = listOf(ResourceType.IMAGE, ResourceType.MEDIA, ResourceType.FONT).toMutableList()

    /**
     * amazon.com note:
     * The following have to pass, or the site refuses to serve:
     * .woff,
     * .mp4
     * */
    open val mustPassUrls: MutableList<String>
        get() = mutableListOf()

    /**
     * Blocking urls patten using widcards
     * */
    open val blockingUrls: MutableList<String>
        get() = listOf(
            "*.png", "*.jpg", "*.jpeg", "*.gif", "*.ico", "*.webp",
            "*.woff", "*.woff2",
            "*.mp4", "*.svg",
            "*.png?*", "*.jpg?*", "*.gif?*", "*.ico?*", "*.webp?*",
            "https://img*"
        ).filterNot { it in mustPassUrls }.toMutableList()

    open val mustPassUrlPatterns: MutableList<Regex>
        get() = listOf(
            "about:blank",
            "data:.+",
        ).map { it.toRegex() }.union(mustPassUrls.map { Wildchar(it).toRegex() }).toMutableList()

    open val blockingUrlPatterns: MutableList<Regex>
        get() = blockingUrls.map { Wildchar(it).toRegex() }.toMutableList()
}

open class BrowserSettings(
    parameters: Map<String, Any> = mapOf(),
    var jsDirectory: String = "js",
    val conf: ImmutableConfig = ImmutableConfig()
) {
    companion object {
        private val logger = getLogger(BrowserSettings::class)

        // required
        var viewPort = AppConstants.DEFAULT_VIEW_PORT

        val preloadJavaScriptResources = """
            stealth.js
            __pulsar_utils__.js
            configs.js
            node_ext.js
            node_traversor.js
        """.trimIndent().split("\n").map { "js/" + it.trim() }.toMutableList()
        val preloadJavaScripts: MutableMap<String, String> = LinkedHashMap()
        val jsParameters = mutableMapOf<String, Any>()

        val isHeadlessOnly: Boolean get() = !AppContext.isGUIAvailable

        fun withGoodNetwork(): Companion {
            return BrowserSettings
        }

        fun withWorseNetwork(): Companion {
            EmulateSettings.worseNetSettings.toSystemProperties()
            return BrowserSettings
        }

        fun withWorstNetwork(): Companion {
            EmulateSettings.worstNetSettings.toSystemProperties()
            return BrowserSettings
        }

        fun withGUI(): Companion {
            if (isHeadlessOnly) {
                logger.info("GUI is not available")
                return BrowserSettings
            }

            arrayOf(BROWSER_LAUNCH_SUPERVISOR_PROCESS, BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS).forEach {
                System.clearProperty(it)
            }
            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.GUI.name)
            return BrowserSettings
        }

        fun enableUrlBlocking(): Companion {
            System.setProperty(BROWSER_ENABLE_URL_BLOCKING, "true")
            return BrowserSettings
        }

        fun disableUrlBlocking(): Companion {
            System.setProperty(BROWSER_ENABLE_URL_BLOCKING, "false")
            return BrowserSettings
        }

        // TODO: not implemented
        fun blockImages(): Companion {
            // enableUrlBlocking()
            return BrowserSettings
        }

        fun defaultUserDataDir() = AppPaths.CHROME_TMP_DIR

        fun generateUserDataDir(): Path {
            val numInstances = Files.list(AppPaths.BROWSER_TMP_DIR).filter { Files.isDirectory(it) }.count().inc()
            val rand = Random.nextInt(0, 1000000).toString(Character.MAX_RADIX)
            return AppPaths.BROWSER_TMP_DIR.resolve("br.$numInstances$rand")
        }
    }

    val supervisorProcess get() = conf.get(BROWSER_LAUNCH_SUPERVISOR_PROCESS)
    val supervisorProcessArgs get() = conf.getTrimmedStringCollection(BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS)

    /**
     * Chrome has to run without sandbox in a virtual machine
     * */
    val forceNoSandbox get() = AppContext.OS_IS_WSL

    /**
     * Add a --no-sandbox flag to launch the chrome if we are running inside a virtual machine,
     * for example, virtualbox, vmware or WSL
     * */
    val noSandbox get() = forceNoSandbox || conf.getBoolean(BROWSER_LAUNCH_NO_SANDBOX, true)

    val displayMode
        get() = if (isHeadlessOnly) DisplayMode.HEADLESS
        else conf.getEnum(BROWSER_DISPLAY_MODE, DisplayMode.GUI)

    val isSupervised get() = supervisorProcess != null && displayMode == DisplayMode.SUPERVISED
    val isHeadless get() = displayMode == DisplayMode.HEADLESS
    val isGUI get() = displayMode == DisplayMode.GUI

    val jsInvadingEnabled get() = conf.getBoolean(BROWSER_JS_INVADING_ENABLED, true)
    val userDataDir get() = conf.getPathOrNull(BROWSER_DATA_DIR) ?: generateUserDataDir()
    val enableUrlBlocking get() = conf.getBoolean(BROWSER_ENABLE_URL_BLOCKING, false)

    // We will wait for document ready manually using javascript
    var pageLoadStrategy = "none"

    // The javascript to execute by Web browsers
    val propertyNames
        get() = conf.getTrimmedStrings(FETCH_CLIENT_JS_COMPUTED_STYLES, AppConstants.CLIENT_JS_PROPERTY_NAMES)

    var clientJsVersion = "0.2.3"

    // Available user agents
    val userAgents = mutableListOf<String>()

    /**
     * The js to inject to the browser
     * */
    protected var preloadJs = ""

    init {
        mapOf(
            "propertyNames" to propertyNames,
            "viewPortWidth" to viewPort.width,
            "viewPortHeight" to viewPort.height
        ).also { jsParameters.putAll(it) }
    }

    open fun formatViewPort(delimiter: String = ","): String {
        return "${viewPort.width}$delimiter${viewPort.height}"
    }

    open fun generatePreloadJs(reload: Boolean = false): String {
        if (reload) {
            preloadJavaScripts.clear()
            preloadJs = ""
        }

        if (preloadJs.isEmpty()) {
            loadJs()
        }

        return preloadJs
    }

    open fun randomUserAgent(): String {
        if (userAgents.isEmpty()) {
            generateUserAgents()
        }

        if (userAgents.isNotEmpty()) {
            return userAgents[Random.nextInt(userAgents.size)]
        }

        return ""
    }

    // also see https://github.com/arouel/uadetector
    open fun generateUserAgents() {
        if (userAgents.isNotEmpty()) return

        ResourceLoader.readAllLines("ua/chrome-user-agents-linux.txt")
            .filter { it.startsWith("Mozilla/5.0") }
            .toCollection(userAgents)
    }

    open fun buildChromeUserAgent(
        mozilla: String = "5.0",
        appleWebKit: String = "537.36",
        chrome: String = "70.0.3538.77",
        safari: String = "537.36"
    ): String {
        return "Mozilla/$mozilla (X11; Linux x86_64) AppleWebKit/$appleWebKit (KHTML, like Gecko) Chrome/$chrome Safari/$safari"
    }

    open fun generatePredefinedJsVariables(): String {
        // Note: Json-2.6.2 does not recognize MutableMap, but knows Map
        val configs = GsonBuilder().create().toJson(jsParameters.toMap())

        // set predefined variables shared between javascript and jvm program
        return """
            ;
            let META_INFORMATION_ID = "${AppConstants.PULSAR_META_INFORMATION_ID}";
            let SCRIPT_SECTION_ID = "${AppConstants.PULSAR_SCRIPT_SECTION_ID}";
            let ATTR_HIDDEN = "${AppConstants.PULSAR_ATTR_HIDDEN}";
            let ATTR_OVERFLOW_HIDDEN = "${AppConstants.PULSAR_ATTR_OVERFLOW_HIDDEN}";
            let ATTR_OVERFLOW_VISIBLE = "${AppConstants.PULSAR_ATTR_OVERFLOW_VISIBLE}";
            let PULSAR_CONFIGS = $configs;
        """.trimIndent()
    }

    open fun loadDefaultResource() {
        preloadJavaScriptResources.associateWithTo(preloadJavaScripts) {
            ResourceLoader.readAllLines(it).joinToString("\n") { it }
        }
    }

    private fun loadJs() {
        val sb = StringBuilder()

        val jsVariables = generatePredefinedJsVariables()
        sb.appendLine(jsVariables).appendLine("\n\n\n")

        loadExternalResource()
        loadDefaultResource()
        preloadJavaScripts.values.joinTo(sb, ";\n")

        preloadJs = sb.toString()
        reportPreloadJs(preloadJs)
    }

    private fun loadExternalResource() {
        val dir = AppPaths.BROWSER_DATA_DIR.resolve("browser/js/preload")
        if (Files.isDirectory(dir)) {
            dir.listDirectoryEntries()
                .filter { it.isReadable() }
                .filter { it.toString().endsWith(".js") }
                .associateTo(preloadJavaScripts) { it.toString() to Files.readString(it) }
        }
    }

    private fun reportPreloadJs(script: String) {
        val dir = AppPaths.REPORT_DIR.resolve("browser/js")
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        val report = Files.writeString(dir.resolve("preload.js"), script)
        logger.info("Generated js: file://$report")
    }
}
