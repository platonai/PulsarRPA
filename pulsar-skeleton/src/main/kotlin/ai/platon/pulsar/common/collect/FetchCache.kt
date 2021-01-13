package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.UrlAware
import java.util.concurrent.ConcurrentLinkedQueue

interface FetchCache {
    val nonReentrantQueue: MutableCollection<UrlAware>
    val nReentrantQueue: MutableCollection<UrlAware>
    val reentrantQueue: MutableCollection<UrlAware>
    val fetchQueues: Array<MutableCollection<UrlAware>>
        get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    val totalSize get() = fetchQueues.sumOf { it.size }
}

open class ConcurrentFetchCache(conf: ImmutableConfig): FetchCache {
    override val nonReentrantQueue = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantQueue = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantQueue = ConcurrentLinkedQueue<UrlAware>()
}

class LoadingFetchCache(
        val urlLoader: ExternalUrlLoader,
        val priority: Int = Priority13.NORMAL.value,
        val capacity: Int = 1_000
) : FetchCache, Loadable<UrlAware> {

    companion object {
        const val G_NON_REENTRANT = 1
        const val G_N_ENTRANT = 2
        const val G_REENTRANT = 3
    }

    override val nonReentrantQueue = ConcurrentNonReentrantLoadingQueue(urlLoader, G_NON_REENTRANT, priority, capacity)
    override val nReentrantQueue = ConcurrentNEntrantLoadingQueue(urlLoader, 3, G_N_ENTRANT, priority, capacity)
    override val reentrantQueue = ConcurrentLoadingQueue(urlLoader, G_REENTRANT, priority, capacity)
    override val fetchQueues: Array<MutableCollection<UrlAware>>
        get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)

    override fun load() {
        fetchQueues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load() }
    }

    override fun loadNow(): Collection<UrlAware> {
        return fetchQueues.filterIsInstance<Loadable<UrlAware>>().flatMap { it.loadNow() }
    }
}
