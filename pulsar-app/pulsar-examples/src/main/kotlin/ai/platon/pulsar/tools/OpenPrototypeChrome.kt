package ai.platon.pulsar.tools

import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.ql.context.SQLContexts
import kotlinx.coroutines.runBlocking

fun main() {
    val driver = DefaultBrowserFactory().launchPrototypeBrowser().newDriver()

    runBlocking {
        driver.navigateTo("about:blank")
        driver.navigateTo("https://www.amazon.com")
    }

    readlnOrNull()
}
