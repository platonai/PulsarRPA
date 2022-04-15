package ai.platon.pulsar.examples

import ai.platon.pulsar.context.PulsarContexts
import com.google.gson.Gson

fun main() {
    // create a pulsar session
    val session = PulsarContexts.createSession()
    // the main url we are playing with
    val url = "https://list.jd.com/list.html?cat=652,12345,12349"
    // load a page, fetch it from the web if it has expired or if it's being fetched for the first time
    val page = session.load(url, "-expires 1d")
    // parse the page content into a Jsoup document
    val document = session.parse(page)
    // do something with the document
    // ...

    // or, load and parse
    val document2 = session.loadDocument(url, "-expires 1d")
    // do something with the document
    // ...

    // load all pages with links specified by -outLink
    val pages = session.loadOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
    // load, parse and scrape fields
    val fields = session.scrape(url, "-expires 1d", "li[data-sku]", listOf(".p-name em", ".p-price"))
    // load, parse and scrape named fields
    val fields2 = session.scrape(url, "-i 1d", "li[data-sku]", mapOf("name" to ".p-name em", "price" to ".p-price"))

    println("== document")
    println(document.title)
    println(document.selectFirst("title").text())

    println("== document2")
    println(document2.title)
    println(document2.selectFirst("title").text())

    println("== pages")
    println(pages.map { it.url })

    val gson = Gson()
    println("== fields")
    println(gson.toJson(fields))

    println("== fields2")
    println(gson.toJson(fields2))
}
