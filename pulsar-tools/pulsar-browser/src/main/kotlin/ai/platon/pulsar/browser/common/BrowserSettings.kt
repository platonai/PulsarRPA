package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.random.Random

/**
 * The [BrowserSettings] class defines a convenient interface to control the behavior of browsers.
 * */
open class BrowserSettings(
    val conf: ImmutableConfig = ImmutableConfig()
) {
    companion object {
        // The viewport size for browser to rendering all webpages
        var screenViewport = AppConstants.DEFAULT_VIEW_PORT
        // Compression quality from range [0..100] (jpeg only) to capture screenshots
        var screenshotQuality = 50

        /**
         * Check if the current environment supports only headless mode.
         * */
        val isHeadlessOnly: Boolean get() = !AppContext.isGUIAvailable

        /**
         * Specify the browser type for all fetches.
         * */
        @JvmStatic
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
        @JvmStatic
        fun withGoodNetwork(): Companion {
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to different network conditions
         * to obtain the best data quality and data collection speed.
         * */
        @JvmStatic
        fun withWorseNetwork(): Companion {
            InteractSettings.worseNetSettings.overrideSystemProperties()
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to different network conditions
         * to obtain the best data quality and data collection speed.
         * */
        @JvmStatic
        fun withWorstNetwork(): Companion {
            InteractSettings.worstNetSettings.overrideSystemProperties()
            return BrowserSettings
        }

        /**
         * Launch the browser in GUI mode.
         * */
        @JvmStatic
        fun withGUI(): Companion {
            if (isHeadlessOnly) {
                System.err.println("GUI is not available")
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
        @JvmStatic
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
        @JvmStatic
        fun supervised(): Companion {
            System.setProperty(BROWSER_DISPLAY_MODE, DisplayMode.SUPERVISED.name)

            return BrowserSettings
        }

        /**
         * Tell the system to work with single page application.
         * To collect SPA data, the execution needs to have no timeout limit.
         * */
        @JvmStatic
        fun withSPA(): Companion {
            System.setProperty(FETCH_TASK_TIMEOUT, Duration.ofDays(1000).toString())
            System.setProperty(BROWSER_SPA_MODE, "true")
            return BrowserSettings
        }

        /**
         * Enable url blocking. If url blocking is enabled and the blocking rules are set,
         * resources matching the rules will be blocked by the browser.
         * */
        @JvmStatic
        fun enableUrlBlocking(): Companion {
            System.setProperty(BROWSER_ENABLE_URL_BLOCKING, "true")
            return BrowserSettings
        }

        /**
         * Disable url blocking. If url blocking is disabled, blocking rules are ignored.
         * */
        @JvmStatic
        fun disableUrlBlocking(): Companion {
            System.setProperty(BROWSER_ENABLE_URL_BLOCKING, "false")
            return BrowserSettings
        }

        /**
         * Block all images.
         * */
        @JvmStatic
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
        @JvmStatic
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
        @JvmStatic
        fun disableUserAgentOverriding(): Companion {
            System.setProperty(BROWSER_ENABLE_UA_OVERRIDING, "false")
            return BrowserSettings
        }

        @JvmStatic
        fun enableProxy(): Companion {
            ProxyPoolManager.enableProxy()
            return this
        }

        @JvmStatic
        fun disableProxy(): Companion {
            ProxyPoolManager.disableProxy()
            return this
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
     * Check if it's SPA mode, SPA stands for single page application.
     *
     * If pulsar works in SPA mode:
     * 1. execution of fetches has no timeout limit
     * */
    val isSPA get() = conf.getBoolean(BROWSER_SPA_MODE, false)
    /**
     * Check if startup scripts are allowed. If true, pulsar injects scripts into the browser
     * before loading a page, and custom scripts are also allowed.
     * */
    val isStartupScriptEnabled get() = conf.getBoolean(BROWSER_JS_INVADING_ENABLED, true)
    /**
     * Check if url blocking is enabled.
     * If true and blocking rules are set, resources matching the rules will be blocked by the browser.
     * */
    val isUrlBlockingEnabled get() = conf.getBoolean(BROWSER_ENABLE_URL_BLOCKING, false)
    /**
     * Check if user agent overriding is enabled. User agent overriding disabled by default,
     * since inappropriate user agent overriding will be detected by the target website and
     * the visits will be blocked.
     * */
    val isUserAgentOverridingEnabled get() = conf.getBoolean(BROWSER_ENABLE_UA_OVERRIDING, false)

    /**
     * Page load strategy.
     *
     * Pulsar checks document ready using javascript so just set the strategy to be none.
     *
     * @see <a href='https://blog.knoldus.com/page-loading-strategy-in-the-selenium-webdriver/'>Page Loading Strategy</a>
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

    val userAgent = UserAgent()
    val confuser = ScriptConfuser()
    val scriptLoader = ScriptLoader(confuser, conf)
}

/**
 * The browser display mode.
 *
 * Three display modes are supported:
 * 1. GUI: open as a normal browser
 * 2. HEADLESS: open in headless mode
 * 3. SUPERVISED: supervised by other programs
 * */
enum class DisplayMode { SUPERVISED, GUI, HEADLESS }

/**
 * The interaction settings
 * */
data class InteractSettings(
    var scrollCount: Int = 10,
    var scrollInterval: Duration = Duration.ofMillis(500),
    var scriptTimeout: Duration = Duration.ofMinutes(1),
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

    fun overrideSystemProperties() {
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
