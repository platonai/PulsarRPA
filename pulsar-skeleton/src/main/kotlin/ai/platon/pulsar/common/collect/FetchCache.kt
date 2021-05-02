package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

interface FetchCache {
    val name: String
    val nonReentrantQueue: Queue<UrlAware>
    val nReentrantQueue: Queue<UrlAware>
    val reentrantQueue: Queue<UrlAware>
    val queues: Array<Queue<UrlAware>>
        get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    val size get() = queues.sumOf { it.size }
    val estimatedSize get() = queues.sumOf { it.size }
}

open class ConcurrentFetchCache(
    override val name: String = ""
) : FetchCache {
    override val nonReentrantQueue = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantQueue = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantQueue = ConcurrentLinkedQueue<UrlAware>()
}

class LoadingFetchCache(
    /**
     * The cache name, a loading fetch cache requires a unique name
     * */
    override val name: String = "",
    /**
     * The external url loader
     * */
    val urlLoader: ExternalUrlLoader,
    /**
     * The priority
     * */
    val priority: Int,
    /**
     * The cache capacity
     * */
    val capacity: Int = LoadingQueue.DEFAULT_CAPACITY,
) : FetchCache, Loadable<UrlAware> {

    companion object {
        const val G_NON_REENTRANT = 1
        const val G_N_ENTRANT = 2
        const val G_REENTRANT = 3
    }

    override val nonReentrantQueue =
        ConcurrentNonReentrantLoadingQueue(urlLoader, UrlGroup(name, G_NON_REENTRANT, priority), capacity)
    override val nReentrantQueue =
        ConcurrentNEntrantLoadingQueue(urlLoader, UrlGroup(name, G_N_ENTRANT, priority), 3, capacity)
    override val reentrantQueue = ConcurrentLoadingQueue(urlLoader, UrlGroup(name, G_REENTRANT, priority), capacity)
    override val queues: Array<Queue<UrlAware>>
        get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    override val size get() = queues.sumOf { it.size }
    override val estimatedSize: Int
        get() = size + queues.filterIsInstance<LoadingQueue<UrlAware>>().sumOf { it.estimatedExternalSize }

    override fun load() {
        queues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load() }
    }

    override fun loadNow(): Collection<UrlAware> {
        return queues.filterIsInstance<Loadable<UrlAware>>().flatMap { it.loadNow() }
    }
}
