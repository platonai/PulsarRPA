package ai.platon.pulsar.common.collect.collector

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.AbstractPriorityDataCollector
import ai.platon.pulsar.common.collect.FetchCache
import ai.platon.pulsar.common.collect.Loadable
import ai.platon.pulsar.common.collect.queue.LoadingQueue
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*

open class FetchCacheCollector(
    val fetchCache: FetchCache,
    priority: Int = Priority13.HIGHER.value,
) : AbstractPriorityDataCollector<UrlAware>(priority) {

    private val queues get() = fetchCache.queues

    override var name: String = "FCC"

    override val size: Int
        get() = fetchCache.size

    override val estimatedSize: Int
        get() = fetchCache.estimatedSize

    constructor(fetchCache: FetchCache, priority: Priority13) : this(fetchCache, priority.value)

    /**
     * If the fetch cache is a LoadingFetchCache, the items can be both in memory or in external source,
     * so even if all queues are empty, hasMore can return true
     * */
    @Synchronized
    override fun hasMore(): Boolean {
        if (size > 0) {
            return true
        }

        // size is 0, estimatedSize > 0, there are items in the database
        // estimatedSize is updated at least every 5 seconds
        // load actually performed at least every 5 seconds
        if (estimatedSize > 0 && fetchCache is Loadable<*>) {
            fetchCache.load()
        }

        return size > 0
    }

    @Synchronized
    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()
        val count = queues.sumOf { consume(it, sink) }
        return afterCollect(count)
    }

    private fun consume(queue: Queue<UrlAware>, sink: MutableCollection<UrlAware>): Int {
        if (queue is LoadingQueue && queue.size == 0 && queue.estimatedExternalSize == 0) {
            return 0
        }
        return queue.poll()?.takeIf { sink.add(it) }?.let { 1 } ?: 0
    }
}
