package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings

/**
 * A factory to create browser instances.
 * */
interface BrowserFactory {
    /**
     * Connect to a browser instance, the browser instance should be open with Chrome devtools open.
     * */
    fun connect(port: Int, settings: BrowserSettings = BrowserSettings()): Browser

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