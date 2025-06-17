package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

/**
 * A factory to create browser instances.
 * */
interface BrowserFactory {
    /**
     * Connect to a browser instance, the browser instance should be open with Chrome devtools open.
     *
     * @param browserType The type of the browser to connect to, e.g. `BrowserType.PULSAR_CHROME`.
     * @param port The port on which the browser is listening for CDP connections.
     * @return The newly created `Browser` instance.
     * @throws BrowserUnavailableException If the connection fails or the context cannot be created.
     * */
    @Throws(BrowserLaunchException::class)
    fun connect(browserType: BrowserType, port: Int, settings: BrowserSettings = BrowserSettings()): Browser

    /**
     * Connects to an existing Chromium browser instance via CDP and creates a new context for PulsarRPA.
     *
     * @param browserType The type of the browser, e.g. `BrowserType.PULSAR_CHROME`.
     * @param endpointURL The CDP WebSocket or HTTP endpoint, e.g.:
     *  `http://localhost:9222/` or
     *  `ws://127.0.0.1:9222/devtools/browser/387adf4a-243f-4051-a181-46798f4a46f4`
     * @return The newly created `BrowserContext`
     * @throws BrowserUnavailableException If the connection fails or the context cannot be created.
     */
    @Throws(BrowserLaunchException::class)
    fun connectOverCDP(browserType: BrowserType, endpointURL: String, settings: BrowserSettings = BrowserSettings()): Browser

    /**
     * Launch the system default browser, the system default browser is your daily used browser.
     * */
    @Throws(BrowserLaunchException::class)
    fun launch(browserId: BrowserId): Browser

    /**
     * Launch the system default browser, the system default browser is your daily used browser.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchSystemDefaultBrowser(): Browser

    /**
     * Launch the default browser, notice, the default browser is not the one you used daily.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchDefaultBrowser(): Browser

    /**
     * Launch the prototype browser, the prototype browser is a browser instance with default settings.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchPrototypeBrowser(): Browser

    /**
     * Launch the next sequential browser, the browser's user data dir rotates between a group of dirs.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchNextSequentialBrowser(): Browser

    /**
     * Launch a random temporary browser, the browser's user data dir is a random temporary dir.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchRandomTempBrowser(): Browser
}
