package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

interface FetchCache {
    val name: String
    val nonReentrantQueue: Queue<UrlAware>
    val nReentrantQueue: Queue<UrlAware>
    val reentrantQueue: Queue<UrlAware>
    val queues: List<Queue<UrlAware>>
        get() = listOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    val size get() = queues.sumOf { it.size }
    val externalSize: Int get() = 0
    val estimatedSize get() = size

    fun removeDeceased()
    fun clear()
}

abstract class AbstractFetchCache(
    override val name: String = ""
) : FetchCache {
    override fun removeDeceased() {
        val now = Instant.now()
        queues.forEach { it.removeIf { it.deadTime < now } }
    }

    override fun clear() {
        queues.forEach { it.clear() }
    }
}

open class ConcurrentFetchCache(name: String = "") : AbstractFetchCache(name) {
    override val nonReentrantQueue = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantQueue = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantQueue = ConcurrentLinkedQueue<UrlAware>()
}

class LoadingFetchCache(
    /**
     * The cache name, a loading fetch cache requires a unique name
     * */
    name: String = "",
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
    /**
     * The transformer
     * */
    val transformer: (UrlAware) -> UrlAware = { it }
) : AbstractFetchCache(name), Loadable<UrlAware> {

    companion object {
        const val G_NON_REENTRANT = 1
        const val G_N_ENTRANT = 2
        const val G_REENTRANT = 3
    }

    override val nonReentrantQueue =
        ConcurrentNonReentrantLoadingQueue(urlLoader, UrlGroup(name, G_NON_REENTRANT, priority, capacity), transformer)
    override val nReentrantQueue =
        ConcurrentNEntrantLoadingQueue(urlLoader, UrlGroup(name, G_N_ENTRANT, priority, capacity), 3, transformer)
    override val reentrantQueue = ConcurrentLoadingQueue(urlLoader, UrlGroup(name, G_REENTRANT, priority, capacity), transformer)
    override val queues: List<Queue<UrlAware>>
        get() = listOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    override val size get() = queues.sumOf { it.size }
    override val externalSize: Int
        get() = queues.filterIsInstance<LoadingQueue<UrlAware>>().sumOf { it.externalSize }
    override val estimatedSize: Int
        get() = size + queues.filterIsInstance<LoadingQueue<UrlAware>>().sumOf { it.estimatedExternalSize }

    override fun load() {
        queues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load() }
    }

    override fun load(delay: Duration) {
        queues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load(delay) }
    }

    override fun loadNow(): Collection<UrlAware> {
        return queues.filterIsInstance<Loadable<UrlAware>>().flatMap { it.loadNow() }
    }
}
