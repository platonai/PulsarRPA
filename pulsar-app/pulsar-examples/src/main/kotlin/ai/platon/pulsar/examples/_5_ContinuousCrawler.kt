package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

/**
 * Demonstrates continuous crawls.
 * */
fun main() {
    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        // do something wonderful with the document
        println(document.title + "\t|\t" + document.baseUri)

        // extract more links from the document
        context.submitAll(document.selectHyperlinks("a[href~=/dp/]"))
    }

    val urls = LinkExtractors.fromResource("seeds10.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls).await()
}
