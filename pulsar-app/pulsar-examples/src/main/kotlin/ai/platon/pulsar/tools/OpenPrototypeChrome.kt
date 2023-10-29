package ai.platon.pulsar.tools

import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.ql.context.SQLContexts
import kotlinx.coroutines.runBlocking

fun main() {
    val driver = SQLContexts.create().getBean(WebDriverFactory::class).launchBrowser(BrowserId.PROTOTYPE).newDriver()

    runBlocking {
        driver.navigateTo("about:blank")
        driver.navigateTo("https://www.amazon.com")
    }

    readlnOrNull()
}
