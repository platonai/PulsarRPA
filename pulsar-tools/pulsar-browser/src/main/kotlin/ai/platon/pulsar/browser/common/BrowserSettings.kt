package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.core.JacksonException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * The [BrowserSettings] class defines a convenient interface to control the behavior of browsers.
 * The [BrowserSettings] class provides a set of static methods to configure the browser settings.
 *
 * For example, to run multiple temporary browsers in headless mode, which is usually used in the spider scenario,
 * you can use the following code:
 *
 * ```kotlin
 * BrowserSettings
 *    .headless()
 *    .privacy(4)
 *    .maxTabs(12)
 *    .enableUrlBlocking()
 * ```
 *
 * The above code will:
 * 1. Run the browser in headless mode
 * 2. Set the number of privacy contexts to 4
 * 3. Set the max number of open tabs in each browser context to 12
 * 4. Enable url blocking
 *
 * If you want to run your system's default browser in GUI mode, and interact with the webpage, you can use the
 * following code:
 *
 * ```kotlin
 * BrowserSettings.withSystemDefaultBrowser().withGUI().withSPA()
 * ```
 *
 * The above code will:
 * 1. Use the system's default browser
 * 2. Run the browser in GUI mode
 * 3. Set the system to work with single page application
 * */
open class BrowserSettings(
    /**
     * The configuration.
     * */
    val conf: ImmutableConfig = ImmutableConfig()
) {
    companion object {
        /**
         * The viewport size for browser to rendering all webpages.
         * */
        var SCREEN_VIEWPORT = AppConstants.DEFAULT_VIEW_PORT
        /**
         * The screenshot quality.
         * Compression quality from range [0..100] (jpeg only) to capture screenshots.
         * */
        var SCREENSHOT_QUALITY = 50
        /**
         * The interaction settings. Interaction settings define how the system
         * interacts with webpages to mimic the behavior of real people.
         * */
        var INTERACT_SETTINGS = InteractSettings.DEFAULT
        /**
         * Check if the current environment supports only headless mode.
         * TODO: AppContext.isGUIAvailable doesn't work on some platform
         * */
        val isHeadlessOnly: Boolean get() = !AppContext.isGUIAvailable
        /**
         * Specify the browser type to fetch webpages.
         *
         * NOTICE: PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withBrowser(browserType: String): Companion {
            System.setProperty(BROWSER_TYPE, browserType)
            return BrowserSettings
        }
        /**
         * Specify the browser type to fetch webpages.
         *
         * NOTICE: PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withBrowser(browserType: BrowserType): Companion {
            System.setProperty(BROWSER_TYPE, browserType.name)
            return BrowserSettings
        }
        /**
         * Use the system's default Chrome browser, so PulsarRPA visits websites just like you do.
         * Any change to the browser will be kept.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser() = withSystemDefaultBrowser(BrowserType.PULSAR_CHROME)
        /**
         * Use the system's default browser with the given type, so PulsarRPA visits websites just like you do.
         * Any change to the browser will be kept.
         *
         * NOTICE: PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.crawl.fetch.privacy.SystemDefaultPrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS_KEY, clazz)
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
            val clazz = "ai.platon.pulsar.crawl.fetch.privacy.PrototypePrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS_KEY, clazz)
            withBrowser(browserType)
            return BrowserSettings
        }
        /**
         * Use a temporary browser that inherits from the prototype browser’s environment; the temporary browser
         * will not be used again after it is shut down.
         *
         * PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withTemporaryBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.crawl.fetch.privacy.SequentialPrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS_KEY, clazz)
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
            InteractSettings.GOOD_NET_SETTINGS.copy().overrideSystemProperties()
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
            InteractSettings.WORSE_NET_SETTINGS.copy().overrideSystemProperties()
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
            InteractSettings.WORST_NET_SETTINGS.copy().overrideSystemProperties()
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
        /**
         * Enable url blocking with the given probability.
         * The probability must be in [0, 1].
         * */
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
            System.setProperty(BROWSER_RESOURCE_BLOCK_PROBABILITY, "0.0")
            return BrowserSettings
        }
        /**
         * Block all images.
         * */
        @JvmStatic
        fun blockImages(): Companion {
            TODO("Not implemented")
            enableUrlBlocking()
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
        /**
         * Enable proxy if available.
         * */
        @JvmStatic
        fun enableProxy(): Companion {
            ProxyPoolManager.enableProxy()
            return this
        }
        /**
         * Disable proxy.
         * */
        @JvmStatic
        fun disableProxy(): Companion {
            ProxyPoolManager.disableProxy()
            return this
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
     * Check if it's SPA mode, SPA stands for Single Page Application.
     *
     * If PulsarPRA works in SPA mode:
     * 1. execution of loads and fetches has no timeout limit, so we can interact with the page as long as we want.
     * */
    val isSPA get() = conf.getBoolean(BROWSER_SPA_MODE, false)
    /**
     * Check if startup scripts are allowed. If true, PulsarPRA injects scripts into the browser
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
    val interactSettings get() = InteractSettings.fromJson(conf[BROWSER_INTERACT_SETTINGS], InteractSettings.DEFAULT)

    /**
     * Page load strategy.
     *
     * PulsarRPA checks document ready using javascript so just set the strategy to be none.
     *
     * @see <a href='https://blog.knoldus.com/page-loading-strategy-in-the-selenium-webdriver/'>
     *     Page Loading Strategy</a>
     * */
    var pageLoadStrategy = "none"

    /**
     * The user agent to use.
     * */
    val userAgent = UserAgent()
    /**
     * The script confuser.
     * */
    val confuser = ScriptConfuser()
    /**
     * The script loader.
     * */
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
     * Whether to bring the page to the front.
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
    /**
     * The minimum delay time in milliseconds.
     * */
    var minDelayMillis = 100
    /**
     * The minimum delay time in milliseconds.
     * */
    var maxDelayMillis = 2000
    /**
     * The delay policy for each action.
     * The delay policy is a map from action to a range of delay time in milliseconds.
     * */
    var delayPolicy = mutableMapOf(
        "gap" to 200..700,
        "click" to 500..1500,
        "delete" to 30..80,
        "keyUpDown" to 50..150,
        "press" to 100..400,
        "type" to 50..550,
        "mouseWheel" to 800..1300,
        "dragAndDrop" to 800..1300,
        "waitForNavigation" to 500..1000,
        "waitForSelector" to 500..1000,
        "waitUntil" to 500..1000,
        "default" to 100..600,
        "" to 100..600
    )
    /**
     * The minimum delay time in milliseconds.
     * */
    var minTimeout = Duration.ofSeconds(1)
    /**
     * The minimum delay time in milliseconds.
     * */
    var maxTimeout = Duration.ofMinutes(3)
    /**
     * Timeout policy for each action in seconds.
     * */
    var timeoutPolicy = mutableMapOf(
        "pageLoad" to pageLoadTimeout,
        "script" to scriptTimeout,
        "waitForNavigation" to Duration.ofSeconds(60),
        "waitForSelector" to Duration.ofSeconds(60),
        "waitUntil" to Duration.ofSeconds(60),
        "default" to Duration.ofSeconds(60),
        "" to Duration.ofSeconds(60)
    )

    /**
     * The delay policy for each action.
     * The delay policy is a map from action to a range of delay time in milliseconds.
     *
     * The map should contain the following keys:
     * * gap
     * * click
     * * delete
     * * keyUpDown
     * * press
     * * type
     * * mouseWheel
     * * dragAndDrop
     * * waitForNavigation
     * * waitForSelector
     * * waitUntil
     * * default
     * * ""(empty key)
     *
     * @return a map from action to a range of delay time in milliseconds.
     * */
    fun generateRestrictedDelayPolicy(): Map<String, IntRange> {
        val fallback = (minDelayMillis..maxDelayMillis)
        
        delayPolicy.forEach { (action, delay) ->
            if (delay.first < minDelayMillis) {
                delayPolicy[action] = minDelayMillis..delay.last.coerceAtLeast(minDelayMillis)
            } else if (delay.last > maxDelayMillis) {
                delayPolicy[action] = delay.first.coerceAtMost(maxDelayMillis)..maxDelayMillis
            }
        }
        
        delayPolicy["default"] = delayPolicy["default"] ?: fallback
        delayPolicy[""] = delayPolicy["default"] ?: fallback
        
        return delayPolicy
    }
    
    /**
     * Timeout policy for each action.
     *
     * The map should contain the following keys:
     * * waitForNavigation
     * * waitForSelector
     * * waitUntil
     * * default
     * * ""(empty key)
     *
     * @return a map from action to a range of delay time in milliseconds.
     * */
    fun generateRestrictedTimeoutPolicy(): Map<String, Duration> {
        val fallback = Duration.ofSeconds(60)
        
        timeoutPolicy.forEach { (action, timeout) ->
            if (timeout < minTimeout) {
                timeoutPolicy[action] = minTimeout
            } else if (timeout > maxTimeout) {
                timeoutPolicy[action] = maxTimeout
            }
        }
        
        timeoutPolicy["default"] = timeoutPolicy["default"] ?: fallback
        timeoutPolicy[""] = timeoutPolicy["default"] ?: fallback
        
        return timeoutPolicy
    }
    
    /**
     * TODO: just use an InteractSettings object, instead of separate properties
     * */
    fun overrideSystemProperties() {
        Systems.setProperty(BROWSER_INTERACT_SETTINGS,
            pulsarObjectMapper().writeValueAsString(this))
        
        Systems.setProperty(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        Systems.setProperty(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        Systems.setProperty(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        Systems.setProperty(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }
    
    fun overrideConfiguration(conf: MutableConfig) {
        conf[BROWSER_INTERACT_SETTINGS] = pulsarObjectMapper().writeValueAsString(this)
        /**
         * TODO: just use an InteractSettings object, instead of separate properties
         * */
        conf.setInt(FETCH_SCROLL_DOWN_COUNT, scrollCount)
        conf.setDuration(FETCH_SCROLL_DOWN_INTERVAL, scrollInterval)
        conf.setDuration(FETCH_SCRIPT_TIMEOUT, scriptTimeout)
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout)
    }

    /**
     * Do not scroll the page by default.
     * */
    fun noScroll(): InteractSettings {
        initScrollPositions = ""
        scrollCount = 0
        return this
    }

    /**
     * Build the initial scroll positions.
     * */
    fun buildInitScrollPositions(): List<Double> {
        if (initScrollPositions.isBlank()) {
            return listOf()
        }

        return initScrollPositions.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    }

    /**
     * Build the scroll positions.
     * */
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
    
    /**
     * Convert the object to a json string.
     *
     * @return a json string
     * */
    @Throws(JacksonException::class)
    fun toJson(): String {
        return pulsarObjectMapper().writeValueAsString(this)
    }

    companion object {
        private val OBJECT_CACHE = ConcurrentHashMap<String, InteractSettings>()
        
        /**
         * Default settings for Web page interaction behavior.
         * */
        val DEFAULT = InteractSettings()

        /**
         * Web page interaction behavior settings under good network conditions, in which case we perform
         * each action faster.
         * */
        val GOOD_NET_SETTINGS = InteractSettings()

        /**
         * Web page interaction behavior settings under worse network conditions, in which case we perform
         * each action more slowly.
         * */
        val WORSE_NET_SETTINGS = InteractSettings(
            scrollCount = 10,
            scrollInterval = Duration.ofSeconds(1),
            scriptTimeout = Duration.ofMinutes(2),
            Duration.ofMinutes(3),
        )

        /**
         * Web page interaction behavior settings under worst network conditions, in which case we perform
         * each action very slowly.
         * */
        val WORST_NET_SETTINGS = InteractSettings(
            scrollCount = 15,
            scrollInterval = Duration.ofSeconds(3),
            scriptTimeout = Duration.ofMinutes(3),
            Duration.ofMinutes(4),
        )
        
        /**
         * Parse the json string to an InteractSettings object.
         *
         * @param json the json string
         * @return an InteractSettings object
         * */
        @Throws(JacksonException::class)
        fun fromJson(json: String): InteractSettings {
            return OBJECT_CACHE.computeIfAbsent(json) {
                pulsarObjectMapper().readValue(json, InteractSettings::class.java)
            }
        }
        
        /**
         * Parse the json string to an InteractSettings object.
         *
         * @param json the json string
         * @param defaultValue the default value
         * @return an InteractSettings object
         * */
        fun fromJson(json: String?, defaultValue: InteractSettings): InteractSettings {
            return fromJsonOrNull(json) ?: defaultValue
        }
        
        /**
         * Parse the json string to an InteractSettings object.
         *
         * @param json the json string
         * @return an InteractSettings object, or null if the json string is null, or the json string is invalid
         * */
        fun fromJsonOrNull(json: String?): InteractSettings? = json?.runCatching { fromJson(json) }?.getOrNull()
    }
}
