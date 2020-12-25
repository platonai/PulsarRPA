package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.url.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

abstract class AbstractLoadingQueue(
        val loader: ExternalUrlLoader,
        val group: Int = 0,
        val priority: Int = Priority.NORMAL.value,
        val capacity: Int = 100_000
): AbstractQueue<UrlAware>(), Loadable<UrlAware> {
    protected val cache = ConcurrentSkipListSet<UrlAware>()

    val freeSlots get() = capacity - cache.size
    val isFull get() = freeSlots == 0

    constructor(loader: ExternalUrlLoader, group: Int, priority: Priority = Priority.NORMAL)
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

    override fun add(url: UrlAware) = offer(url)

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        return if (freeSlots > 0) {
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
        return cache.pollFirst()
    }

    override val size: Int get() = tryRefresh().cache.size

    private fun tryRefresh(): AbstractLoadingQueue {
        if (freeSlots > 0) {
            load()
        }
        return this
    }
}
