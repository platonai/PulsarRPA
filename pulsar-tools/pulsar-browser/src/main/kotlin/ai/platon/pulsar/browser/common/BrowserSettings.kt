package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.annotation.JsonIgnore
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
         * The interaction settings. Interaction settings define how the system
         * interacts with webpages to mimic the behavior of real people.
         * */
        var interactSettings = InteractSettings.DEFAULT
        
        /**
         * Check if the current environment supports only headless mode.
         * TODO: this doesn't work on some platform
         * */
        val isHeadlessOnly: Boolean get() = !AppContext.isGUIAvailable

        /**
         * Specify the browser type to fetch webpages.
         * */
        @Deprecated("Inappropriate name", ReplaceWith("withBrowser(browserType)"))
        @JvmStatic
        fun withBrowser(browserType: String): Companion {
            System.setProperty(BROWSER_TYPE, browserType)
            return BrowserSettings
        }

        /**
         * Specify the browser type to fetch webpages.
         *
         * PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withBrowser(browserType: BrowserType): Companion {
            System.setProperty(BROWSER_TYPE, browserType.name)
            return BrowserSettings
        }

        /**
         * Use google-chrome with the default environment, so PulsarRPA visits websites just like you do.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser() = withSystemDefaultBrowser(BrowserType.PULSAR_CHROME)

        /**
         * Use the specified browser with the default environment, so PulsarRPA visits websites just like you do.
         * PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.crawl.fetch.privacy.SystemDefaultPrivacyContextIdGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS, clazz)
            withBrowser(browserType)
            return BrowserSettings
        }

        /**
         * Use google-chrome with the prototype environment, any change to the browser will be kept.
         * */
        @JvmStatic
        fun withPrototypeBrowser() = withPrototypeBrowser(BrowserType.PULSAR_CHROME)

        /**
         * Use the specified browser with the prototype environment, any change to the browser will be kept.
         *
         * PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withPrototypeBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.crawl.fetch.privacy.PrototypePrivacyContextIdGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS, clazz)
            withBrowser(browserType)
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to the current network conditions
         * to obtain the best data quality and data collection speed.
         * */
        @JvmStatic
        fun withGoodNetwork(): Companion {
            return BrowserSettings
        }

        /**
         * Indicate the network condition.
         *
         * The system adjusts its behavior according to the current network conditions
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
         * The system adjusts its behavior according to the current network conditions
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
         * Launch the browser in GUI mode.
         * */
        @JvmStatic
        fun headed() = withGUI()

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
         * Set the number of privacy contexts
         * */
        @Deprecated("Verbose name", ReplaceWith("privacy(n)"))
        @JvmStatic
        fun privacyContext(n: Int): Companion = privacy(n)

        /**
         * Set the number of privacy contexts
         * */
        @JvmStatic
        fun privacy(n: Int): Companion {
            if (n <= 0) {
                throw IllegalArgumentException("The number of privacy context has to be > 0")
            }

            System.setProperty(PRIVACY_CONTEXT_NUMBER, "$n")
            return BrowserSettings
        }

        /**
         * Set the max number to open tabs in each browser context
         * */
        @JvmStatic
        fun maxTabs(n: Int): Companion {
            if (n <= 0) {
                throw IllegalArgumentException("The number of open tabs has to be > 0")
            }

            System.setProperty(BROWSER_MAX_ACTIVE_TABS, "$n")
            return BrowserSettings
        }

        /**
         * Tell the system to work with single page application.
         * To collect SPA data, the execution needs to have no timeout limit.
         * */
        @JvmStatic
        fun withSPA(): Companion {
            System.setProperty(FETCH_TASK_TIMEOUT, Duration.ofDays(1000).toString())
            System.setProperty(PRIVACY_CONTEXT_IDLE_TIMEOUT, Duration.ofDays(1000).toString())
            System.setProperty(BROWSER_SPA_MODE, "true")
            return BrowserSettings
        }

        /**
         * Enable url blocking. If url blocking is enabled and the blocking rules are set,
         * resources matching the rules will be blocked by the browser.
         * */
        @JvmStatic
        fun enableUrlBlocking(): Companion {
            System.setProperty(BROWSER_RESOURCE_BLOCK_PROBABILITY, "1.0")
            return BrowserSettings
        }

        @JvmStatic
        fun enableUrlBlocking(probability: Float): Companion {
            require(probability in 0.0f..1.0f)
            System.setProperty(BROWSER_RESOURCE_BLOCK_PROBABILITY, "$probability")
            return BrowserSettings
        }

        /**
         * Disable url blocking. If url blocking is disabled, blocking rules are ignored.
         * */
        @JvmStatic
        fun disableUrlBlocking(): Companion {
            System.setProperty(BROWSER_RESOURCE_BLOCK_PROBABILITY, "1.0")
            return BrowserSettings
        }

        /**
         * Block all images.
         * */
        @JvmStatic
        fun blockImages(): Companion {
            enableUrlBlocking()
            TODO("Not implemented")
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
            val numInstances = Files.list(AppPaths.CONTEXT_TMP_DIR).filter { Files.isDirectory(it) }.count().inc()
            val rand = Random.nextInt(0, 1000000).toString(Character.MAX_RADIX)
            return AppPaths.CONTEXT_TMP_DIR.resolve("cx.$numInstances$rand")
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
     * TODO: this flag might be upgraded by WSL
     * */
    val forceNoSandbox get() = AppContext.OS_IS_WSL

    /**
     * Add a --no-sandbox flag to launch the chrome if we are running inside a virtual machine,
     * for example, virtualbox, vmware or WSL
     * */
    val noSandbox get() = forceNoSandbox || conf.getBoolean(BROWSER_LAUNCH_NO_SANDBOX, true)

    /**
     * The browser's display mode, can be one of the following values:
     * * headless
     * * GUI
     * * supervised
     * */
    val displayMode
        get() = when {
            conf[BROWSER_DISPLAY_MODE] != null -> conf.getEnum(BROWSER_DISPLAY_MODE, DisplayMode.HEADLESS)
            isHeadlessOnly -> DisplayMode.HEADLESS
            else -> DisplayMode.GUI
        }

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
     * If PulsarPRA works in SPA mode:
     * 1. execution of fetches has no timeout limit
     * */
    val isSPA get() = conf.getBoolean(BROWSER_SPA_MODE, false)
    /**
     * Check if startup scripts are allowed. If true, pulsar injects scripts into the browser
     * before loading a page, and custom scripts are also allowed.
     * */
    val isStartupScriptEnabled get() = conf.getBoolean(BROWSER_JS_INVADING_ENABLED, true)
    /**
     * The probability to block resource requests.
     * The probability must be in [0, 1].
     * */
    val resourceBlockProbability get() = conf.getFloat(BROWSER_RESOURCE_BLOCK_PROBABILITY, 0.0f)
    /**
     * Check if user agent overriding is enabled. User agent overriding is disabled by default,
     * because inappropriate user agent overriding can be detected by the website,
     * furthermore, there is no obvious benefits to rotate the user agent.
     * */
    val isUserAgentOverridingEnabled get() = conf.getBoolean(BROWSER_ENABLE_UA_OVERRIDING, false)
    
    /**
     * The interaction settings. Interaction settings define how the system
     * interacts with webpages to mimic the behavior of real people.
     * */
    val interactSettings get() = Companion.interactSettings

    /**
     * Page load strategy.
     *
     * PulsarRPA checks document ready using javascript so just set the strategy to be none.
     *
     * @see <a href='https://blog.knoldus.com/page-loading-strategy-in-the-selenium-webdriver/'>
     *     Page Loading Strategy</a>
     * */
    var pageLoadStrategy = "none"

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
 * The interaction settings.
 * */
data class InteractSettings(
    /**
     * The number of scroll downs on the page.
     * */
    var scrollCount: Int = 3,
    /**
     * The time interval to scroll down on the page.
     * */
    var scrollInterval: Duration = Duration.ofMillis(500),
    /**
     * Timeout for executing custom scripts on the page.
     * */
    var scriptTimeout: Duration = Duration.ofMinutes(1),
    /**
     * Timeout for loading a webpage.
     * */
    var pageLoadTimeout: Duration = Duration.ofMinutes(3),
    /**
     * Whether to bring the webpage to the front.
     * */
    var bringToFront: Boolean = false,
    /**
     * Page positions to scroll to, these numbers are percentages of the total height,
     * e.g., 0.2 means to scroll to 20% of the height of the page.
     *
     * Some typical positions are:
     * * 0.3,0.75,0.4,0.5
     * * 0.2, 0.3, 0.5, 0.75, 0.5, 0.4, 0.5, 0.75
     * */
    var initScrollPositions: String = "0.3,0.75,0.4,0.5"
) {
    @JsonIgnore
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

    /**
     * TODO: just use an InteractSettings object, instead of separate properties
     * */
    fun overrideSystemProperties() {
        Systems.setProperty(FETCH_INTERACT_SETTINGS,
            pulsarObjectMapper().writeValueAsString(this))

        Systems.setProperty(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        Systems.setProperty(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        Systems.setProperty(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        Systems.setProperty(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }

    /**
     * TODO: just use an InteractSettings object, instead of separate properties
     * */
    fun overrideConfiguration(conf: MutableConfig) {
        Systems.setProperty(FETCH_INTERACT_SETTINGS,
            pulsarObjectMapper().writeValueAsString(this))

        conf.setInt(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        conf.setDuration(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        conf.setDuration(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }
    
    fun copy(settings: InteractSettings): InteractSettings {
        scrollCount = settings.scrollCount
        scrollInterval = settings.scrollInterval
        scriptTimeout = settings.scriptTimeout
        pageLoadTimeout = settings.pageLoadTimeout
        bringToFront = settings.bringToFront
        initScrollPositions = settings.initScrollPositions

        return this
    }

    fun noInitScroll(): InteractSettings {
        initScrollPositions = ""
        return this
    }

    fun noScroll(): InteractSettings {
        scrollCount = 0
        return this
    }

    fun goodNetwork() = copy(goodNetSettings)

    fun worseNetwork() = copy(worseNetSettings)

    fun worstNetwork() = copy(worstNetSettings)

    fun buildInitScrollPositions(): List<Double> {
        if (initScrollPositions.isBlank()) {
            return listOf()
        }

        return initScrollPositions.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    }

    fun buildScrollPositions(): List<Double> {
        val positions = buildInitScrollPositions().toMutableList()

        if (scrollCount <= 0) {
            return positions
        }

        val random = Random.nextInt(3)
        val enhancedScrollCount = (scrollCount + random - 1).coerceAtLeast(1)
        // some website show lazy content only when the page is in the front.
        repeat(enhancedScrollCount) { i ->
            val ratio = (0.6 + 0.1 * i).coerceAtMost(0.8)
            positions.add(ratio)
        }

        return positions
    }

    companion object {
        /**
         * Default settings for Web page interaction behavior.
         * */
        val DEFAULT = InteractSettings()

        /**
         * Web page interaction behavior settings under good network conditions, in which case we perform
         * each action faster.
         * */
        var goodNetSettings = InteractSettings()

        /**
         * Web page interaction behavior settings under worse network conditions, in which case we perform
         * each action more slowly.
         * */
        var worseNetSettings = InteractSettings(
            scrollCount = 10,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofMinutes(2),
            Duration.ofMinutes(3),
        )

        /**
         * Web page interaction behavior settings under worst network conditions, in which case we perform
         * each action very slowly.
         * */
        var worstNetSettings = InteractSettings(
            scrollCount = 15,
            scrollInterval = Duration.ofSeconds(3),
            scriptTimeout = Duration.ofMinutes(3),
            Duration.ofMinutes(4),
        )
    }
}
