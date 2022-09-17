package ai.platon.pulsar.browser.common

import ai.platon.pulsar.browser.common.BrowserSettings.Companion.screenViewport
import ai.platon.pulsar.browser.common.ScriptConfuser.Companion.scriptNamePrefix
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import com.google.gson.GsonBuilder
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.isReadable
import kotlin.io.path.listDirectoryEntries
import kotlin.random.Random

/**
 * The browser display mode
 * SUPERVISED: supervised by other programs
 * GUI: open as a normal browser
 * HEADLESS: open in headless mode
 * */
enum class DisplayMode { SUPERVISED, GUI, HEADLESS }

/**
 * The emulation settings
 * */
data class InteractSettings(
    var scrollCount: Int = 10,
    var scrollInterval: Duration = Duration.ofMillis(500),
    var scriptTimeout: Duration = Duration.ofMinutes(1),
    // TODO: use fetch task timeout instead
    var pageLoadTimeout: Duration = Duration.ofMinutes(3)
) {
    var delayPolicy: (String) -> Long = { type ->
        when (type) {
            "gap" -> 500L + Random.nextInt(500)
            "click" -> 500L + Random.nextInt(1000)
            "type" -> 50L + Random.nextInt(500)
            "mouseWheel" -> 800L + Random.nextInt(500)
            "dragAndDrop" -> 800L + Random.nextInt(500)
            "waitForNavigation" -> 500L
            "waitForSelector" -> 500L
            else -> 100L + Random.nextInt(500)
        }
    }

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

    fun overrideConfiguration(conf: MutableConfig) {
        conf.setInt(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        conf.setDuration(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        conf.setDuration(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }

    companion object {
        val DEFAULT = InteractSettings()

        var goodNetSettings = InteractSettings()

        var worseNetSettings = InteractSettings(
            scrollCount = 10,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofMinutes(2),
            Duration.ofMinutes(3),
        )

        var worstNetSettings = InteractSettings(
            scrollCount = 15,
            scrollInterval = Duration.ofSeconds(3),
            scriptTimeout = Duration.ofMinutes(3),
            Duration.ofMinutes(4),
        )
    }
}

open class BrowserSettings(
    parameters: Map<String, Any> = mapOf(),
    var jsDirectory: String = "js",
    val conf: ImmutableConfig = ImmutableConfig()
) {
    companion object {
        private val logger = getLogger(BrowserSettings::class)

        // The viewport size for browser to rendering all webpages
        @Deprecated("Use screenViewport instead", ReplaceWith("screenViewport"))
        var viewPort = AppConstants.DEFAULT_VIEW_PORT
        var screenViewport = AppConstants.DEFAULT_VIEW_PORT
        // Compression quality from range [0..100] (jpeg only) to capture screenshots
        var screenshotQuality = 50
        // Available user agents
        val userAgents = mutableListOf<String>()

        val jsParameters = mutableMapOf<String, Any>()
        val preloadJavaScriptResources = """
            stealth.js
            __pulsar_utils__.js
            configs.js
            node_ext.js
            node_traversor.js
            feature_calculator.js
        """.trimIndent().split("\n").map { "js/" + it.trim() }.toMutableList()
        private val preloadJavaScripts: MutableMap<String, String> = LinkedHashMap()

        private val confuser = ScriptConfuser()

        val isHeadlessOnly: Boolean get() = !AppContext.isGUIAvailable

        fun withBrowser(browserType: String): Companion {
            System.setProperty(BROWSER_TYPE, browserType)
            return BrowserSettings
        }

        fun withGoodNetwork(): Companion {
            return BrowserSettings
        }

        fun withWorseNetwork(): Companion {
            InteractSettings.worseNetSettings.toSystemProperties()
            return BrowserSettings
        }

        fun withWorstNetwork(): Companion {
            InteractSettings.worstNetSettings.toSystemProperties()
            return BrowserSettings
        }

        fun withGUI(): Companion {
            if (isHeadlessOnly) {
                logger.info("GUI is not available")
                return BrowserSettings
            }

            listOf(
                BROWSER_LAUNCH_SUPERVISOR_PROCESS,
                BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS
            ).forEach { System.clearProperty(it) }

            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.GUI.name)

            return BrowserSettings
        }

        fun headless(): Companion {
            listOf(
                BROWSER_LAUNCH_SUPERVISOR_PROCESS,
                BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS
            ).forEach { System.clearProperty(it) }

            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.HEADLESS.name)

            return BrowserSettings
        }

        fun supervised(): Companion {
            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.SUPERVISED.name)

            return BrowserSettings
        }

        /**
         * Single page application
         * */
        fun withSPA(): Companion {
            System.setProperty(FETCH_TASK_TIMEOUT, Duration.ofDays(1000).toString())
            System.setProperty(BROWSER_SPA_MODE, "true")
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

        fun enableUserAgentOverriding(): Companion {
            System.setProperty(BROWSER_ENABLE_UA_OVERRIDING, "true")
            return BrowserSettings
        }

        fun disableUserAgentOverriding(): Companion {
            System.setProperty(BROWSER_ENABLE_UA_OVERRIDING, "false")
            return BrowserSettings
        }

        // TODO: not implemented
        fun blockImages(): Companion {
            // enableUrlBlocking()
            return BrowserSettings
        }

        fun defaultUserDataDir() = AppPaths.CHROME_TMP_DIR

        fun randomUserAgent(): String {
            if (userAgents.isEmpty()) {
                loadUserAgents()
            }

            if (userAgents.isNotEmpty()) {
                return userAgents[Random.nextInt(userAgents.size)]
            }

            return ""
        }

        // also see https://github.com/arouel/uadetector
        fun loadUserAgents() {
            if (userAgents.isNotEmpty()) return

            var usa = ResourceLoader.readAllLines("ua/chrome-user-agents.txt")
                .filter { it.startsWith("Mozilla/5.0") }
            if (SystemUtils.IS_OS_LINUX) {
                usa = usa.filter { it.contains("X11") }
            } else if (SystemUtils.IS_OS_WINDOWS) {
                usa = usa.filter { it.contains("Windows") }
            }

            usa.toCollection(userAgents)
        }

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
    val isSPA get() = conf.getBoolean(BROWSER_SPA_MODE, false)

    val enableStartupScript get() = conf.getBoolean(BROWSER_JS_INVADING_ENABLED, true)
    val enableUrlBlocking get() = conf.getBoolean(BROWSER_ENABLE_URL_BLOCKING, false)
    /**
     * If user agent overriding is enabled. User agent overriding disabled by default,
     * since target websites can read the user agent and check specified browser features
     * to determine if they match or not.
     * */
    val enableUserAgentOverriding get() = conf.getBoolean(BROWSER_ENABLE_UA_OVERRIDING, false)

    // We will wait for document ready manually using javascript
    var pageLoadStrategy = "none"

    // The javascript to execute by Web browsers
    val propertyNames
        get() = conf.getTrimmedStrings(FETCH_CLIENT_JS_COMPUTED_STYLES, AppConstants.CLIENT_JS_PROPERTY_NAMES)

    /**
     * The js to inject to the browser
     * */
    var preloadJs = ""

    var interactSettings = InteractSettings.DEFAULT

    init {
        mapOf(
            "propertyNames" to propertyNames,
            "viewPortWidth" to screenViewport.width,
            "viewPortHeight" to screenViewport.height,

            "META_INFORMATION_ID" to AppConstants.PULSAR_META_INFORMATION_ID,
            "SCRIPT_SECTION_ID" to AppConstants.PULSAR_SCRIPT_SECTION_ID,
            "ATTR_HIDDEN" to AppConstants.PULSAR_ATTR_HIDDEN,
            "ATTR_OVERFLOW_HIDDEN" to AppConstants.PULSAR_ATTR_OVERFLOW_HIDDEN,
            "ATTR_OVERFLOW_VISIBLE" to AppConstants.PULSAR_ATTR_OVERFLOW_VISIBLE,
            "ATTR_ELEMENT_NODE_VI" to AppConstants.PULSAR_ATTR_ELEMENT_NODE_VI,
            "ATTR_TEXT_NODE_VI" to AppConstants.PULSAR_ATTR_TEXT_NODE_VI,
        ).also { jsParameters.putAll(it) }

        searchChromeBinaryPathAllAround()
    }

    open fun formatViewPort(delimiter: String = ","): String {
        return "${screenViewport.width}$delimiter${screenViewport.height}"
    }

    open fun randomUserAgentOrNull(): String? {
        return if (enableUserAgentOverriding) randomUserAgent() else null
    }

    /**
     * Make sure generatePreloadJs is thread safe
     * */
    @Synchronized
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

    open fun generatePredefinedJsConfig(): String {
        // Note: Json-2.6.2 does not recognize MutableMap, but knows Map
        val configs = GsonBuilder().create().toJson(jsParameters.toMap())

        // set predefined variables shared between javascript and jvm program
        val configVar = confuse( "${scriptNamePrefix}CONFIGS")
        return """
            ;
            let $configVar = $configs;
        """.trimIndent()
    }

    private fun loadDefaultResource() {
        preloadJavaScriptResources.associateWithTo(preloadJavaScripts) {
            ResourceLoader.readAllLines(it).joinToString("\n") { confuse(it) }
        }
    }

    /**
     * Confuse script
     * */
    open fun confuse(script: String): String = confuser.confuse(script)

    private fun loadJs() {
        val sb = StringBuilder()

        val jsVariables = generatePredefinedJsConfig()
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
        Files.createDirectories(dir)
        val report = Files.writeString(dir.resolve("preload.gen.js"), script)
        logger.info("Generated js: file://$report")
    }

    /**
     * Find BROWSER_CHROME_PATH in all config files
     * */
    private fun searchChromeBinaryPathAllAround() {
        val chromeBinaryPath = conf.get(BROWSER_CHROME_PATH)
        if (chromeBinaryPath != null) {
            val path = Paths.get(chromeBinaryPath).takeIf { Files.isExecutable(it) }?.toAbsolutePath()
            if (path != null) {
                System.setProperty(BROWSER_CHROME_PATH, chromeBinaryPath)
            }
        }
    }
}
