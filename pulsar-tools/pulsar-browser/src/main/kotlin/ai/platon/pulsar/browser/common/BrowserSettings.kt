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

        val confuser = ScriptConfuser()

        /**
         * Check if the current environment supports only headless mode.
         * */
        val isHeadlessOnly: Boolean get() = !AppContext.isGUIAvailable

        /**
         * Specify the browser type for all fetches.
         * */
        fun withBrowser(browserType: String): Companion {
            System.setProperty(BROWSER_TYPE, browserType)
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to different network conditions
         * to obtain the best data quality and data collection speed.
         * */
        fun withGoodNetwork(): Companion {
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to different network conditions
         * to obtain the best data quality and data collection speed.
         * */
        fun withWorseNetwork(): Companion {
            InteractSettings.worseNetSettings.toSystemProperties()
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to different network conditions
         * to obtain the best data quality and data collection speed.
         * */
        fun withWorstNetwork(): Companion {
            InteractSettings.worstNetSettings.toSystemProperties()
            return BrowserSettings
        }

        /**
         * Launch the browser in GUI mode.
         * */
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

        /**
         * Launch the browser in headless mode.
         * */
        fun headless(): Companion {
            listOf(
                BROWSER_LAUNCH_SUPERVISOR_PROCESS,
                BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS
            ).forEach { System.clearProperty(it) }

            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.HEADLESS.name)

            return BrowserSettings
        }

        /**
         * Launch the browser in supervised mode.
         * */
        fun supervised(): Companion {
            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.SUPERVISED.name)

            return BrowserSettings
        }

        /**
         * Tell the system to work with single page application.
         * To collect SPA data, the execution needs to have no timeout limit.
         * */
        fun withSPA(): Companion {
            System.setProperty(FETCH_TASK_TIMEOUT, Duration.ofDays(1000).toString())
            System.setProperty(BROWSER_SPA_MODE, "true")
            return BrowserSettings
        }

        /**
         * Enable url blocking. If url blocking is enabled and the blocking rules are set,
         * resources matching the rules will be blocked by the browser.
         * */
        fun enableUrlBlocking(): Companion {
            System.setProperty(BROWSER_ENABLE_URL_BLOCKING, "true")
            return BrowserSettings
        }

        /**
         * Disable url blocking. If url blocking is disabled, blocking rules are ignored.
         * */
        fun disableUrlBlocking(): Companion {
            System.setProperty(BROWSER_ENABLE_URL_BLOCKING, "false")
            return BrowserSettings
        }

        /**
         * Block all images.
         * TODO: not implemented
         * */
        fun blockImages(): Companion {
            // enableUrlBlocking()
            return BrowserSettings
        }

        /**
         * Enable user agent overriding.
         *
         * Inappropriate user agent overriding will be detected by the target website and
         * the visits will be blocked.
         * */
        fun enableUserAgentOverriding(): Companion {
            System.setProperty(BROWSER_ENABLE_UA_OVERRIDING, "true")
            return BrowserSettings
        }

        /**
         * Disable user agent overriding.
         *
         * Inappropriate user agent overriding will be detected by the target website and
         * the visits will be blocked.
         * */
        fun disableUserAgentOverriding(): Companion {
            System.setProperty(BROWSER_ENABLE_UA_OVERRIDING, "false")
            return BrowserSettings
        }

        /**
         * Generate a random user agent
         * */
        fun randomUserAgent(): String {
            if (userAgents.isEmpty()) {
                loadUserAgents()
            }

            if (userAgents.isNotEmpty()) {
                return userAgents[Random.nextInt(userAgents.size)]
            }

            return ""
        }

        /**
         * Generate a random user agent,
         * also see <a href='https://github.com/arouel/uadetector'>uadetector</a>
         * */
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

        /**
         * Generate a user data directory.
         * */
        fun generateUserDataDir(): Path {
            val numInstances = Files.list(AppPaths.BROWSER_TMP_DIR).filter { Files.isDirectory(it) }.count().inc()
            val rand = Random.nextInt(0, 1000000).toString(Character.MAX_RADIX)
            return AppPaths.BROWSER_TMP_DIR.resolve("br.$numInstances$rand")
        }
    }

    /**
     * The supervisor process
     * */
    val supervisorProcess get() = conf.get(BROWSER_LAUNCH_SUPERVISOR_PROCESS)
    /**
     * The supervisor process arguments
     * */
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

    /**
     * The browser's display mode, can be one of:
     * headless,
     * GUI,
     * supervised
     * */
    val displayMode
        get() = if (isHeadlessOnly) DisplayMode.HEADLESS
        else conf.getEnum(BROWSER_DISPLAY_MODE, DisplayMode.GUI)

    /**
     * If true, the browser will run in supervised mode.
     * */
    val isSupervised get() = supervisorProcess != null && displayMode == DisplayMode.SUPERVISED
    /**
     * If true, the browser will run in headless mode.
     * */
    val isHeadless get() = displayMode == DisplayMode.HEADLESS
    /**
     * If true, the browser will run in GUI mode as normal.
     * */
    val isGUI get() = displayMode == DisplayMode.GUI
    /**
     * If true, the system will work with a single page application and the
     * execution of fetches has no timeout limit.
     * */
    val isSPA get() = conf.getBoolean(BROWSER_SPA_MODE, false)
    /**
     * If true, the system injects scripts into the browser before loading a page.
     * */
    val enableStartupScript get() = conf.getBoolean(BROWSER_JS_INVADING_ENABLED, true)
    /**
     * If true and blocking rules are set, resources matching the rules will be blocked by the browser.
     * */
    val enableUrlBlocking get() = conf.getBoolean(BROWSER_ENABLE_URL_BLOCKING, false)
    /**
     * If user agent overriding is enabled. User agent overriding disabled by default,
     * since inappropriate user agent overriding will be detected by the target website and
     * the visits will be blocked.
     * */
    val enableUserAgentOverriding get() = conf.getBoolean(BROWSER_ENABLE_UA_OVERRIDING, false)

    /**
     * Page load strategy.
     *
     * The system checks document ready using javascript so just set the strategy to be none.
     * */
    var pageLoadStrategy = "none"

    /**
     * The javascript code to inject into the browser.
     * */
    var preloadJs = ""

    /**
     * The interaction settings. Interaction settings define how the system
     * interacts with webpages to mimic the behavior of real people.
     * */
    var interactSettings = InteractSettings.DEFAULT

    val scriptLoader = ScriptLoader(confuser, jsParameters, conf)

    open fun formatViewPort(delimiter: String = ","): String {
        return "${screenViewport.width}$delimiter${screenViewport.height}"
    }

    open fun randomUserAgentOrNull(): String? {
        return if (enableUserAgentOverriding) randomUserAgent() else null
    }

    /**
     * Confuse script
     * */
    open fun confuse(script: String): String = confuser.confuse(script)
}
