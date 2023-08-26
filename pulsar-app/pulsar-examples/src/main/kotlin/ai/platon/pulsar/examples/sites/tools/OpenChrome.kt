package ai.platon.pulsar.examples.sites.tools

import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.ql.context.SQLContexts
import kotlinx.coroutines.runBlocking

fun main() {
    val driver = SQLContexts.create().getBean(WebDriverFactory::class).launchBrowser().newDriver()

    runBlocking {
        driver.navigateTo("about:blank")
    }

    readLine()
}
