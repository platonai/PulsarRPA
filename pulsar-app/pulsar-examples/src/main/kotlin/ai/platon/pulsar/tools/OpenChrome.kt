package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import kotlinx.coroutines.runBlocking

fun main() {
    val driver = DefaultBrowserFactory().launchDefaultBrowser().newDriver()

    runBlocking {
        driver.navigateTo("about:blank")
        driver.navigateTo("https://www.amazon.in")
    }

    readlnOrNull()
}
