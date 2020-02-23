package ai.platon.pulsar.browser.driver.examples

class LogRequestsExample: BrowserExampleBase() {
    override fun run() {
        val page = devTools.page
        val network = devTools.network

        // Log requests with onRequestWillBeSent event handler.
        network.onRequestWillBeSent { event ->
            System.out.printf("request: %s %s%s", event.request.method, event.request.url, System.lineSeparator())
        }
        network.onLoadingFinished {
            // Close the tab and close the browser when loading finishes.
            chrome.closeTab(tab)
            launcher.close()
        }

        network.enable()
        // Enable page events.
        page.enable()
        // Navigate to baidu.com.
        page.navigate(testUrl)
    }

    private fun isBlocked(url: String): Boolean {
        return url.endsWith(".png") || url.endsWith(".css")
    }
}

fun main() {
    LogRequestsExample().use { it.run() }
}
