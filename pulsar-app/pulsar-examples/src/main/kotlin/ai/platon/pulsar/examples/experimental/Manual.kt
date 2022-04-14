package ai.platon.pulsar.examples.experimental

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.context.PulsarContexts

class Manual(val session: PulsarSession = PulsarContexts.createSession()) {
    val url = "https://list.jd.com/list.html?cat=652,12345,12349"

    fun scrapeOutPages(): List<Map<String, String?>> = session.scrapeOutPages(url,
            "-expires 1d -itemExpires 7d -outLink a[href~=item]",
            ".product-intro",
            listOf(".sku-name", ".p-price"))

    fun scrapeOutPages2(): List<Map<String, String?>> = session.scrapeOutPages(url,
            "-i 1d -ii 7d -ol a[href~=item]",
            ".product-intro",
            mapOf("name" to ".sku-name", "price" to ".p-price"))

    fun runAll() {
        println("Scrape out pages:")
        println(scrapeOutPages().joinToString("\n") { it[".p-price"] + " | " + it[".sku-name"] })
        println("Scrape out pages - 2:")
        println(scrapeOutPages2().joinToString("\n") { it["price"] + " | " + it["name"] })
    }
}

fun main() {
    Manual().runAll()
}
