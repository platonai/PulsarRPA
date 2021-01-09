package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.UrlAware
import java.util.concurrent.ConcurrentLinkedQueue

interface FetchCache {
    val nonReentrantQueue: MutableCollection<UrlAware>
    val nReentrantQueue: MutableCollection<UrlAware>
    val reentrantQueue: MutableCollection<UrlAware>
    val fetchQueues: Array<MutableCollection<UrlAware>> get() =
        arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    val totalSize get() = fetchQueues.sumOf { it.size }
}

enum class FetchQueueGroup { NonReentrant, NEntrant, Reentrant }

open class ConcurrentFetchCache(conf: ImmutableConfig): FetchCache {
    override val nonReentrantQueue = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantQueue = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantQueue = ConcurrentLinkedQueue<UrlAware>()
}

class LoadingFetchCache(
        val urlLoader: ExternalUrlLoader,
        val priority: Int = Priority13.NORMAL.value,
        val capacity: Int = 100_000,
        val conf: ImmutableConfig
) : FetchCache, Loadable<UrlAware> {
    override val nonReentrantQueue = ConcurrentNonReentrantLoadingQueue(urlLoader,
            FetchQueueGroup.NonReentrant.ordinal, priority, capacity)
    override val nReentrantQueue = ConcurrentNEntrantLoadingQueue(urlLoader, 3,
            FetchQueueGroup.NEntrant.ordinal, priority, capacity)
    override val reentrantQueue = ConcurrentLoadingQueue(urlLoader,
            FetchQueueGroup.Reentrant.ordinal, priority, capacity)
    override val fetchQueues: Array<MutableCollection<UrlAware>> get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)

    override fun load() {
        fetchQueues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load() }
    }

    override fun loadNow(): Collection<UrlAware> {
        return fetchQueues.filterIsInstance<Loadable<UrlAware>>().flatMap { it.loadNow() }
    }
}
