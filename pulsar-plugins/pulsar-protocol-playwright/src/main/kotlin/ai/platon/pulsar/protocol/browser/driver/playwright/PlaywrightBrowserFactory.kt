package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLaunchException
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyContext
import com.microsoft.playwright.Playwright

class PlaywrightBrowserFactory {
    companion object {
        private val playwright = Playwright.create()
    }

    private val logger = getLogger(this)

    fun connect(port: Int, settings: BrowserSettings = BrowserSettings()): Browser {
        val browserId = BrowserId(PrivacyContext.RANDOM_CONTEXT_DIR, BrowserType.PLAYWRIGHT_CHROME)
        try {
            val browser = playwright.chromium().connectOverCDP("http://localhost:$port")
            return PlaywrightBrowser(browserId, browser, settings)
        } catch (e: Exception) {
            throw BrowserLaunchException("Failed to launch browser | $browserId", e)
        }
    }

    @Throws(BrowserLaunchException::class)
    fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser = launch0(browserId, launcherOptions, launchOptions)

    @Throws(BrowserLaunchException::class)
    private fun launch0(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val browserSettings = launcherOptions.browserSettings
        val browser = when (browserId.browserType) {
//            BrowserType.PLAYWRIGHT_CHROME -> PlaywrightBrowserInstance(instanceId, launcherOptions, launchOptions)
            else -> launchPlaywrightBrowser(browserId, launcherOptions, launchOptions)
        }

        if (!browserSettings.isGUI) {
            // Web drivers are in GUI mode, please close it manually
            // browser.registerShutdownHook()
        }

        return browser
    }

    @Synchronized
    @Throws(BrowserLaunchException::class)
    private fun launchPlaywrightBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, browserOptions: ChromeOptions
    ): PlaywrightBrowser {
        try {
            val options = com.microsoft.playwright.BrowserType.LaunchPersistentContextOptions()
            if (browserOptions.headless) {
                options.headless = true
            }
            val proxy = browserId.fingerprint.proxyEntry
            if (proxy != null) {
                options.proxy =
                    com.microsoft.playwright.options.Proxy(proxy.toURI().toString()).setUsername(proxy.username)
                        .setPassword(proxy.password)
            }
            if (browserOptions.noSandbox) {
                options.chromiumSandbox = false
            }
            val browser = playwright.chromium().launchPersistentContext(browserId.userDataDir, options).browser()
            return PlaywrightBrowser(browserId, browser, launcherOptions.browserSettings)
        } catch (e: Exception) {
            throw BrowserLaunchException("Failed to launch browser | $browserId", e)
        }
    }
}