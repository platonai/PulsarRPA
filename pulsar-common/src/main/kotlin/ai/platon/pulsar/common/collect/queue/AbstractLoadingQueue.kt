package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlTopic
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
    val topic: UrlTopic,
    val transformer: (UrlAware) -> UrlAware
) : AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {
    private val logger = getLogger(AbstractLoadingQueue::class)

    // TODO: check the synchronization, non-concurrent collection might be OK
    protected val cacheImplementation = ConcurrentLinkedQueue<UrlAware>()

    private val capacity = topic.pageSize

    var loadCount: Int = 0
        protected set

    var savedCount: Int = 0
        protected set

    val cache: Collection<UrlAware> = cacheImplementation

    /**
     * The cache size
     * */
    @get:Synchronized
    override val size: Int
        get() = cacheImplementation.size

    /**
     * Query the underlying database, this operation might be slow, try to use estimatedExternalSize
     * */
    @get:Synchronized
    override val externalSize: Int
        get() {
            return try {
                loader.countRemaining(topic)
            } catch (e: Exception) {
                logger.warn("Failed to count", e)
                0
            }
        }

    @get:Synchronized
    override val estimatedExternalSize: Int
        get() = externalSize

    @get:Synchronized
    val freeSlots
        get() = capacity - cacheImplementation.size

    @get:Synchronized
    val isFull
        get() = freeSlots == 0

    @Synchronized
    override fun clear() {
        cacheImplementation.clear()
    }

    @Synchronized
    override fun deepClear() {
        externalClear()
        clear()
    }

    @Synchronized
    fun externalClear() {
        loader.deleteAll(topic)
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
            loader.loadToNow(cacheImplementation, freeSlots, topic, transformer)
        } catch (e: Exception) {
            logger.warn("Failed to load", e)
            listOf()
        }
    }

    @Synchronized
    override fun shuffle() {
        val l = cacheImplementation.toMutableList()
        cacheImplementation.clear()
        l.shuffle()
        cacheImplementation.addAll(l)
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
            cacheImplementation.add(url)
        } else {
            overflow(url)
            true
        }
    }

    @Synchronized
    override fun removeIf(filter: Predicate<in UrlAware>): Boolean {
        return cacheImplementation.removeIf(filter)
    }

    @Synchronized
    override fun iterator(): MutableIterator<UrlAware> = refreshIfNecessary().cacheImplementation.iterator()

    @Synchronized
    override fun peek(): UrlAware? {
        refreshIfNecessary()
        return cacheImplementation.peek()
    }

    @Synchronized
    override fun poll(): UrlAware? {
        refreshIfNecessary()
        return cacheImplementation.poll()
    }

    @Synchronized
    override fun overflow(url: UrlAware) {
        loader.save(url, topic)
        ++savedCount
    }

    @Synchronized
    override fun overflow(urls: List<UrlAware>) {
        try {
            loader.saveAll(urls, topic)
            savedCount += urls.size
        } catch (e: Exception) {
            logger.warn("Failed to save urls", e)
        }
    }

    private fun refreshIfNecessary(): AbstractLoadingQueue {
        if (cacheImplementation.isEmpty()) {
            load()
        }

        return this
    }
}
