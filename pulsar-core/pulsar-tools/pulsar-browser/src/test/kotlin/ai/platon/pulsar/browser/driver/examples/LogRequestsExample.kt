package ai.platon.pulsar.browser.driver.examples

class LogRequestsExample: BrowserExampleBase() {
    override val testUrl: String = "https://www.stbchina.cn/"

    override fun run() {

        network.enable()
        page.enable()

        network.onRequestWillBeSent { event ->
            println(String.format("request: [%s] %s\n", event.request.method, event.request.url))
        }

        network.onResponseReceived { event ->
            if ("application/json" == event.response.mimeType) {
                println(String.format("response: [%s] %s", event.response.mimeType, event.response.url))
                if ("listChildrenCategoryWithNologin.do" in event.response.url) {
                    println(event.response.serviceWorkerResponseSource)
                }
            }
        }

        network.onLoadingFinished {
            // Close the tab and close the browser when loading finishes.
//            chrome.closeTab(tab)
//            launcher.close()
        }

        page.navigate(testUrl)
    }

    private fun isBlocked(url: String): Boolean {
        return url.endsWith(".png") || url.endsWith(".css")
    }
}

fun main() {
    LogRequestsExample().use { it.run() }
}
