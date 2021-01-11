package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.url.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

interface LoadingQueue<T>: Queue<T>, Loadable<T> {
    fun shuffle()
}

abstract class AbstractLoadingQueue(
        val loader: ExternalUrlLoader,
        val group: Int = 0,
        val priority: Int = Priority13.NORMAL.value,
        val capacity: Int = 100_000
): AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {
//    protected val cache = ConcurrentSkipListSet<UrlAware>()
    protected val cache = Collections.synchronizedList(LinkedList<UrlAware>())

    val freeSlots get() = capacity - cache.size
    val isFull get() = freeSlots == 0

    constructor(loader: ExternalUrlLoader, group: Int, priority: Priority13 = Priority13.NORMAL)
            : this(loader, group, priority.value)

    override fun load() {
        if (freeSlots > 0) {
            loader.loadTo(cache, freeSlots, group, priority)
        }
    }

    override fun loadNow(): Collection<UrlAware> {
        return if (freeSlots > 0) {
            loader.loadToNow(cache, freeSlots, group, priority)
        } else listOf()
    }

    override fun shuffle() {
        cache.shuffle()
    }

    override fun add(url: UrlAware) = offer(url)

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        return if (!url.isPersistable || freeSlots > 0) {
            cache.add(url)
        } else {
            loader.save(url, group)
            true
        }
    }

    override fun iterator(): MutableIterator<UrlAware> = tryRefresh().cache.iterator()

    override fun peek(): UrlAware? {
        var url = cache.firstOrNull()
        while (url == null && loader.hasMore()) {
            loadNow()
            url = cache.firstOrNull()
        }
        return url
    }

    override fun poll(): UrlAware? {
        peek()
        return cache.removeFirstOrNull()
    }

    override val size: Int get() = tryRefresh().cache.size

    private fun tryRefresh(): AbstractLoadingQueue {
        if (freeSlots > 0) {
            load()
        }
        return this
    }
}
