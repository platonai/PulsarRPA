package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import com.microsoft.playwright.Playwright

class PlaywrightBrowser(
    id: BrowserInstanceId,
    browserSettings: BrowserSettings,
): AbstractBrowser(id, browserSettings) {
    companion object {
        private val createOptions = Playwright.CreateOptions().setEnv(mutableMapOf("PWDEBUG" to "0"))
        private val playwright = Playwright.create(createOptions)

        init {
            // System.setProperty("playwright.cli.dir", Paths.get("/tmp/playwright-java").toString())
        }
    }

    internal val actualBrowser: com.microsoft.playwright.Browser by lazy {
        playwright.chromium().launch()
    }

    override fun newDriver(): WebDriver {
        return PlaywrightDriver(browserSettings, this)
    }
}
