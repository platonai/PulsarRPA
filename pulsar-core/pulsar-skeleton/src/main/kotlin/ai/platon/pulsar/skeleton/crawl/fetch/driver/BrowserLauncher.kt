package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

interface BrowserLauncher {
    fun connect(port: Int, settings: BrowserSettings = BrowserSettings()): Browser
    fun launch(browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions): Browser
}