package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.StartStopRunnable
import java.util.function.Predicate

class CrawlLoops(val loops: MutableList<CrawlLoop>) : StartStopRunnable {
    companion object {
        val filters = mutableListOf<Predicate<CrawlLoop>>()
    }

    fun first() = loops.first()

    fun last() = loops.last()

    inline fun <reified T: CrawlLoop> firstIsInstance() = loops.filterIsInstance<T>().first()

    inline fun <reified T: CrawlLoop> lastIsInstance() = loops.filterIsInstance<T>().last()

    override fun start() {
        loops.filter { loop -> filters.isEmpty() || filters.all { it.test(loop) } }
            .forEach { it.start() }
    }

    override fun stop() {
        loops.filter { loop -> filters.isEmpty() || filters.all { it.test(loop) } }
            .forEach { it.stop() }
    }
}
