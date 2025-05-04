package ai.platon.pulsar.protocol.browser.impl

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLaunchException
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

abstract class AbstractBrowserFactory : BrowserFactory {
    /**
     * Launch a browser with the given browser id, the browser id is used to identify the browser instance.
     * */
    @Throws(BrowserLaunchException::class)
    abstract fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser

    /**
     * Launch the system default browser, the system default browser is your daily used browser.
     * */
    @Throws(BrowserLaunchException::class)
    override fun launchSystemDefaultBrowser(): Browser =
        launch(BrowserId.SYSTEM_DEFAULT, LauncherOptions(), ChromeOptions())

    /**
     * Launch the default browser, notice, the default browser is not the one you used daily.
     * */
    @Throws(BrowserLaunchException::class)
    override fun launchDefaultBrowser(): Browser = launch(BrowserId.DEFAULT, LauncherOptions(), ChromeOptions())

    /**
     * Launch the prototype browser, the prototype browser is a browser instance with default settings.
     * */
    @Throws(BrowserLaunchException::class)
    override fun launchPrototypeBrowser(): Browser = launch(BrowserId.PROTOTYPE, LauncherOptions(), ChromeOptions())

    /**
     * Launch the next sequential browser, the browser's user data dir rotates between a group of dirs.
     * */
    @Throws(BrowserLaunchException::class)
    override fun launchNextSequentialBrowser(): Browser =
        launch(BrowserId.NEXT_SEQUENTIAL, LauncherOptions(), ChromeOptions())

    /**
     * Launch a random temporary browser, the browser's user data dir is a random temporary dir.
     * */
    @Throws(BrowserLaunchException::class)
    override fun launchRandomTempBrowser(): Browser = launch(BrowserId.RANDOM_TEMP, LauncherOptions(), ChromeOptions())
}