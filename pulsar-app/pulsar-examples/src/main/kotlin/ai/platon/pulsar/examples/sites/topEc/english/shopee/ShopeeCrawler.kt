package ai.platon.pulsar.examples.sites.topEc.english.shopee

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.PulsarContexts

fun main() {
    BrowserSettings.withSystemDefaultBrowser()
    
    val portalUrl = "https://shopee.sg/Computers-Peripherals-cat.11013247"
    val args = "-i 1s -ii 5d -ol a[href~=sp_atk] -tl 20 -ignoreFailure"
    val session = PulsarContexts.createSession()

//    val fieldSelectors = listOf("._2Csw3W", "._3uBhVI", "._3b2Btx", "._1kpF5Y")
    val fieldSelectors = mapOf(
        "title" to "._2Csw3W",
        "price" to "._3uBhVI",
        "star" to "._3b2Btx",
        "ratings" to "._1kpF5Y"
    )
    val fields = session.scrapeOutPages(portalUrl, args, fieldSelectors)
    println(fields)
}
