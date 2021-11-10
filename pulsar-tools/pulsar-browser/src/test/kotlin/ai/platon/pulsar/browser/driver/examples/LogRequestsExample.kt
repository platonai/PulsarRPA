package ai.platon.pulsar.browser.driver.examples

class LogRequestsExample: BrowserExampleBase() {
    override fun run() {
        network.onRequestWillBeSent { event ->
            println(String.format("request: [%s] %s\n", event.request.method, event.request.url))
        }
        network.onLoadingFinished {
            // Close the tab and close the browser when loading finishes.
//            chrome.closeTab(tab)
//            launcher.close()
        }

        network.enable()
        page.enable()

        page.navigate(testUrl)
        page.resetNavigationHistory()
        page.navigate("https://list.jd.com/list.html?cat=670,677,688")
    }

    private fun isBlocked(url: String): Boolean {
        return url.endsWith(".png") || url.endsWith(".css")
    }
}

fun main() {
    LogRequestsExample().use { it.run() }
}
