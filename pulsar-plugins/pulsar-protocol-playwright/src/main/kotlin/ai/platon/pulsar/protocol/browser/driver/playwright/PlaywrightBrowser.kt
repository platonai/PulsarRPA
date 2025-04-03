package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.runBlocking

class PlaywrightBrowser(
    id: BrowserId,
    settings: BrowserSettings,
) : AbstractBrowser(id, settings) {
    companion object {
        private val playwright = Playwright.create()
        private val browser = playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(false))
    }

    override fun newDriver(): WebDriver {
        return PlaywrightDriver(this, browser.newPage(), settings)
    }

    override fun newDriver(url: String): WebDriver {
        val driver = PlaywrightDriver(this, browser.newPage(), settings)
        runBlocking {
            driver.navigateTo(url)
            driver.waitForNavigation()
        }
        return driver
    }
}
