package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.BrowserLaunchException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowser
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException

class BrowserFactory {
    private val logger = getLogger(this)

    @Throws(BrowserLaunchException::class)
    fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val browserSettings = launcherOptions.browserSettings
        val browser = when(browserId.browserType) {
            BrowserType.MOCK_CHROME -> createMockBrowser(browserId, launcherOptions, launchOptions)
//            BrowserType.PLAYWRIGHT_CHROME -> PlaywrightBrowserInstance(instanceId, launcherOptions, launchOptions)
            else -> launchChromeDevtoolsBrowser(browserId, launcherOptions, launchOptions)
        }

        if (!browserSettings.isGUI) {
            // Web drivers are in GUI mode, please close it manually
            // browser.registerShutdownHook()
        }

        return browser
    }

    private fun createMockBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): MockBrowser {
        val backupFingerprint = Fingerprint(BrowserType.PULSAR_CHROME)
        val backupBrowserId = BrowserId(browserId.contextDir, backupFingerprint)
        val browserSettings = launcherOptions.browserSettings
        return MockBrowser(browserId,
            browserSettings,
            launchChromeDevtoolsBrowser(backupBrowserId, launcherOptions, launchOptions))
    }

    @Synchronized
    @Throws(BrowserLaunchException::class)
    private fun launchChromeDevtoolsBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): ChromeDevtoolsBrowser {
        val launcher = ChromeLauncher(userDataDir = browserId.userDataDir, options = launcherOptions)

        try {
            val chrome = launcher.launch(launchOptions)
            return ChromeDevtoolsBrowser(browserId, chrome, launcher)
        } catch (e: WebDriverException) {
            logger.warn("Failed to launch browser", e)
            throw BrowserLaunchException("Failed to launch browser | $browserId")
        }
    }
}
