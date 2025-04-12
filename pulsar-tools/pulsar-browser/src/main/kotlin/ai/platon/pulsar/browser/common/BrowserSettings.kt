package ai.platon.pulsar.browser.common

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.AppConstants.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import java.net.URI
import java.time.Duration

open class BrowserSettings constructor(
    /**
     * The configuration.
     * */
    val config: ImmutableConfig = ImmutableConfig()
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
         * The default interaction settings. Interaction settings define how the system
         * interacts with webpages to mimic the behavior of real people.
         *
         * If you want to use a custom script confuser, you need to set the field before the BrowserSettings object is created.
         * If you are using spring boot, you should set the field in a ApplicationContextInitializer.
         * */
        var INTERACT_SETTINGS = InteractSettings.DEFAULT

        /**
         * The default script confuser, which is used to confuse the javascript that will be injected to the webpage.
         *
         * If you want to use a custom script confuser, you need to set the field before the BrowserSettings object is created.
         * If you are using spring boot, you should set the field in a ApplicationContextInitializer.
         * */
        var SCRIPT_CONFUSER: ScriptConfuser = SimpleScriptConfuser()

        /**
         * Specify the browser type to fetch webpages.
         *
         * 
         * */
        @JvmStatic
        fun withBrowser(browserType: String): Companion {
            System.setProperty(BROWSER_TYPE, browserType)
            return BrowserSettings
        }
        
        /**
         * Specify the browser type to fetch webpages.
         *
         * 
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
         * 
         * */
        @JvmStatic
        fun withSystemDefaultBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.skeleton.crawl.fetch.privacy.SystemDefaultPrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS, clazz)
            withBrowser(browserType)
            return BrowserSettings
        }
        
        /**
         * Use the default Chrome browser. Any change to the browser will be kept.
         * */
        @JvmStatic
        fun withDefaultBrowser() = withDefaultBrowser(BrowserType.PULSAR_CHROME)
        
        /**
         * Use the default Chrome browser. Any change to the browser will be kept.
         * 
         * */
        @JvmStatic
        fun withDefaultBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.skeleton.crawl.fetch.privacy.DefaultPrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS, clazz)
            withBrowser(browserType)
            maxBrowsers(1).maxOpenTabs(1000)
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
            val clazz = "ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrototypePrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS, clazz)
            withBrowser(browserType)
            maxBrowsers(1).maxOpenTabs(1000)
            return BrowserSettings
        }
        
        /**
         * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
         * permanent unless the context directories are deleted manually.
         *
         * PULSAR_CHROME is the only supported browser currently.
         *
         * @return the BrowserSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(): Companion {
            return withSequentialBrowsers(10)
        }
        
        /**
         * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
         * permanent unless the context directories are deleted manually.
         *
         * PULSAR_CHROME is the only supported browser currently.
         *
         * @param maxAgents The maximum number of sequential agents, the active agents are chosen from them.
         * @return the BrowserSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(maxAgents: Int): Companion {
            System.setProperty(MAX_SEQUENTIAL_PRIVACY_AGENT_NUMBER, "$maxAgents")
            val clazz = "ai.platon.pulsar.skeleton.crawl.fetch.privacy.SequentialPrivacyAgentGenerator"
            System.setProperty(PRIVACY_AGENT_GENERATOR_CLASS, clazz)
            return BrowserSettings
        }
        
        /**
         * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
         * will not be used again after it is shut down.* */
        @JvmStatic
        fun withTemporaryBrowser(): Companion {
            return withTemporaryBrowser(BrowserType.PULSAR_CHROME)
        }
        
        /**
         * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
         * will not be used again after it is shut down.
         *
         * PULSAR_CHROME is the only supported browser currently.
         * */
        @JvmStatic
        fun withTemporaryBrowser(browserType: BrowserType): Companion {
            val clazz = "ai.platon.pulsar.skeleton.crawl.fetch.privacy.RandomPrivacyAgentGenerator"
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
            InteractSettings.GOOD_NET_SETTINGS.overrideSystemProperties()
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
            InteractSettings.WORSE_NET_SETTINGS.overrideSystemProperties()
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
            InteractSettings.WORST_NET_SETTINGS.overrideSystemProperties()
            return BrowserSettings
        }
        
        /**
         * Launch the browser in GUI mode.
         * */
        @JvmStatic
        fun withGUI(): Companion {
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
         * Set the max number of browsers
         * */
        @JvmStatic
        fun maxBrowsers(n: Int): Companion {
            if (n <= 0) {
                throw IllegalArgumentException("The number of browser contexts has to be greater than 0")
            }
            if (n > 50) {
                System.err.println("PulsarRPA: The number of browser contexts is too large, it may cause out of disk space")
            }

            System.setProperty(PRIVACY_CONTEXT_NUMBER, "$n")
            return BrowserSettings
        }
        /**
         * Set the max number to open tabs in each browser context
         * */
        fun maxOpenTabs(n: Int): Companion {
            if (n <= 0) {
                throw IllegalArgumentException("The number of open tabs has to be > 0")
            }
            if (n > 50) {
                System.err.println("PulsarRPA: The number of open tabs is too large, it may cause out of memory")
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
         * Use the specified interact settings to interact with webpages.
         * */
        fun withInteractSettings(settings: InteractSettings): Companion {
            settings.overrideSystemProperties()
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
        /**
         * Export all pages automatically once they are fetched.
         *
         * The export directory is under AppPaths.WEB_CACHE_DIR.
         * A typical export path is:
         *
         * * AppPaths.WEB_CACHE_DIR/default/pulsar_chrome/OK/amazon-com
         * * C:\Users\pereg\AppData\Local\Temp\pulsar-pereg\cache\web\default\pulsar_chrome\OK\amazon-com
         * */
        @JvmStatic
        fun enableOriginalPageContentAutoExporting(): Companion {
            System.setProperty(FETCH_PAGE_AUTO_EXPORT_LIMIT, Int.MAX_VALUE.toString())
            return this
        }
        /**
         * Export at most [limit] pages once they are fetched.
         *
         * The export directory is under AppPaths.WEB_CACHE_DIR.
         * A typical export path is:
         *
         * * AppPaths.WEB_CACHE_DIR/default/pulsar_chrome/OK/amazon-com
         * * C:\Users\pereg\AppData\Local\Temp\pulsar-pereg\cache\web\default\pulsar_chrome\OK\amazon-com
         * */
        @JvmStatic
        fun enableOriginalPageContentAutoExporting(limit: Int): Companion {
            System.setProperty(FETCH_PAGE_AUTO_EXPORT_LIMIT, limit.toString())
            return this
        }
        /**
         * Disable original page content exporting.
         * */
        @JvmStatic
        fun disableOriginalPageContentAutoExporting(): Companion {
            System.setProperty(FETCH_PAGE_AUTO_EXPORT_LIMIT, "0")
            return this
        }
    }
    /**
     * The javascript to execute by Web browsers.
     * */
    private val jsPropertyNames: Array<String>
        get() = config.getTrimmedStrings(FETCH_CLIENT_JS_COMPUTED_STYLES, CLIENT_JS_PROPERTY_NAMES)

    /**
     * The supervisor process
     * */
    val supervisorProcess get() = config.get(BROWSER_LAUNCH_SUPERVISOR_PROCESS)
    /**
     * The supervisor process arguments
     * */
    val supervisorProcessArgs get() = config.getTrimmedStringCollection(BROWSER_LAUNCH_SUPERVISOR_PROCESS_ARGS)

    /**
     * Add a --no-sandbox flag to launch the chrome if we are running inside a virtual machine,
     * for example, virtualbox, vmware or WSL
     * */
    val noSandbox get() = config.getBoolean(BROWSER_LAUNCH_NO_SANDBOX, true)

    /**
     * The browser's display mode, can be one of the following values:
     * * headless
     * * GUI
     * * supervised
     * */
    val displayMode
        get() = when {
            config[BROWSER_DISPLAY_MODE] != null -> config.getEnum(BROWSER_DISPLAY_MODE, DisplayMode.HEADLESS)
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
    val isSPA get() = config.getBoolean(BROWSER_SPA_MODE, false)
    /**
     * Check if startup scripts are allowed. If true, PulsarPRA injects scripts into the browser
     * before loading a page, and custom scripts are also allowed.
     * */
    val isStartupScriptEnabled get() = config.getBoolean(BROWSER_JS_INVADING_ENABLED, true)
    /**
     * The probability to block resource requests.
     * The probability must be in [0, 1].
     * */
    val resourceBlockProbability get() = config.getFloat(BROWSER_RESOURCE_BLOCK_PROBABILITY, 0.0f)
    /**
     * Check if user agent overriding is enabled. User agent overriding is disabled by default,
     * because inappropriate user agent overriding can be detected by the website,
     * furthermore, there is no obvious benefits to rotate the user agent.
     * */
    val isUserAgentOverridingEnabled get() = config.getBoolean(BROWSER_ENABLE_UA_OVERRIDING, false)

    val fetchTaskTimeout get() = config.getDuration(FETCH_TASK_TIMEOUT, FETCH_TASK_TIMEOUT_DEFAULT)

    val pollingDriverTimeout get() = config.getDuration(POLLING_DRIVER_TIMEOUT, POLLING_DRIVER_TIMEOUT_DEFAULT)

    /**
     * The interaction settings. Interaction settings define how the system
     * interacts with webpages to mimic the behavior of real people.
     * */
    val interactSettings get() = InteractSettings.fromJson(config[BROWSER_INTERACT_SETTINGS], InteractSettings.DEFAULT)

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
    val confuser = BrowserSettings.SCRIPT_CONFUSER
    /**
     * The script loader.
     * */
    val scriptLoader = ScriptLoader(confuser, jsPropertyNames)

    open fun formatViewPort(delimiter: String = ","): String {
        return "${SCREEN_VIEWPORT.width}$delimiter${SCREEN_VIEWPORT.height}"
    }

    open fun createGeneralOptions(): MutableMap<String, Any> {
        val generalOptions = mutableMapOf<String, Any>()

        // generalOptions.setCapability("browserLanguage", "zh_CN")
        // generalOptions.setCapability("resolution", "${viewPort.width}x${viewPort.height}")

        return generalOptions
    }

    open fun createChromeOptions(generalOptions: Map<String, Any>): ChromeOptions {
        val chromeOptions = ChromeOptions()
        chromeOptions.merge(generalOptions)

        // rewrite proxy argument
        chromeOptions.removeArgument("proxy")
        when (val proxy = generalOptions["proxy"]) {
            is String -> chromeOptions.proxyServer = proxy
            is URI -> chromeOptions.proxyServer = proxy.host + ":" + proxy.port
            is ProxyEntry -> chromeOptions.proxyServer = proxy.hostPort
        }

        chromeOptions.headless = isHeadless
        chromeOptions.noSandbox = noSandbox

        chromeOptions
            .addArgument("window-position", "0,0")
            .addArgument("window-size", formatViewPort())
            .addArgument("pageLoadStrategy", pageLoadStrategy)
            .addArgument("throwExceptionOnScriptError", "true")
//            .addArgument("start-maximized")

        return chromeOptions
    }
}
