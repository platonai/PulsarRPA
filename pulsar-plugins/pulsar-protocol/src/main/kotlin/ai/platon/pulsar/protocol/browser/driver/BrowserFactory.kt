package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessException
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowser
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserLaunchException
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

class BrowserFactory {
    private val logger = getLogger(this)
    
    fun connect(port: Int, browserSettings: BrowserSettings = BrowserSettings()): Browser {
        return ChromeDevtoolsBrowser(port, browserSettings = browserSettings)
    }
    
    @Throws(BrowserLaunchException::class)
    fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val browserSettings = launcherOptions.browserSettings
        val browser = when (browserId.browserType) {
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
    
    @Throws(BrowserLaunchException::class)
    private fun createMockBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, browserOptions: ChromeOptions
    ): MockBrowser {
        val backupFingerprint = Fingerprint(BrowserType.PULSAR_CHROME)
        val backupBrowserId = BrowserId(browserId.contextDir, backupFingerprint)
        val browserSettings = launcherOptions.browserSettings
        return MockBrowser(
            browserId,
            browserSettings,
            launchChromeDevtoolsBrowser(backupBrowserId, launcherOptions, browserOptions)
        )
    }
    
    @Synchronized
    @Throws(BrowserLaunchException::class)
    private fun launchChromeDevtoolsBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, browserOptions: ChromeOptions
    ): ChromeDevtoolsBrowser {
        val launcher = ChromeLauncher(userDataDir = browserId.userDataDir, options = launcherOptions)
        
        try {
            val chrome = launcher.launch(browserOptions)
            return ChromeDevtoolsBrowser(browserId, chrome, launcherOptions.browserSettings, launcher)
        } catch (e: ChromeProcessException) {
            logger.warn("Failed to launch browser", e)
            throw BrowserLaunchException("Failed to launch browser | $browserId")
        }
    }
}
