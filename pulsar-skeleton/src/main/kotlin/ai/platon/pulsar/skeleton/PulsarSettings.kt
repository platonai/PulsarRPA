package ai.platon.pulsar.skeleton

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.browser.BrowserType

/**
 * The [PulsarSettings] class defines a convenient interface to control the behavior of PulsarRPA.
 *
 * For example, to run multiple temporary browsers in headless mode, which is usually used in the spider scenario,
 * you can use the following code:
 *
 * ```kotlin
 * PulsarSettings
 *    .headless()
 *    .privacy(4)
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
    /**
     * Use the system's default Chrome browser, so PulsarRPA visits websites just like you do.
     * Any change to the browser will be kept.
     * */
    fun withSystemDefaultBrowser() = withSystemDefaultBrowser(BrowserType.PULSAR_CHROME)
    /**
     * Use the system's default browser with the given type, so PulsarRPA visits websites just like you do.
     * Any change to the browser will be kept.
     *
     * NOTICE: PULSAR_CHROME is the only supported browser currently.
     * */
    fun withSystemDefaultBrowser(browserType: BrowserType): PulsarSettings {
        BrowserSettings.withSystemDefaultBrowser(browserType)
        return this
    }
    /**
     * Use the default Chrome browser. Any change to the browser will be kept.
     * */
    fun withDefaultBrowser() = withDefaultBrowser(BrowserType.PULSAR_CHROME)
    /**
     * Use the default Chrome browser. Any change to the browser will be kept.
     *
     * NOTICE: PULSAR_CHROME is the only supported browser currently.
     * */
    fun withDefaultBrowser(browserType: BrowserType): PulsarSettings {
        BrowserSettings.withDefaultBrowser(browserType)
        return this
    }
    /**
     * Use google-chrome with the prototype environment, any change to the browser will be kept.
     * */
    fun withPrototypeBrowser() = withPrototypeBrowser(BrowserType.PULSAR_CHROME)
    /**
     * Use the specified browser with the prototype environment, any change to the browser will be kept.
     *
     * PULSAR_CHROME is the only supported browser currently.
     * */
    fun withPrototypeBrowser(browserType: BrowserType): PulsarSettings {
        BrowserSettings.withPrototypeBrowser(browserType)
        return this
    }
    /**
     * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
     * permanent unless the context directories are deleted manually.
     *
     * PULSAR_CHROME is the only supported browser currently.
     *
     * @return the PulsarSettings itself
     * */
    fun withSequentialBrowsers(): PulsarSettings {
        return withSequentialBrowsers(10)
    }
    /**
     * Use sequential browsers that inherits from the prototype browser’s environment. The sequential browsers are
     * permanent unless the context directories are deleted manually.
     *
     * PULSAR_CHROME is the only supported browser currently.
     *
     * @param maxAgents The maximum number of sequential privacy agents, the active privacy contexts is chosen from them.
     * @return the PulsarSettings itself
     * */
    fun withSequentialBrowsers(maxAgents: Int): PulsarSettings {
        BrowserSettings.withSequentialBrowsers(maxAgents)
        return this
    }
    /**
     * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
     * will not be used again after it is shut down.
     * */
    fun withTemporaryBrowser(): PulsarSettings {
        return withTemporaryBrowser(BrowserType.PULSAR_CHROME)
    }
    /**
     * Use a temporary browser that inherits from the prototype browser’s environment. The temporary browser
     * will not be used again after it is shut down.
     *
     * PULSAR_CHROME is the only supported browser currently.
     * */
    fun withTemporaryBrowser(browserType: BrowserType): PulsarSettings {
        BrowserSettings.withTemporaryBrowser(browserType)
        return this
    }
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
    fun maxBrowsers(n: Int): PulsarSettings {
        BrowserSettings.maxBrowsers(n)
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
     * Enable proxy if available.
     * */
    fun enableProxy(): PulsarSettings {
        BrowserSettings.enableProxy()
        return this
    }
    /**
     * Disable proxy.
     * */
    fun disableProxy(): PulsarSettings {
        BrowserSettings.disableProxy()
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
}
