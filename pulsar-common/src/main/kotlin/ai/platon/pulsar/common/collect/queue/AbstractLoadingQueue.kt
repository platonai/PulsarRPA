package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlGroup
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Predicate

/**
 * An url queue should be small since every url uses about 1s to fetch
 * */
abstract class AbstractLoadingQueue(
    val loader: ExternalUrlLoader,
    val group: UrlGroup,
    val transformer: (UrlAware) -> UrlAware
) : AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {
    private val logger = getLogger(AbstractLoadingQueue::class)

    protected val urlCache = ConcurrentLinkedQueue<UrlAware>()

    private val capacity = group.pageSize

    var loadCount: Int = 0
        protected set

    var savedCount: Int = 0
        protected set

    val cache: Collection<UrlAware> = urlCache

    /**
     * The cache size
     * */
    @get:Synchronized
    override val size: Int
        get() = urlCache.size

    /**
     * Query the underlying database, this operation might be slow, try to use estimatedExternalSize
     * */
    @get:Synchronized
    override val externalSize: Int
        get() {
            try {
                return loader.countRemaining(group)
            } catch (e: Exception) {
                logger.warn("Failed to count", e)
            }

            return 0
        }

    @get:Synchronized
    override val estimatedExternalSize: Int
        get() = externalSize

    @get:Synchronized
    val freeSlots
        get() = capacity - urlCache.size

    @get:Synchronized
    val isFull
        get() = freeSlots == 0

    @Synchronized
    override fun clear() {
        urlCache.clear()
    }

    @Synchronized
    fun externalClear() {
        loader.deleteAll(group)
    }

    @Synchronized
    override fun load() {
        if (freeSlots > 0) {
            loadNow()
        }
    }

    @Synchronized
    override fun loadNow(): Collection<UrlAware> {
        if (freeSlots <= 0) return listOf()

        return try {
            ++loadCount
            loader.loadToNow(urlCache, freeSlots, group, transformer)
        } catch (e: Exception) {
            logger.warn("Failed to load", e)
            listOf()
        }
    }

    @Synchronized
    override fun shuffle() {
        val l = urlCache.toMutableList()
        urlCache.clear()
        l.shuffle()
        urlCache.addAll(l)
    }

    @Synchronized
    override fun add(url: UrlAware) = offer(url)

    @Synchronized
    override fun addAll(urls: Collection<UrlAware>): Boolean {
        if (urls.size > freeSlots) {
            val n = freeSlots
            // TODO: can be optimized
            super.addAll(urls.take(n))
            overflow(urls.drop(n))
        } else {
            super.addAll(urls)
        }
        return true
    }

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        return if (!url.isPersistable || freeSlots > 0) {
            urlCache.add(url)
        } else {
            overflow(url)
            true
        }
    }

    @Synchronized
    override fun removeIf(filter: Predicate<in UrlAware>): Boolean {
        return urlCache.removeIf(filter)
    }

    @Synchronized
    override fun iterator(): MutableIterator<UrlAware> = refreshIfNecessary().urlCache.iterator()

    @Synchronized
    override fun peek(): UrlAware? {
        refreshIfNecessary()
        return urlCache.peek()
    }

    @Synchronized
    override fun poll(): UrlAware? {
        refreshIfNecessary()
        return urlCache.poll()
    }

    @Synchronized
    override fun overflow(url: UrlAware) {
        loader.save(url, group)
        ++savedCount
    }

    @Synchronized
    override fun overflow(urls: List<UrlAware>) {
        try {
            loader.saveAll(urls, group)
            savedCount += urls.size
        } catch (e: Exception) {
            logger.warn("Failed to save urls", e)
        }
    }

    private fun refreshIfNecessary(): AbstractLoadingQueue {
        if (urlCache.isEmpty()) {
            load()
        }

        return this
    }
}
