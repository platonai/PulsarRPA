package ai.platon.pulsar.browser.driver.chrome.examples

import com.google.gson.Gson

class BlockUrlsExample: BrowserExampleBase() {
    fun isBlocked(url: String): Boolean {
        return url.endsWith(".png") || url.endsWith(".css")
    }

    override fun run() {
        val page = devTools.page
        val network = devTools.network

        network.setBlockedURLs(listOf("*.png", "*.css"))
        page.onLoadEventFired { devTools.close() }
        network.enable()
        page.enable()
        page.navigate(testUrl)

        println(Gson().toJson(chrome.getVersion()))
    }
}

fun main() {
    BlockUrlsExample().use { it.run() }
}
