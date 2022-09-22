package ai.platon.pulsar.protocol.browser.driver.selenium

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import org.openqa.selenium.chrome.ChromeDriver
import org.slf4j.LoggerFactory

class SeleniumBrowser(
    id: BrowserId,
    browserSettings: BrowserSettings
): AbstractBrowser(id, browserSettings) {

    private val logger = LoggerFactory.getLogger(SeleniumBrowser::class.java)

    override fun newDriver(): WebDriver {
        return SeleniumDriver(browserSettings, this)
    }

    override fun close() {
    }
}
