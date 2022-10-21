package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.springframework.web.servlet.function.ServerResponse.async
import java.util.concurrent.CompletableFuture

/**
 * Demonstrates how to load pages in Kotlin flows.
 * */
internal object CrawlFlow {
    val session = PulsarContexts.createSession()

    fun loadAll() {
        LinkExtractors.fromResource("seeds10.txt").asSequence()
            .map(session::open).map(session::parse).map(FeaturedDocument::guessTitle)
            .forEach { println(it) }
    }

    suspend fun loadAllAsync2() {
        LinkExtractors.fromResource("seeds10.txt")
            .asFlow().flowOn(Dispatchers.Default)
            .map { "$it -i 1d" }
            .map { session.loadDeferred(it) }
            .map { session.parse(it) }
            .map { it.guessTitle() }
            .onEach { println(it) }
            .collect()
    }
}

fun main(args: Array<String>) {
    CrawlFlow.loadAll()
    runBlocking { CrawlFlow.loadAllAsync2() }
}
