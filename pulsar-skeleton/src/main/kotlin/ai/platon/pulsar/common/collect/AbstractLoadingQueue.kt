package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.url.UrlAware
import java.util.*

interface LoadingQueue<T>: Queue<T>, Loadable<T> {
    /**
     * An url queue should be small since every url uses about 1s to fetch
     * */

    companion object {
        const val DEFAULT_CAPACITY = 100
    }

    fun shuffle()
}

/**
 * An url queue should be small since every url uses about 1s to fetch
 * */
abstract class AbstractLoadingQueue(
        val loader: ExternalUrlLoader,
        val group: Int = 0,
        val priority: Int = Priority13.NORMAL.value,
        val capacity: Int = LoadingQueue.DEFAULT_CAPACITY
): AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {
//    protected val implementation = ArrayBlockingQueue<UrlAware>(capacity)
    protected val implementation = LinkedList<UrlAware>()

    @get:Synchronized
    val freeSlots get() = capacity - implementation.size

    @get:Synchronized
    val isFull get() = freeSlots == 0

    constructor(loader: ExternalUrlLoader, group: Int, priority: Priority13 = Priority13.NORMAL)
            : this(loader, group, priority.value)

    @Synchronized
    override fun load() {
        if (freeSlots > 0) {
            loader.loadTo(implementation, freeSlots, group, priority)
        }
    }

    @Synchronized
    override fun loadNow(): Collection<UrlAware> {
        return if (freeSlots > 0) {
            loader.loadToNow(implementation, freeSlots, group, priority)
        } else listOf()
    }

    @Synchronized
    override fun shuffle() {
        val l = implementation.toMutableList()
        implementation.clear()
        l.shuffle()
        implementation.addAll(l)
    }

    @Synchronized
    override fun add(url: UrlAware) = offer(url)

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        return if (!url.isPersistable || freeSlots > 0) {
            implementation.add(url)
        } else {
            loader.save(url, group)
            true
        }
    }

    @Synchronized
    fun removeIf(filter: (UrlAware) -> Boolean): Boolean {
        return implementation.removeIf(filter)
    }

    @Synchronized
    override fun iterator(): MutableIterator<UrlAware> = tryRefresh().implementation.iterator()

    @Synchronized
    override fun peek(): UrlAware? {
        var url = implementation.peek()
        while (url == null && loader.hasMore()) {
            loadNow()
            url = implementation.peek()
        }
        return url
    }

    @Synchronized
    override fun poll(): UrlAware? {
        peek()
        return implementation.poll()
    }

    @get:Synchronized
    override val size: Int get() = tryRefresh().implementation.size

    private fun tryRefresh(): AbstractLoadingQueue {
        if (freeSlots > 0) {
            load()
        }
        return this
    }
}
