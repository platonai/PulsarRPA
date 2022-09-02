package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.StartStopRunnable
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

class CrawlLoops(val loops: MutableList<CrawlLoop>) : StartStopRunnable {
    companion object {
        val filters = mutableListOf<Predicate<CrawlLoop>>()
    }

    private val started = AtomicBoolean()

    val isStarted get() = started.get()

    constructor(loop: CrawlLoop): this(mutableListOf(loop))

    fun first() = loops.first()

    fun last() = loops.last()

    inline fun <reified T: CrawlLoop> firstIsInstance() = loops.filterIsInstance<T>().first()

    inline fun <reified T: CrawlLoop> lastIsInstance() = loops.filterIsInstance<T>().last()

    override fun start() {
        if (started.compareAndSet(false, true)) {
            loops.filter { loop -> filters.isEmpty() || filters.all { it.test(loop) } }
                .forEach { it.start() }
        }
    }

    override fun stop() {
        if (started.compareAndSet(true, false)) {
            loops.filter { loop -> filters.isEmpty() || filters.all { it.test(loop) } }
                .forEach { it.stop() }
        }
    }

    override fun await() {
        loops.forEach { it.await() }
    }
}
