package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document

fun main() {
    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: Document ->
        context.submitAll(document.selectHyperlinks("a[href~=/dp/]"))
    }
    val urls = LinkExtractors.fromResource("seeds10.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls).await()
}
