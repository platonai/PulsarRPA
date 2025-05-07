package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLaunchException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLauncher
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyContext
import java.util.Optional

class PlaywrightBrowserLauncher : BrowserLauncher {
    override fun connect(port: Int, settings: BrowserSettings): Browser {
        val browserId = BrowserId(PrivacyContext.RANDOM_TEMP_CONTEXT_DIR, BrowserType.PLAYWRIGHT_CHROME)
        try {
            val browserContext = PlaywrightBrowser.connectOverCDP(port)
            return PlaywrightBrowser(browserId, browserContext, settings)
        } catch (e: Exception) {
            throw BrowserLaunchException("Failed to launch browser | $browserId", e)
        }
    }

    @Throws(BrowserLaunchException::class)
    override fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): PlaywrightBrowser = launch0(browserId, launcherOptions, launchOptions)

    @Throws(BrowserLaunchException::class)
    private fun launch0(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): PlaywrightBrowser {
        require(browserId.browserType == BrowserType.PLAYWRIGHT_CHROME) {
            "Browser type must be PLAYWRIGHT_CHROME | $browserId"
        }

        val browserSettings = launcherOptions.browserSettings
        val browser = launchPlaywrightBrowser(browserId, launcherOptions, launchOptions)

        if (!browserSettings.isGUI) {
            // Web drivers are in GUI mode, please close it manually
            // browser.registerShutdownHook()
        }

        return browser
    }

    @Synchronized
    @Throws(BrowserLaunchException::class)
    private fun launchPlaywrightBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, chromeOptions: ChromeOptions
    ): PlaywrightBrowser {
        try {
            val options = toLaunchPersistentContextOptions(browserId, launcherOptions, chromeOptions)

            val context = PlaywrightBrowser.launchPersistentContext(browserId.userDataDir, options)
            return PlaywrightBrowser(browserId, context, launcherOptions.browserSettings)
        } catch (e: Exception) {
            throw BrowserLaunchException("Failed to launch browser | $browserId", e)
        }
    }

    private fun toLaunchPersistentContextOptions(
        browserId: BrowserId, launcherOptions: LauncherOptions, chromeOptions: ChromeOptions
    ): com.microsoft.playwright.BrowserType.LaunchPersistentContextOptions {
        val options = com.microsoft.playwright.BrowserType.LaunchPersistentContextOptions()
        options.headless = chromeOptions.headless
        val proxy = browserId.fingerprint.proxyEntry
        if (proxy != null) {
            options.proxy =
                com.microsoft.playwright.options.Proxy(proxy.toURI().toString()).setUsername(proxy.username)
                    .setPassword(proxy.password)
        }
        if (chromeOptions.noSandbox) {
            options.chromiumSandbox = false
        }

        options.ignoreHTTPSErrors = true
        // chromeOptions.ignoreCertificateErrors
        val settings = launcherOptions.browserSettings
        options.viewportSize = Optional.of(com.microsoft.playwright.options.ViewportSize(
            settings.viewportSize.width, settings.viewportSize.height
        ))

        return options
    }
}
