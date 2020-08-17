package ai.platon.pulsar.examples

import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.withContext

class Manual(context: PulsarContext) {
    val session = context.createSession()
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

    fun scrape1() = session.scrape(url, "-i 1d", "li[data-sku]",
            mapOf("name" to ".p-name em", "price" to ".p-price"))

    fun scrapeOutPages(): List<Map<String, String?>> = session.scrapeOutPages(url,
            "-expires 1d -itemExpires 7d -outLink a[href~=item]",
            ".product-intro",
            listOf(".sku-name", ".p-price"))

    fun scrapeOutPages2(): List<Map<String, String?>> = session.scrapeOutPages(url,
            "-i 1d -ii 7d -ol a[href~=item]",
            ".product-intro",
            mapOf("name" to ".sku-name", "price" to ".p-price"))

    fun runAll() {
        val fmt = "price: %-20s name: %s"

        println("== LOAD ==")
        load()
        println("\n== loadOutPages ==")
        loadOutPages()

        println("\n== scrape ==")
        scrape().map { String.format(fmt, it[".p-price"], it[".p-name em"]) }.forEach { println(it) }
        println("\n== scrape1 ==")
        scrape1().map { String.format(fmt, it["price"], it["name"]) }.forEach { println(it) }

        println("\n== scrapeOutPages ==")
        scrapeOutPages().map { String.format(fmt, it[".p-price"], it[".sku-name"]) }.forEach { println(it) }
        println("\n== scrapeOutPages2 ==")
        scrapeOutPages2().map { String.format(fmt, it["price"], it["name"]) }.forEach { println(it) }
    }
}

fun main() = withContext { Manual(it).runAll() }
