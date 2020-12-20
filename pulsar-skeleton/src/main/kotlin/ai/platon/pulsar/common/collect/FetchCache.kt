package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

interface FetchCache {
    val nonReentrantFetchUrls: Queue<UrlAware>
    val nReentrantFetchUrls: Queue<UrlAware>
    val reentrantFetchUrls: Queue<UrlAware>
    val fetchUrlQueues: Array<Queue<UrlAware>> get() =
        arrayOf(nonReentrantFetchUrls, nReentrantFetchUrls, reentrantFetchUrls)
    val totalSize get() = fetchUrlQueues.sumOf { it.size }
}

open class ConcurrentFetchCache(conf: ImmutableConfig): FetchCache {
    override val nonReentrantFetchUrls = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantFetchUrls = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantFetchUrls = ConcurrentLinkedQueue<UrlAware>()
}

enum class FetchQueueGroup { NonReentrant, NEntrant, Reentrant }

class LoadingFetchCache(
        val urlLoader: ExternalUrlLoader,
        val priority: Int,
        val conf: ImmutableConfig
) : FetchCache {
    override val nonReentrantFetchUrls = ConcurrentNonReentrantLoadingUrlQueue(urlLoader, FetchQueueGroup.NonReentrant.ordinal)
    override val nReentrantFetchUrls = ConcurrentNEntrantLoadingUrlQueue(urlLoader, 3, FetchQueueGroup.NEntrant.ordinal)
    override val reentrantFetchUrls = ConcurrentReentrantLoadingUrlQueue(urlLoader, FetchQueueGroup.Reentrant.ordinal)
    override val fetchUrlQueues: Array<Queue<UrlAware>> get() = arrayOf(nonReentrantFetchUrls, nReentrantFetchUrls, reentrantFetchUrls)

    fun load() {
        fetchUrlQueues.filterIsInstance<LoadingUrlQueue>().forEach { it.load() }
    }
}
