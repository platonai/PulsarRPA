package ai.platon.pulsar.skeleton

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.browser.BrowserContextMode
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.CapabilityTypes.*

/**
 * The [PulsarSettings] class defines a convenient interface to control the behavior of PulsarRPA.
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
open class PulsarSettings {

    fun withBrowserContextMode(contextMode: BrowserContextMode): PulsarSettings =
        withBrowserContextMode(contextMode, BrowserType.DEFAULT)

    fun withBrowserContextMode(contextMode: BrowserContextMode, browserType: BrowserType): PulsarSettings {
        BrowserSettings.withBrowserContextMode(contextMode, browserType)
        return this
    }

    fun withBrowser(browserType: BrowserType): PulsarSettings {
        BrowserSettings.withBrowser(browserType)
        return this
    }

    /**
     * Use the system's default Chrome browser, so PulsarRPA visits websites just like you do.
     * Any change to the browser will be kept.
     * */
    fun withSystemDefaultBrowser() = withBrowserContextMode(BrowserContextMode.DEFAULT, BrowserType.DEFAULT)

    /**
     * Use the system's default browser with the given type, so PulsarRPA visits websites just like you do.
     * Any change to the browser will be kept.
     * */
    fun withSystemDefaultBrowser(browserType: BrowserType): PulsarSettings {
        return withBrowserContextMode(BrowserContextMode.SYSTEM_DEFAULT, browserType)
    }

    /**
     * Use the default browser which has an isolated profile and user data directory.
     * Any modifications made to the browser will be preserved, including the cookies, history, etc.
     * */
    fun withDefaultBrowser() = withBrowserContextMode(BrowserContextMode.DEFAULT, BrowserType.DEFAULT)

    /**
     * Use the default browser which has an isolated profile and user data directory.
     * Any modifications made to the browser will be preserved, including the cookies, history, etc.
     *
     *
     * */
    fun withDefaultBrowser(browserType: BrowserType): PulsarSettings {
        return withBrowserContextMode(BrowserContextMode.DEFAULT, browserType)
    }

    /**
     * Use Google Chrome with the prototype environment.
     * Any modifications made to the browser will be preserved.
     * Sequential and temporary browsers will inherit the environment from the prototype browser.
     */
    fun withPrototypeBrowser() = withBrowserContextMode(BrowserContextMode.PROTOTYPE, BrowserType.DEFAULT)

    /**
     * Use the specified browser with the prototype environment.
     * Any modifications made to the browser will be preserved.
     * Sequential and temporary browsers will inherit the environment from the prototype browser.
     * */
    fun withPrototypeBrowser(browserType: BrowserType): PulsarSettings {
        return withBrowserContextMode(BrowserContextMode.PROTOTYPE, browserType)
    }

    /**
     * Use sequential browsers that inherits the prototype browser’s environment.
     * The sequential browsers are permanent unless the context directories are deleted manually.
     *
     * @return the PulsarSettings itself
     * */
    fun withSequentialBrowsers(): PulsarSettings {
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
    fun withSequentialBrowsers(browserType: BrowserType): PulsarSettings {
        return withSequentialBrowsers(browserType, 10)
    }

    /**
     * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
     * permanent unless the context directories are deleted manually.
     *
     * @param maxAgents The maximum number of sequential privacy agents, the active privacy contexts is chosen from them.
     * @return the PulsarSettings itself
     * */
    fun withSequentialBrowsers(browserType: BrowserType, maxAgents: Int): PulsarSettings {
        BrowserSettings.withSequentialBrowsers(browserType, maxAgents)
        return this
    }

    /**
     * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
     * will not be used again after it is shut down.
     * */
    fun withTemporaryBrowser(): PulsarSettings {
        return withBrowserContextMode(BrowserContextMode.TEMPORARY, BrowserType.DEFAULT)
    }

    /**
     * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
     * will not be used again after it is shut down.
     * */
    fun withTemporaryBrowser(browserType: BrowserType) = withBrowserContextMode(BrowserContextMode.TEMPORARY, browserType)

    /**
     * Launch the browser in GUI mode.
     * */
    fun withGUI(): PulsarSettings {
        BrowserSettings.withGUI()
        return this
    }

    /**
     * Launch the browser in GUI mode.
     * */
    fun headed() = withGUI()

    /**
     * Launch the browser in headless mode.
     * */
    fun headless(): PulsarSettings {
        BrowserSettings.headless()
        return this
    }

    /**
     * Launch the browser in supervised mode.
     * */
    fun supervised(): PulsarSettings {
        BrowserSettings.supervised()
        return this
    }

    /**
     * Set the max number of agents
     * */
    @Deprecated("Use maxBrowserContexts instead", ReplaceWith("maxBrowserContexts(n)"))
    fun maxBrowsers(n: Int): PulsarSettings {
        maxBrowserContexts(n)
        return this
    }

    /**
     * Set the max number of agents
     * */
    fun maxBrowserContexts(n: Int): PulsarSettings {
        BrowserSettings.maxBrowserContexts(n)
        return this
    }

    /**
     * Set the max number to open tabs in each browser context
     * */
    fun maxOpenTabs(n: Int): PulsarSettings {
        BrowserSettings.maxOpenTabs(n)
        return this
    }

    /**
     * Tell the system to work with single page application.
     * To collect SPA data, the execution needs to have no timeout limit.
     * */
    fun withSPA(): PulsarSettings {
        BrowserSettings.withSPA()
        return this
    }

    /**
     * Use the specified interact settings to interact with webpages.
     * */
    fun withInteractSettings(settings: InteractSettings): PulsarSettings {
        BrowserSettings.withInteractSettings(settings)
        return this
    }

    /**
     * Enable url blocking. If url blocking is enabled and the blocking rules are set,
     * resources matching the rules will be blocked by the browser.
     * */
    fun enableUrlBlocking(): PulsarSettings {
        BrowserSettings.enableUrlBlocking()
        return this
    }

    /**
     * Enable url blocking with the given probability.
     * The probability must be in [0, 1].
     * */
    fun enableUrlBlocking(probability: Float): PulsarSettings {
        BrowserSettings.enableUrlBlocking(probability)
        return this
    }

    /**
     * Disable url blocking. If url blocking is disabled, blocking rules are ignored.
     * */
    fun disableUrlBlocking(): PulsarSettings {
        BrowserSettings.disableUrlBlocking()
        return this
    }

    /**
     * Block all images.
     * */
    fun blockImages(): PulsarSettings {
        BrowserSettings.blockImages()
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
    fun enableOriginalPageContentAutoExporting(): PulsarSettings {
        BrowserSettings.enableOriginalPageContentAutoExporting()
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
    fun enableOriginalPageContentAutoExporting(limit: Int): PulsarSettings {
        BrowserSettings.enableOriginalPageContentAutoExporting(limit)
        return this
    }

    /**
     * Disable original page content exporting.
     * */
    fun disableOriginalPageContentAutoExporting(): PulsarSettings {
        BrowserSettings.disableOriginalPageContentAutoExporting()
        return this
    }

    /**
     * Sets the Large Language Model (LLM) provider for the PulsarRPA settings.
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
     * PulsarSettings().withLLM("volcengine", "ep-20250218132011-2scs8", apiKey)
     * ```
     *
     * @param provider The name of the LLM provider to be used. Must not be null.
     * @return The current instance of [PulsarSettings] to allow method chaining.
     * @throws IllegalArgumentException If the provided `provider` is null.
     */
    fun withLLM(provider: String, name: String, apiKey: String): PulsarSettings {
        withLLMProvider(provider)
        withLLMName(name)
        withLLMAPIKey(apiKey)
        return this
    }

    /**
     * Sets the Large Language Model (LLM) provider for the PulsarRPA settings.
     *
     * This function allows specifying the LLM provider to be used. The provider must be a non-null string.
     *
     * Supported LLM providers include:
     * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
     * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
     *
     * For example, you can use the following code to set the LLM provider:
     * ```kotlin
     * PulsarSettings()
     *     .withLLMProvider("volcengine") // use volcengine as the LLM provider
     *     .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
     *     .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own
     * ```
     *
     * @param provider The name of the LLM provider to be used. Must not be null.
     * @return The current instance of [PulsarSettings] to allow method chaining.
     * @throws IllegalArgumentException If the provided `provider` is null.
     */
    fun withLLMProvider(provider: String?): PulsarSettings {
        // Validate that the provider is not null
        requireNotNull(provider) { "$LLM_PROVIDER NOT set" }

        // Set the LLM provider as a system property
        System.setProperty(LLM_PROVIDER, provider)
        return this
    }

    /**
     * Sets the Large Language Model (LLM) name for the PulsarRPA settings.
     *
     * Supported LLM providers include:
     * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
     * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
     *
     * For example, you can use the following code to set the LLM provider:
     * ```kotlin
     * PulsarSettings()
     *     .withLLMProvider("volcengine") // use volcengine as the LLM provider
     *     .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
     *     .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own
     * ```
     *
     * @param name The name of the Large Language Model (LLM) to be set. This parameter cannot be null.
     * @return The current instance of [PulsarSettings] to allow for method chaining.
     * @throws IllegalArgumentException If the provided name is null.
     */
    fun withLLMName(name: String?): PulsarSettings {
        // Validate that the LLM name is not null
        requireNotNull(name) { "$LLM_NAME NOT set" }

        // Set the LLM name as a system property
        System.setProperty(LLM_NAME, name)
        return this
    }

    /**
     * Sets the Large Language Model (LLM) API key for the PulsarRPA settings.
     *
     * Supported LLM providers include:
     * * <a href='https://www.volcengine.com/docs/82379/1399008'>Volcengine API</a>
     * * <a href='https://api-docs.deepseek.com/'>DeepSeek API</a>
     *
     * For example, you can use the following code to set the LLM provider:
     * ```kotlin
     * PulsarSettings()
     *     .withLLMProvider("volcengine") // use volcengine as the LLM provider
     *     .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
     *     .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own
     * ```
     *
     * @param key The API key to be set. This parameter cannot be null, as the LLM service requires a valid API key.
     * @return The current instance of [PulsarSettings] to allow for method chaining.
     * @throws IllegalArgumentException If the provided API key is null, indicating that a valid key is required.
     */
    fun withLLMAPIKey(key: String?): PulsarSettings {
        // Validate that the API key is not null before setting it as a system property.
        requireNotNull(key) { "$LLM_API_KEY not set" }

        // Set the provided API key as a system property for global access.
        System.setProperty(LLM_API_KEY, key)
        return this
    }
}
