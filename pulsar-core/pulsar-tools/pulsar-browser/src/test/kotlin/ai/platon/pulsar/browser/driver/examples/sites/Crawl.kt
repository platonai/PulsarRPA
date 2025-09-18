package ai.platon.pulsar.browser.driver.examples.sites

import ai.platon.pulsar.browser.driver.examples.BrowserExampleBase

class Crawler: BrowserExampleBase() {

    override val testUrl = "https://ly.simuwang.com/"

    override fun run() {
        network.setBlockedURLs(listOf("*fireyejs*"))
        network.enable()

        page.addScriptToEvaluateOnNewDocument(preloadJs)
        page.enable()

        page.navigate(testUrl)
    }
}

fun main() {
    Crawler().use { it.run() }
}
