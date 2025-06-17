package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

interface BrowserLauncher {

    /**
     * Connects to an existing Chromium browser instance via CDP and creates a new context for PulsarRPA.
     *
     * @param port The port on which the Chromium browser is listening for CDP connections.
     * @return The newly created `BrowserContext`
     * @throws BrowserUnavailableException If the connection fails or the context cannot be created.
     */
    @Throws(BrowserLaunchException::class)
    fun connect(port: Int, settings: BrowserSettings = BrowserSettings()): Browser

    /**
     * Connects to an existing Chromium browser instance via CDP and creates a new context for PulsarRPA.
     *
     * @param endpointURL The CDP WebSocket or HTTP endpoint, e.g.:
     *  `http://localhost:9222/` or
     *  `ws://127.0.0.1:9222/devtools/browser/387adf4a-243f-4051-a181-46798f4a46f4`
     * @return The newly created `BrowserContext`
     * @throws BrowserUnavailableException If the connection fails or the context cannot be created.
     */
    @Throws(BrowserLaunchException::class)
    fun connectOverCDP(endpointURL: String, settings: BrowserSettings = BrowserSettings()): Browser

    /**
     * Launch a browser with the given browser id, the browser id is used to identify the browser instance.
     * @throws BrowserLaunchException If the browser cannot be launched.
     */
    @Throws(BrowserLaunchException::class)
    fun launch(browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions): Browser
}
