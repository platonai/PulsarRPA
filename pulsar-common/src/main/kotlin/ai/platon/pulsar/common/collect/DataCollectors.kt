package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.Priority
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
        priority: Priority = Priority.NORMAL
): AbstractPriorityDataCollector<E>(priority) {

    override var name = "MultiSourceDC"

    private val isActive get() = AppContext.isActive
    private val roundCounter = AtomicInteger()
    private val collectedCounter = AtomicInteger()

    val round get() = roundCounter.get()
    val totalCollected get() = collectedCounter.get()

    constructor(vararg thatCollectors: PriorityDataCollector<E>, priority: Priority = Priority.NORMAL):
            this(arrayListOf(*thatCollectors), priority)

    override fun hasMore() = collectors.any { it.hasMore() }

    override fun collectTo(sink: MutableList<E>): Int {
        roundCounter.incrementAndGet()

        var collected = 0
        val sortedCollectors = collectors.sortedBy { it.priority }
        while (isActive && collected == 0 && hasMore()) {
            sortedCollectors.forEach {
                if (isActive && collected == 0 && it.hasMore()) {
                    collected += if (it.priority >= Priority.HIGHEST.value) {
                        collectTo(0, it, sink)
                    } else {
                        it.collectTo(sink)
                    }
                }
            }
        }

        return collected
    }

    private fun collectTo(index: Int, collector: DataCollector<E>, sink: MutableList<E>): Int {
        val list = mutableListOf<E>()
        collector.collectTo(list)
        sink.addAll(index, list)
        return list.size
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
        priority: Priority = Priority.LOWEST
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
