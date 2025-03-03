package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.protocol.browser.impl.BrowserFactoryImpl
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLaunchException
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

/**
 * A factory to create browser instances.
 * */
class BrowserFactory {
    private val factory = BrowserFactoryImpl()

    /**
     * Connect to a browser instance, the browser instance should be open with Chrome devtools open.
     * */
    fun connect(port: Int, settings: BrowserSettings = BrowserSettings()): Browser = factory.connect(port, settings)

    /**
     * Launch the system default browser, the system default browser is your daily used browser.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchSystemDefaultBrowser(): Browser = factory.launch(BrowserId.SYSTEM_DEFAULT, LauncherOptions(), ChromeOptions())

    /**
     * Launch the default browser, notice, the default browser is not the one you used daily.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchDefaultBrowser(): Browser = factory.launch(BrowserId.DEFAULT, LauncherOptions(), ChromeOptions())

    /**
     * Launch the prototype browser, the prototype browser is a browser instance with default settings.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchPrototypeBrowser(): Browser = factory.launch(BrowserId.PROTOTYPE, LauncherOptions(), ChromeOptions())

    /**
     * Launch the next sequential browser, the browser's user data dir rotates between a group of dirs.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchNextSequentialBrowser(): Browser =
        factory.launch(BrowserId.NEXT_SEQUENTIAL, LauncherOptions(), ChromeOptions())

    /**
     * Launch a random temporary browser, the browser's user data dir is a random temporary dir.
     * */
    @Throws(BrowserLaunchException::class)
    fun launchRandomTempBrowser(): Browser = factory.launch(BrowserId.RANDOM, LauncherOptions(), ChromeOptions())

    /**
     * Launch a browser with the given browser id, the browser id is used to identify the browser instance.
     * */
    @Throws(BrowserLaunchException::class)
    fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser = factory.launch(browserId, launcherOptions, launchOptions)
}
