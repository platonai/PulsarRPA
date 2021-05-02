package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.util.*

interface LoadingQueue<T>: Queue<T>, Loadable<T> {
    companion object {
        /**
         * An url queue should be small since every url uses about 1s to fetch
         * */
        const val DEFAULT_CAPACITY = 100
    }

    val externalSize: Int

    val estimatedExternalSize: Int

    fun shuffle()

    fun overflow(url: UrlAware)
}

/**
 * An url queue should be small since every url uses about 1s to fetch
 * */
abstract class AbstractLoadingQueue(
        val loader: ExternalUrlLoader,
        val group: UrlGroup,
        val capacity: Int = LoadingQueue.DEFAULT_CAPACITY
): AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {

    companion object {
        private const val ESTIMATED_EXTERNAL_SIZE_KEY = "EES"
    }

    private val expiringCache = ConcurrentExpiringLRUCache<Int>(1, Duration.ofMinutes(1))

    protected val implementation = LinkedList<UrlAware>()

    /**
     * The cache size
     * */
    @get:Synchronized
    override val size: Int get() = tryRefresh().implementation.size

    /**
     * Query the underlying database, try to use estimatedExternalSize
     * */
    override val externalSize: Int
        get() = loader.countRemaining(group)

    override val estimatedExternalSize: Int
        get() = expiringCache.computeIfAbsent(ESTIMATED_EXTERNAL_SIZE_KEY) { externalSize }

    @get:Synchronized
    val freeSlots get() = capacity - implementation.size

    @get:Synchronized
    val isFull get() = freeSlots == 0

    @Synchronized
    override fun load() {
        if (freeSlots > 0) {
            loader.loadTo(implementation, freeSlots, group)
        }
    }

    @Synchronized
    override fun loadNow(): Collection<UrlAware> {
        return if (freeSlots > 0) {
            loader.loadToNow(implementation, freeSlots, group)
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
    override fun addAll(urls: Collection<UrlAware>): Boolean {
        // TODO: optimize using loader.saveAll()
        urls.forEach { add(it) }
        return true
    }

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        return if (!url.isPersistable || freeSlots > 0) {
            implementation.add(url)
        } else {
            overflow(url)
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
        tryRefresh()
        return implementation.peek()
    }

    @Synchronized
    override fun poll(): UrlAware? {
        tryRefresh()
        return implementation.poll()
    }

    override fun overflow(url: UrlAware) {
        loader.save(url, group)
    }

    private fun tryRefresh(): AbstractLoadingQueue {
        if (freeSlots > 0) {
            load()
        }
        return this
    }
}
