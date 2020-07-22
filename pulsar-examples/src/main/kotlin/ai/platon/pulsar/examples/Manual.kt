package ai.platon.pulsar.examples

import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.dom.select.firstText
import ai.platon.pulsar.dom.select.firstTextOrNull

class Manual(context: PulsarContext) {
    val session = context.createSession()
    val url = "https://list.jd.com/list.html?cat=652,12345,12349"

    fun load() = session.load(url, "-expires 1d")

    fun loadOutPages() = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")

    fun scrape() {
        val page = session.load(url, "-expires 1d")
        val document = session.parse(page)
        val products = document.select("li[data-sku]").map {
            it.firstText(".sku-name") to it.firstTextOrNull(".p-price")
        }
        products.forEach { (name, price) -> println("$price $name") }
    }

    fun scrapeChained() {
        session.load(url, "-expires 1d")
                .let { session.parse(it) }
                .select("li[data-sku]")
                .map { it.firstText(".sku-name") to it.firstTextOrNull(".p-price") }
                .forEach { (name, price) -> println("$price $name") }
    }

    fun scrapeOutPages() {
        val pages = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
        val documents = pages.map { session.parse(it) }
        val products = documents.mapNotNull { it.selectFirstOrNull(".product-intro") }.map {
            it.firstText(".sku-name") to it.firstTextOrNull(".p-price")
        }
        products.forEach { (name, price) -> println("$price $name") }
    }

    fun scrapeOutPagesChained() {
        session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
                .map { session.parse(it) }
                .mapNotNull { it.selectFirstOrNull(".product-intro") }
                .map { it.firstText(".sku-name") to it.firstTextOrNull(".p-price") }
                .forEach { (name, price) -> println("$price $name") }
    }

    fun run() {
        load()
        loadOutPages()
        scrape()
        scrapeOutPages()
    }
}

fun main() = withContext { Manual(it).run() }
