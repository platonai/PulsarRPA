package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document

fun main() {
    val parseHandler = { _: WebPage, document: Document ->
        // do something wonderful with the document
        println(document.title() + "\t|\t" + document.baseUri())
    }
    val urls = LinkExtractors.fromResource("seeds10.txt").map { ParsableHyperlink(it, parseHandler) }
    val context = PulsarContexts.create().asyncLoadAll(urls)
    // feel free to fetch/load any amount of urls here using async loading methods
    // ...
    context.await()
}
