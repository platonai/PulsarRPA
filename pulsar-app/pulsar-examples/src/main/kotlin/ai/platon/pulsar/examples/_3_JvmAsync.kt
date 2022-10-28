package ai.platon.pulsar.examples

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.context.PulsarContexts.createSession
import ai.platon.pulsar.dom.FeaturedDocument
import java.util.concurrent.CompletableFuture

/**
 * Demonstrates how to load pages using Java-style asynchronous calls.
 * */
internal object JvmAsync {
    val session = createSession()

    fun loadAll() {
        LinkExtractors.fromResource("seeds10.txt").parallelStream()
            .map(session::load).map(session::parse).map(FeaturedDocument::guessTitle)
            .forEach { println(it) }
    }

    fun loadAllAsync2() {
        val futures = LinkExtractors.fromResource("seeds10.txt")
            .asSequence()
            .map { "$it -i 1d" }
            .map { session.loadAsync(it) }
            .map { it.thenApply { session.parse(it) } }
            .map { it.thenApply { it.guessTitle() } }
            .map { it.thenAccept { println(it) } }
            .toList()
            .toTypedArray()
        CompletableFuture.allOf(*futures).join()
    }

    fun loadAllAsync3() {
        val futures = session.loadAllAsync(LinkExtractors.fromResource("seeds10.txt"))
            .map { it.thenApply { session.parse(it) } }
            .map { it.thenApply { it.guessTitle() } }
            .map { it.thenAccept { println(it) } }
            .toTypedArray()
        CompletableFuture.allOf(*futures).join()
    }

    fun loadAllAsync4() {
        val futures = session.loadAllAsync(LinkExtractors.fromResource("seeds10.txt"))
            .map { it.thenApply { session.parse(it) }.thenApply { it.guessTitle() }.thenAccept { println(it) } }
            .toTypedArray()
        CompletableFuture.allOf(*futures).join()
    }
}

fun main() {
    JvmAsync.loadAll()
    JvmAsync.loadAllAsync2()
    JvmAsync.loadAllAsync3()
    JvmAsync.loadAllAsync4()
}
