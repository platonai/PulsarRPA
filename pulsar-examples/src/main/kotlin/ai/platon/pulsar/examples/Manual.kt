package ai.platon.pulsar.examples

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.withContext

class Manual(val session: PulsarSession) {
    val url = "https://list.jd.com/list.html?cat=652,12345,12349"

    /**
     * Load [url] if it's not in the database or it's expired
     * expire time: 1 day
     * */
    fun load() = session.load(url, "-expires 1d")

    /**
     * Load [url] if it's not in the database or it's expired, and then
     * load out pages specified by -outLink or they are expired
     *
     * portal page expire time: 1 day
     * out page expire time: 7 days
     * css query for out links in portal page: a[href~=item]
     * */
    fun loadOutPages() = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")

    /**
     * Load [url] if it's not in the database or it's expired, and then
     * scrape the fields in the page, all fields are restricted in a page section specified by restrictCss,
     * each field is specified by a css selector
     *
     * expire time: 1 day
     * restrict css selector: li[data-sku]
     * css selectors for fields: .p-name em, .p-price
     * */
    fun scrape() = session.scrape(url, "-expires 1d", "li[data-sku]", listOf(".p-name em", ".p-price"))

    fun scrape2() = session.scrape(url, "-i 1d", "li[data-sku]",
            mapOf("name" to ".p-name em", "price" to ".p-price"))

    fun scrapeOutPages(): List<Map<String, String?>> = session.scrapeOutPages(url,
            "-expires 1d -itemExpires 7s -outLink a[href~=item]",
            ".product-intro",
            listOf(".sku-name", ".p-price"))

    fun scrapeOutPages2(): List<Map<String, String?>> = session.scrapeOutPages(url,
            "-i 1d -ii 7d -ol a[href~=item]",
            ".product-intro",
            mapOf("name" to ".sku-name", "price" to ".p-price"))

    fun runAll() {
        println("Load:")
        load()
        println("Load out pages:")
        loadOutPages()

        println("Scrape:")
        println(scrape().joinToString("\n") { it[".p-price"] + " | " + it[".p-name em"] })
        println("Scrape 2:")
        println(scrape2().joinToString("\n") { it["price"] + " | " + it["name"] })

        println("Scrape out pages:")
        println(scrapeOutPages().joinToString("\n") { it[".p-price"] + " | " + it[".sku-name"] })
        println("Scrape out pages - 2:")
        println(scrapeOutPages2().joinToString("\n") { it["price"] + " | " + it["name"] })
    }
}

fun main() = withContext {
    Manual(it.createSession()).runAll()
}
