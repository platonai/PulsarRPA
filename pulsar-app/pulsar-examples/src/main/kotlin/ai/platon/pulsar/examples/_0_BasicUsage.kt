package ai.platon.pulsar.examples

import ai.platon.pulsar.context.PulsarContexts
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

/**
 * Demonstrates the very basic usage of Pulsar.
 * */
fun main() {
    // Create a pulsar session
    val session = PulsarContexts.createSession()
    // The main url we are playing with
    val url = "https://www.amazon.com/dp/B0C1H26C46"

    // Load a page from local storage, or fetch it from the Internet if it does not exist or has expired
    val page = session.load(url, "-expires 10s")

    // Submit a url to the URL pool, the submitted url will be processed in a crawl loop
    session.submit(url, "-expires 10s")

    // Parse the page content into a document
    val document = session.parse(page)
    // do something with the document
    // ...

    // Load and parse
    val document2 = session.loadDocument(url, "-expires 10s")
    // do something with the document
    // ...

    // Load the portal page and then load all links specified by `-outLink`.
    // Option `-outLink` specifies the cssSelector to select links in the portal page to load.
    // Option `-topLinks` specifies the maximal number of links selected by `-outLink`.
    val pages = session.loadOutPages(url, "-expires 10s -itemExpires 10s -outLink a[href~=/dp/] -topLinks 10")

    // Load the portal page and submit the out links specified by `-outLink` to the URL pool.
    // Option `-outLink` specifies the cssSelector to select links in the portal page to submit.
    // Option `-topLinks` specifies the maximal number of links selected by `-outLink`.
    session.submitForOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=/dp/] -topLinks 10")

    // Load, parse and scrape fields
    val fields = session.scrape(url, "-expires 10s", "#centerCol",
        listOf("#title", "#acrCustomerReviewText"))

    // Load, parse and scrape named fields
    val fields2 = session.scrape(url, "-i 10s", "#centerCol",
        mapOf("title" to "#title", "reviews" to "#acrCustomerReviewText"))

    // Load, parse and scrape named fields
    val fields3 = session.scrapeOutPages(url, "-i 10s -ii 10s -outLink a[href~=/dp/] -topLink 10", "#centerCol",
        mapOf("title" to "#title", "reviews" to "#acrCustomerReviewText"))

    // Add `-parse` option to activate the parsing subsystem
    val page10 = session.load(url, "-parse -expires 10s")

    // Kotlin suspend calls
    val page11 = runBlocking { session.loadDeferred(url, "-expires 10s") }

    // Java-style async calls
    session.loadAsync(url, "-expires 10s").thenApply(session::parse).thenAccept(session::export)

    println("== document")
    println(document.title)
    println(document.selectFirstOrNull("title")?.text())

    println("== document2")
    println(document2.title)
    println(document2.selectFirstOrNull("title")?.text())

    println("== pages")
    println(pages.map { it.url })

    val gson = Gson()
    println("== fields")
    println(gson.toJson(fields))

    println("== fields2")
    println(gson.toJson(fields2))

    println("== fields3")
    println(gson.toJson(fields3))

    // Wait until all tasks are done.
    session.context.await()
}
