package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Predicate

/**
 * An url queue should be small since every url uses about 1s to fetch
 * */
abstract class AbstractLoadingQueue(
        val loader: ExternalUrlLoader,
        val group: UrlGroup,
        /**
         * The delay time to load after another load
         * */
        var loadDelay: Duration = Duration.ofSeconds(5),
        var estimateDelay: Duration = Duration.ofSeconds(5),
        val transformer: (UrlAware) -> UrlAware
): AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {
    private val logger = getLogger(AbstractLoadingQueue::class)

    protected val urlCache = ConcurrentLinkedQueue<UrlAware>()

    private val capacity = group.pageSize

    @Volatile
    protected open var lastEstimatedExternalSize: Int = -1

    @Volatile
    protected open var lastLoadTime = Instant.EPOCH

    @Volatile
    protected open var lastEstimateTime = Instant.EPOCH

    @Volatile
    protected open var lastBusyTime = Instant.now()

    protected open val isIdle
        get() = Duration.between(lastBusyTime, Instant.now()).seconds > 60

    // always access the underlying layer with a delay to reduce possible IO reads
    protected open val realEstimateDelay get() = if (isIdle) Duration.ofMinutes(1) else estimateDelay

    var loadCount: Int = 0
        protected set

    var savedCount: Int = 0
        protected set

    open val isExpired get() = isExpired(loadDelay)

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
        get() = estimateIfExpired().lastEstimatedExternalSize.coerceAtLeast(0)

    @get:Synchronized
    val freeSlots
        get() = capacity - urlCache.size

    @get:Synchronized
    val isFull
        get() = freeSlots == 0

    open fun isExpired(delay: Duration): Boolean {
        return lastLoadTime + delay < Instant.now()
    }

    open fun expire() {
        lastLoadTime = Instant.EPOCH
        lastEstimateTime = Instant.EPOCH
        lastBusyTime = Instant.now()
    }

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
        if (urlCache.isEmpty() && estimatedExternalSize > 0) {
            loadNow()
        } else if (freeSlots > 0 && isExpired) {
            loadNow()
        }
    }

    @Synchronized
    override fun load(delay: Duration) {
        if (freeSlots > 0 && isExpired(delay)) {
            loadNow()
        }
    }

    @Synchronized
    override fun loadNow(): Collection<UrlAware> {
        if (freeSlots <= 0) return listOf()

        lastLoadTime = Instant.now()

        val urls = try {
            ++loadCount
            loader.loadToNow(urlCache, freeSlots, group, transformer)
        } catch (e: Exception) {
            logger.warn("Failed to load", e)
            listOf()
        }

        if (urls.isNotEmpty()) {
            lastBusyTime = Instant.now()
            estimateNow()
        }

        return urls
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
        estimateNow()
    }

    @Synchronized
    override fun overflow(urls: List<UrlAware>) {
        try {
            loader.saveAll(urls, group)
            savedCount += urls.size
            estimateNow()
        } catch (e: Exception) {
            logger.warn("Failed to save urls", e)
        }
    }

    private fun estimateIfExpired(): AbstractLoadingQueue {
        if (lastEstimateTime + realEstimateDelay < Instant.now()) {
            estimateNow()
        }
        return this
    }

    private fun estimateNow() {
        lastEstimatedExternalSize = externalSize
        lastEstimateTime = Instant.now()
    }

    private fun refreshIfNecessary(): AbstractLoadingQueue {
        estimateIfExpired()

        if (urlCache.isEmpty()) {
            load()
        }

        return this
    }
}
