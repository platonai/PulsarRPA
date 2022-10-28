package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.LinkedBlockingQueue

/**
 * Demonstrates how to load pages in Kotlin flows.
 * */
internal object CrawlCoroutines {
    private val session = PulsarContexts.createSession()
    private val scope = CoroutineScope(Dispatchers.Default) + CoroutineName("demo")

    suspend fun loadAllInCoroutines() {
        val jobs = LinkExtractors.fromResource("seeds10.txt")
            .map { scope.launch { session.loadDeferred("$it -expires 1s").also { println(it.url) } } }
        jobs.joinAll()
    }

    suspend fun loadAllInCoroutines2() {
        val deferredPages = LinkedBlockingQueue<Deferred<WebPage>>()
        val jobs = LinkExtractors.fromResource("seeds10.txt")
            .map { "$it -expires 1s -ignoreFailure" }
            .map { scope.launch { async { session.loadDeferred(it) }.also(deferredPages::add) } }

        // suspends current coroutine until all given jobs are complete.
        jobs.joinAll()
        deferredPages.map { it.await() }.forEach { println(it.url) }
    }
}

fun main() {
    println("== loadAllInCoroutines ==")
    runBlocking { CrawlCoroutines.loadAllInCoroutines() }

    println("== loadAllInCoroutines2 ==")
    runBlocking { CrawlCoroutines.loadAllInCoroutines2() }
}
