package ai.platon.pulsar.browser.driver.examples

import com.google.gson.Gson

class EventsExample: BrowserExampleBase() {
    override fun run() {
        val page = devTools.page
        val network = devTools.network

        network.enable()
        page.enable()
        page.navigate(testUrl)
    }
}

fun main() {
    BlockUrlsExample().use { it.run() }
}
