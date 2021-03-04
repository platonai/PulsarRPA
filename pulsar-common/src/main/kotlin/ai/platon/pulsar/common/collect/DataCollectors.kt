package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.sleep
import ai.platon.pulsar.common.url.CrawlableFatLink
import ai.platon.pulsar.common.url.FatLink
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

interface CrawlableFatLinkCollector {
    val fatLinks: Map<String, CrawlableFatLink>

    fun remove(url: String): CrawlableFatLink?
    fun removeAll(urls: Iterable<String>): Int = urls.count { remove(it) != null }

    fun remove(fatLink: FatLink): CrawlableFatLink?
    fun removeAll(fatLinks: List<FatLink>): Int = fatLinks.count { remove(it) != null }
}

open class MultiSourceDataCollector<E>(
        val collectors: MutableList<PriorityDataCollector<E>>,
        priority: Priority13 = Priority13.NORMAL
): AbstractPriorityDataCollector<E>(priority) {

    override var name = "MultiSourceDC"

    private val roundCounter = AtomicInteger()
    private val collectedCounter = AtomicInteger()

    val round get() = roundCounter.get()
    val totalCollected get() = collectedCounter.get()

    constructor(vararg thatCollectors: PriorityDataCollector<E>, priority: Priority13 = Priority13.NORMAL):
            this(arrayListOf(*thatCollectors), priority)

    override fun hasMore() = collectors.any { it.hasMore() }

    /**
     * Collect items from delegated collectors to the sink.
     * If a delegated collector has priority >= Priority.HIGHEST, the items are added to the front of the sink,
     * or the items are appended to the back of the sink.
     *
     * All collectors with the same priority have the same chance to choose
     * */
    override fun collectTo(sink: MutableList<E>): Int {
        roundCounter.incrementAndGet()
        return collectTo0(sink)
    }

    private fun collectTo0(sink: MutableList<E>): Int {
        var collected = 0
        // The returned map preserves the entry iteration order of the keys produced from the original collection.
        val groupedCollectors = collectors.sortedBy { it.priority }.groupBy { it.priority }
        while (collected == 0 && hasMore()) {
            groupedCollectors.forEach { (_, collectors) ->
                // collectors with the same priority have the same chance to choose
                collectors.shuffled().forEach {
                    if (collected == 0 && it.hasMore()) {
                        collected += if (it.priority >= Priority13.HIGHEST.value) {
                            it.collectTo(0, sink)
                        } else {
                            it.collectTo(sink)
                        }
                    }
                }
            }
        }
        return collected
    }
}

/**
 * A infinite multi source data collector, the collector always has a chance to collect the next items,
 * and if no item actually collected, wait for a while
 * */
open class PauseDataCollector<E>(
        val nilElement: E,
        val n: Int = 1,
        val pause: Duration = Duration.ofSeconds(1),
        val sleeper: () -> Unit = { sleep(pause) },
        priority: Priority13 = Priority13.LOWEST
): AbstractPriorityDataCollector<E>(priority) {
    override var name: String = "PauseDC"

    private var collected = 0

    override fun hasMore(): Boolean {
        return collected < n
    }

    override fun collectTo(sink: MutableList<E>): Int {
        sleeper()

        sink.add(nilElement)
        ++collected

        return 1
    }
}
