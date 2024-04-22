package ai.platon.pulsar.examples.sites.article

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.PulsarContexts

class EEO {
    fun scrape() {
        val url = "https://www.eeo.com.cn/2024/0330/648712.shtml -i 1s"
        val session = PulsarContexts.createSession()
        val document = session.harvest(url)
        
        println(document.contentTitle)
        println(document.textContent)
    }
}

fun main() {
    BrowserSettings.withSystemDefaultBrowser()
    EEO().scrape()
}
