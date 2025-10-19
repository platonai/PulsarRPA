package ai.platon.pulsar.skeleton

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.DisplayMode
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.browser.BrowserProfileMode
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.InteractLevel
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_OPEN_TABS
import ai.platon.pulsar.common.config.MutableConfig

/**
 * The [PulsarSettings] class defines a convenient interface to control the behavior of Browser4.
 *
 * For example, to run multiple temporary browsers in headless mode, which is usually used in the spider scenario,
 * you can use the following code:
 *
 * ```kotlin
 * PulsarSettings
 *    .headless()
 *    .maxBrowsers(4)
 *    .maxOpenTabs(12)
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
 * PulsarSettings.withSystemDefaultBrowser().withGUI().withSPA()
 * ```
 *
 * The above code will:
 * 1. Use the system's default browser
 * 2. Run the browser in GUI mode
 * 3. Set the system to work with single page application
 * */
data class PulsarSettings(
    val profileMode: BrowserProfileMode? = null,
    val displayMode: DisplayMode? = null,
    val maxBrowsers: Int? = null,
    val maxOpenTabs: Int? = null,
    val isSPA: Boolean? = null,
    val interactSettings: InteractSettings? = null,
) {
    fun overrideSystemProperties() {
        overrideConfigurationInternal(null)
    }

    fun overrideConfiguration(conf: MutableConfig) {
        overrideConfigurationInternal(conf)
    }

    private fun overrideConfigurationInternal(conf: MutableConfig? = null) {
        profileMode?.let { withBrowserContextMode(profileMode, conf) }
        when(displayMode) {
            DisplayMode.HEADLESS -> headless(conf)
            DisplayMode.GUI -> headed(conf)
            DisplayMode.SUPERVISED -> supervised(conf)
            else -> {}
        }
        maxBrowsers?.let { maxBrowserContexts(maxBrowsers) }
        maxOpenTabs?.let { maxOpenTabs(maxOpenTabs, conf) }
        isSPA?.takeIf { it }?.let { withSPA(conf) }
        interactSettings?.let { withInteractSettings(interactSettings, conf) }
    }

    companion object {
        @JvmStatic
        fun withBrowserContextMode(contextMode: BrowserProfileMode, conf: MutableConfig? = null): Companion =
            withBrowserContextMode(contextMode, BrowserType.DEFAULT, conf)

        @JvmStatic
        fun withBrowserContextMode(contextMode: BrowserProfileMode, browserType: BrowserType, conf: MutableConfig? = null): Companion {
            BrowserSettings.withBrowserContextMode(contextMode, browserType, conf)
            return PulsarSettings
        }

        @JvmStatic
        fun withBrowser(browserType: BrowserType, conf: MutableConfig? = null): Companion {
            BrowserSettings.withBrowser(browserType, conf)
            return PulsarSettings
        }

        /**
         * Use the system's default Chrome browser, so Browser4 visits websites just like you do.
         * Any change to the browser will be kept.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser(conf: MutableConfig? = null) = withBrowserContextMode(BrowserProfileMode.DEFAULT, BrowserType.DEFAULT, conf)

        /**
         * Use the system's default browser with the given type, so Browser4 visits websites just like you do.
         * Any change to the browser will be kept.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser(browserType: BrowserType, conf: MutableConfig? = null): Companion {
            return withBrowserContextMode(BrowserProfileMode.SYSTEM_DEFAULT, browserType, conf)
        }

        /**
         * Use the default browser which has an isolated profile and user data directory.
         * Any modifications made to the browser will be preserved, including the cookies, history, etc.
         * */
        @JvmStatic
        fun withDefaultBrowser(conf: MutableConfig? = null) = withBrowserContextMode(BrowserProfileMode.DEFAULT, BrowserType.DEFAULT, conf)

        /**
         * Use the default browser which has an isolated profile and user data directory.
         * Any modifications made to the browser will be preserved, including the cookies, history, etc.
         *
         *
         * */
        @JvmStatic
        fun withDefaultBrowser(browserType: BrowserType, conf: MutableConfig? = null): Companion {
            return withBrowserContextMode(BrowserProfileMode.DEFAULT, browserType, conf)
        }

        /**
         * Use Google Chrome with the prototype environment.
         * Any modifications made to the browser will be preserved.
         * Sequential and temporary browsers will inherit the environment from the prototype browser.
         */
        @JvmStatic
        fun withPrototypeBrowser(conf: MutableConfig? = null) = withBrowserContextMode(BrowserProfileMode.PROTOTYPE, BrowserType.DEFAULT, conf)

        /**
         * Use the specified browser with the prototype environment.
         * Any modifications made to the browser will be preserved.
         * Sequential and temporary browsers will inherit the environment from the prototype browser.
         * */
        @JvmStatic
        fun withPrototypeBrowser(browserType: BrowserType, conf: MutableConfig? = null): Companion {
            return withBrowserContextMode(BrowserProfileMode.PROTOTYPE, browserType, conf)
        }

        /**
         * Use sequential browsers that inherits the prototype browser’s environment.
         * The sequential browsers are permanent unless the context directories are deleted manually.
         *
         * @return the PulsarSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(conf: MutableConfig? = null): Companion {
            return withSequentialBrowsers(BrowserType.DEFAULT, 10, conf)
        }

        /**
         * Use sequential browsers that inherits the prototype browser’s environment.
         * The sequential browsers are permanent unless the context directories are deleted manually.
         *
         *
         *
         * @return the PulsarSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(browserType: BrowserType, conf: MutableConfig? = null): Companion {
            return withSequentialBrowsers(browserType, 10, conf)
        }

        /**
         * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
         * permanent unless the context directories are deleted manually.
         *
         * @param maxAgents The maximum number of sequential privacy agents, the active privacy contexts is chosen from them.
         * @return the PulsarSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(browserType: BrowserType, maxAgents: Int, conf: MutableConfig? = null): Companion {
            BrowserSettings.withSequentialBrowsers(browserType, maxAgents, conf)
            return PulsarSettings
        }

        /**
         * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
         * will not be used again after it is shut down.
         * */
        @JvmStatic
        fun withTemporaryBrowser(conf: MutableConfig? = null): Companion {
            return withBrowserContextMode(BrowserProfileMode.TEMPORARY, BrowserType.DEFAULT, conf)
        }

        /**
         * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
         * will not be used again after it is shut down.
         * */
        @JvmStatic
        fun withTemporaryBrowser(browserType: BrowserType, conf: MutableConfig? = null) =
            withBrowserContextMode(BrowserProfileMode.TEMPORARY, browserType, conf)

        /**
         * Launch the browser in GUI mode.
         * */
        @JvmStatic
        fun withGUI(conf: MutableConfig? = null): Companion {
            BrowserSettings.withGUI(conf)
            return PulsarSettings
        }

        /**
         * Launch the browser in GUI mode.
         * */
        @JvmStatic
        fun headed(conf: MutableConfig? = null) = withGUI(conf)

        /**
         * Launch the browser in headless mode.
         * */
        @JvmStatic
        fun headless(conf: MutableConfig? = null): Companion {
            BrowserSettings.headless(conf)
            return PulsarSettings
        }

        /**
         * Launch the browser in supervised mode.
         * */
        @JvmStatic
        fun supervised(conf: MutableConfig? = null): Companion {
            BrowserSettings.supervised(conf)
            return PulsarSettings
        }

        /**
         * Set the max number of agents
         * */
        @Deprecated("Use maxBrowserContexts instead", ReplaceWith("maxBrowserContexts(n)"))
        @JvmStatic
        fun maxBrowsers(n: Int, conf: MutableConfig? = null): Companion {
            maxBrowserContexts(n, conf)
            return PulsarSettings
        }

        /**
         * Set the max number of agents
         * */
        @JvmStatic
        fun maxBrowserContexts(n: Int, conf: MutableConfig? = null): Companion {
            BrowserSettings.maxBrowserContexts(n, conf)
            return PulsarSettings
        }

        /**
         * Set the max number to open tabs in each browser context
         * */
        @JvmStatic
        fun maxOpenTabs(n: Int, conf: MutableConfig? = null): Companion {
            // BrowserSettings.maxOpenTabs does not currently accept conf; we fallback to system property for conf=null
            if (conf == null) {
                BrowserSettings.maxOpenTabs(n)
            } else {
                conf[BROWSER_MAX_OPEN_TABS] = "$n"
            }
            return PulsarSettings
        }

        /**
         * Tell the system to work with single page application.
         * To collect SPA data, the execution needs to have no timeout limit.
         * */
        @JvmStatic
        fun withSPA(conf: MutableConfig? = null) = withSinglePageApplication(conf)

        /**
         * Tell the system to work with single page application.
         * To collect SPA data, the execution needs to have no timeout limit.
         * */
        @JvmStatic
        fun withSinglePageApplication(conf: MutableConfig? = null): Companion {
            BrowserSettings.withSPA(conf)
            return PulsarSettings
        }

        /**
         * Defines the level of interaction with a webpage during crawling.
         *
         * Higher levels involve more interaction (e.g., scrolling, clicking),
         * which may improve content extraction quality at the cost of speed.
         * */
        @JvmStatic
        fun withInteractLevel(level: InteractLevel, conf: MutableConfig? = null): Companion {
            // Interact settings modify config via settings override; no direct conf mapping here
            BrowserSettings.withInteractSettings(InteractSettings.create(level))
            return PulsarSettings
        }

        /**
         * Use the specified interact settings to interact with webpages.
         * */
        @JvmStatic
        fun withInteractSettings(settings: InteractSettings, conf: MutableConfig? = null): Companion {
            // Interact settings override system properties; conf routing is handled inside settings if needed
            BrowserSettings.withInteractSettings(settings)
            return PulsarSettings
        }
    }
}
