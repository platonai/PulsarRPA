package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowser
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowser

class BrowserFactory {

    @Throws(DriverLaunchException::class)
    fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val browser = when(browserId.browserType) {
            BrowserType.MOCK_CHROME -> MockBrowser(browserId, launcherOptions)
//            BrowserType.PLAYWRIGHT_CHROME -> PlaywrightBrowserInstance(instanceId, launcherOptions, launchOptions)
            else -> launchChromeDevtoolsBrowser(browserId, launcherOptions, launchOptions)
        }
        browser.registerShutdownHook()
        return browser
    }

    @Synchronized
    @Throws(DriverLaunchException::class)
    fun launchChromeDevtoolsBrowser(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): ChromeDevtoolsBrowser {
        val launcher = ChromeLauncher(options = launcherOptions)

        val chrome = launcher.runCatching { launch(launchOptions) }
            .getOrElse { throw DriverLaunchException("launch", it) }

        return ChromeDevtoolsBrowser(browserId, chrome, launcher)
    }
}
