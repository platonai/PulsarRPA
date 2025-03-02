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
    private val impl = BrowserFactoryImpl()

    fun connect(port: Int, settings: BrowserSettings = BrowserSettings()): Browser = impl.connect(port, settings)

    @Throws(BrowserLaunchException::class)
    fun launchSystemDefaultBrowser(): Browser = impl.launch(BrowserId.SYSTEM_DEFAULT, LauncherOptions(), ChromeOptions())

    @Throws(BrowserLaunchException::class)
    fun launchDefaultBrowser(): Browser = impl.launch(BrowserId.DEFAULT, LauncherOptions(), ChromeOptions())

    @Throws(BrowserLaunchException::class)
    fun launchPrototypeBrowser(): Browser = impl.launch(BrowserId.PROTOTYPE, LauncherOptions(), ChromeOptions())

    @Throws(BrowserLaunchException::class)
    fun launchNextSequentialTempBrowser(): Browser =
        impl.launch(BrowserId.NEXT_SEQUENTIAL, LauncherOptions(), ChromeOptions())

    @Throws(BrowserLaunchException::class)
    fun launchRandomTempBrowser(): Browser = impl.launch(BrowserId.RANDOM, LauncherOptions(), ChromeOptions())

    @Throws(BrowserLaunchException::class)
    fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser = impl.launch(browserId, launcherOptions, launchOptions)
}
