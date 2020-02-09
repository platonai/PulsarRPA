package ai.platon.pulsar.browser.driver.chrome.examples

import com.google.gson.Gson

class EventsExample: BrowserExampleBase() {
    override fun run() {
        val page = devTools.page
        val network = devTools.network

        network.enable()
        page.enable()
        page.navigate(testUrl)

        println(Gson().toJson(chrome.getVersion()))
    }
}

fun main() {
    BlockUrlsExample().use { it.run() }
}
