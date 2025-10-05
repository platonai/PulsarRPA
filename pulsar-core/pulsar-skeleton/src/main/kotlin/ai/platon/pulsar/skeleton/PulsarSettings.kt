package ai.platon.pulsar.skeleton

import ai.platon.pulsar.browser.common.BlockRule
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.DisplayMode
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.browser.BrowserContextMode
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.InteractLevel
import ai.platon.pulsar.common.config.CapabilityTypes.*
import dev.langchain4j.model.chat.ChatModel

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
    val browserContextMode: BrowserContextMode = BrowserContextMode.DEFAULT,
    val browserType: BrowserType = BrowserType.DEFAULT,
    val displayMode: DisplayMode = DisplayMode.GUI,
    val maxBrowsers: Int = Int.MAX_VALUE,
    val maxOpenTabs: Int = Int.MAX_VALUE,
    val supervisorProcess: String? = null,
    val supervisorProcessArgs: String? = null,
    val isSPA: Boolean = false,
    val interactSettings: InteractSettings = InteractSettings.DEFAULT,
    val blockRule: BlockRule? = null,
    val chatModel: ChatModel? = null,
) {
    companion object {
        @JvmStatic
        fun withBrowserContextMode(contextMode: BrowserContextMode): Companion =
            withBrowserContextMode(contextMode, BrowserType.DEFAULT)

        @JvmStatic
        fun withBrowserContextMode(contextMode: BrowserContextMode, browserType: BrowserType): Companion {
            BrowserSettings.withBrowserContextMode(contextMode, browserType)
            return PulsarSettings
        }

        @JvmStatic
        fun withBrowser(browserType: BrowserType): Companion {
            BrowserSettings.withBrowser(browserType)
            return PulsarSettings
        }

        /**
         * Use the system's default Chrome browser, so Browser4 visits websites just like you do.
         * Any change to the browser will be kept.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser() = withBrowserContextMode(BrowserContextMode.DEFAULT, BrowserType.DEFAULT)

        /**
         * Use the system's default browser with the given type, so Browser4 visits websites just like you do.
         * Any change to the browser will be kept.
         * */
        @JvmStatic
        fun withSystemDefaultBrowser(browserType: BrowserType): Companion {
            return withBrowserContextMode(BrowserContextMode.SYSTEM_DEFAULT, browserType)
        }

        /**
         * Use the default browser which has an isolated profile and user data directory.
         * Any modifications made to the browser will be preserved, including the cookies, history, etc.
         * */
        @JvmStatic
        fun withDefaultBrowser() = withBrowserContextMode(BrowserContextMode.DEFAULT, BrowserType.DEFAULT)

        /**
         * Use the default browser which has an isolated profile and user data directory.
         * Any modifications made to the browser will be preserved, including the cookies, history, etc.
         *
         *
         * */
        @JvmStatic
        fun withDefaultBrowser(browserType: BrowserType): Companion {
            return withBrowserContextMode(BrowserContextMode.DEFAULT, browserType)
        }

        /**
         * Use Google Chrome with the prototype environment.
         * Any modifications made to the browser will be preserved.
         * Sequential and temporary browsers will inherit the environment from the prototype browser.
         */
        @JvmStatic
        fun withPrototypeBrowser() = withBrowserContextMode(BrowserContextMode.PROTOTYPE, BrowserType.DEFAULT)

        /**
         * Use the specified browser with the prototype environment.
         * Any modifications made to the browser will be preserved.
         * Sequential and temporary browsers will inherit the environment from the prototype browser.
         * */
        @JvmStatic
        fun withPrototypeBrowser(browserType: BrowserType): Companion {
            return withBrowserContextMode(BrowserContextMode.PROTOTYPE, browserType)
        }

        /**
         * Use sequential browsers that inherits the prototype browser’s environment.
         * The sequential browsers are permanent unless the context directories are deleted manually.
         *
         * @return the PulsarSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(): Companion {
            return withSequentialBrowsers(BrowserType.DEFAULT, 10)
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
        fun withSequentialBrowsers(browserType: BrowserType): Companion {
            return withSequentialBrowsers(browserType, 10)
        }

        /**
         * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
         * permanent unless the context directories are deleted manually.
         *
         * @param maxAgents The maximum number of sequential privacy agents, the active privacy contexts is chosen from them.
         * @return the PulsarSettings itself
         * */
        @JvmStatic
        fun withSequentialBrowsers(browserType: BrowserType, maxAgents: Int): Companion {
            BrowserSettings.withSequentialBrowsers(browserType, maxAgents)
            return PulsarSettings
        }

        /**
         * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
         * will not be used again after it is shut down.
         * */
        @JvmStatic
        fun withTemporaryBrowser(): Companion {
            return withBrowserContextMode(BrowserContextMode.TEMPORARY, BrowserType.DEFAULT)
        }

        /**
         * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
         * will not be used again after it is shut down.
         * */
        @JvmStatic
        fun withTemporaryBrowser(browserType: BrowserType) =
            withBrowserContextMode(BrowserContextMode.TEMPORARY, browserType)

        /**
         * Launch the browser in GUI mode.
         * */
        @JvmStatic
        fun withGUI(): Companion {
            BrowserSettings.withGUI()
            return PulsarSettings
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
            BrowserSettings.headless()
            return PulsarSettings
        }

        /**
         * Launch the browser in supervised mode.
         * */
        @JvmStatic
        fun supervised(): Companion {
            BrowserSettings.supervised()
            return PulsarSettings
        }

        /**
         * Set the max number of agents
         * */
        @Deprecated("Use maxBrowserContexts instead", ReplaceWith("maxBrowserContexts(n)"))
        @JvmStatic
        fun maxBrowsers(n: Int): Companion {
            maxBrowserContexts(n)
            return PulsarSettings
        }

        /**
         * Set the max number of agents
         * */
        @JvmStatic
        fun maxBrowserContexts(n: Int): Companion {
            BrowserSettings.maxBrowserContexts(n)
            return PulsarSettings
        }

        /**
         * Set the max number to open tabs in each browser context
         * */
        @JvmStatic
        fun maxOpenTabs(n: Int): Companion {
            BrowserSettings.maxOpenTabs(n)
            return PulsarSettings
        }

        /**
         * Tell the system to work with single page application.
         * To collect SPA data, the execution needs to have no timeout limit.
         * */
        @JvmStatic
        fun withSPA(): Companion {

            BrowserSettings.withSPA()
            return PulsarSettings
        }

        /**
         * Defines the level of interaction with a webpage during crawling.
         *
         * Higher levels involve more interaction (e.g., scrolling, clicking),
         * which may improve content extraction quality at the cost of speed.
         * */
        @JvmStatic
        fun withInteractLevel(level: InteractLevel): Companion {
            BrowserSettings.withInteractSettings(InteractSettings.create(level))
            return PulsarSettings
        }

        /**
         * Use the specified interact settings to interact with webpages.
         * */
        @JvmStatic
        fun withInteractSettings(settings: InteractSettings): Companion {
            BrowserSettings.withInteractSettings(settings)
            return PulsarSettings
        }

        /**
         * Enable url blocking. If url blocking is enabled and the blocking rules are set,
         * resources matching the rules will be blocked by the browser.
         * */
        @JvmStatic
        fun enableUrlBlocking(): Companion {
            BrowserSettings.enableUrlBlocking()
            return PulsarSettings
        }

        /**
         * Enable url blocking with the given probability.
         * The probability must be in [0, 1].
         * */
        @JvmStatic
        fun enableUrlBlocking(probability: Float): Companion {
            BrowserSettings.enableUrlBlocking(probability)
            return PulsarSettings
        }

        /**
         * Disable url blocking. If url blocking is disabled, blocking rules are ignored.
         * */
        @JvmStatic
        fun disableUrlBlocking(): Companion {
            BrowserSettings.disableUrlBlocking()
            return PulsarSettings
        }

        /**
         * Block all images.
         * */
        @JvmStatic
        fun blockImages(): Companion {
            BrowserSettings.blockImages()
            return PulsarSettings
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
            BrowserSettings.enableOriginalPageContentAutoExporting()
            return PulsarSettings
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
            BrowserSettings.enableOriginalPageContentAutoExporting(limit)
            return PulsarSettings
        }

        /**
         * Disable original page content exporting.
         * */
        @JvmStatic
        fun disableOriginalPageContentAutoExporting(): Companion {
            BrowserSettings.disableOriginalPageContentAutoExporting()
            return PulsarSettings
        }

        /**
         * Sets the Large Language Model (LLM) provider for the Browser4 settings.
         *
         * This function allows specifying the LLM provider to be used. The provider must be a non-null string.
         *
         * Supported LLM providers include:
         * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
         * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
         *
         * For example, you can use the following code to set the LLM provider:
         * ```kotlin
         * // use volcengine as the LLM provider
         * PulsarSettings.withLLM("volcengine", "ep-20250218132011-2scs8", apiKey)
         * ```
         *
         * @param provider The name of the LLM provider to be used. Must not be null.
         * @return The current instance of [PulsarSettings] to allow method chaining.
         * @throws IllegalArgumentException If the provided `provider` is null.
         */
        @JvmStatic
        fun withLLM(provider: String, name: String, apiKey: String): Companion {
            withLLMProvider(provider)
            withLLMName(name)
            withLLMAPIKey(apiKey)
            return PulsarSettings
        }

        /**
         * Sets the Large Language Model (LLM) provider for the Browser4 settings.
         *
         * This function allows specifying the LLM provider to be used. The provider must be a non-null string.
         *
         * Supported LLM providers include:
         * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
         * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
         *
         * For example, you can use the following code to set the LLM provider:
         * ```kotlin
         * PulsarSettings
         *     .withLLMProvider("volcengine") // use volcengine as the LLM provider
         *     .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
         *     .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own
         * ```
         *
         * @param provider The name of the LLM provider to be used. Must not be null.
         * @return The current instance of [PulsarSettings] to allow method chaining.
         * @throws IllegalArgumentException If the provided `provider` is null.
         */
        @JvmStatic
        fun withLLMProvider(provider: String?): Companion {
            // Validate that the provider is not null
            requireNotNull(provider) { "$LLM_PROVIDER NOT set" }

            // Set the LLM provider as a system property
            System.setProperty(LLM_PROVIDER, provider)
            return PulsarSettings
        }

        /**
         * Sets the Large Language Model (LLM) name for the Browser4 settings.
         *
         * Supported LLM providers include:
         * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
         * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
         *
         * For example, you can use the following code to set the LLM provider:
         * ```kotlin
         * PulsarSettings
         *     .withLLMProvider("volcengine") // use volcengine as the LLM provider
         *     .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
         *     .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own
         * ```
         *
         * @param name The name of the Large Language Model (LLM) to be set. This parameter cannot be null.
         * @return The current instance of [PulsarSettings] to allow for method chaining.
         * @throws IllegalArgumentException If the provided name is null.
         */
        @JvmStatic
        fun withLLMName(name: String?): Companion {
            // Validate that the LLM name is not null
            requireNotNull(name) { "$LLM_NAME NOT set" }

            // Set the LLM name as a system property
            System.setProperty(LLM_NAME, name)
            return PulsarSettings
        }

        /**
         * Sets the Large Language Model (LLM) API key for the Browser4 settings.
         *
         * Supported LLM providers include:
         * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
         * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
         *
         * For example, you can use the following code to set the LLM provider:
         * ```kotlin
         * PulsarSettings
         *     .withLLMProvider("volcengine") // use volcengine as the LLM provider
         *     .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
         *     .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own
         * ```
         *
         * @param key The API key to be set. This parameter cannot be null, as the LLM service requires a valid API key.
         * @return The current instance of [PulsarSettings] to allow for method chaining.
         * @throws IllegalArgumentException If the provided API key is null, indicating that a valid key is required.
         */
        @JvmStatic
        fun withLLMAPIKey(key: String?): Companion {
            // Validate that the API key is not null before setting it as a system property.
            requireNotNull(key) { "$LLM_API_KEY not set" }

            // Set the provided API key as a system property for global access.
            System.setProperty(LLM_API_KEY, key)
            
            return PulsarSettings
        }
    }
}
