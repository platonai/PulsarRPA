package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Predicate

interface LoadingQueue<T>: Queue<T>, Loadable<T> {
    companion object {
        /**
         * An url queue should be small since every url uses about 1s to fetch
         * */
        const val DEFAULT_CAPACITY = 200
    }

    val externalSize: Int

    val estimatedExternalSize: Int

    fun shuffle()

    fun overflow(url: UrlAware)

    fun overflow(url: List<UrlAware>)
}

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

    protected val urlCache = ConcurrentLinkedQueue<UrlAware>()

    private val capacity = group.pageSize

    @Volatile
    private var lastEstimatedExternalSize: Int = -1

    @Volatile
    protected var lastLoadTime = Instant.EPOCH

    protected val isIdle
        get() = lastLoadTime.epochSecond > 0 && Duration.between(lastLoadTime, Instant.now()).seconds > 60

    @Volatile
    protected var lastEstimateTime = Instant.EPOCH

    protected val realEstimateDelay get() = if (isIdle) Duration.ofMinutes(1) else estimateDelay

    var loadCount: Int = 0
        protected set

    var savedCount: Int = 0
        protected set

    val isExpired get() = isExpired(loadDelay)

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
        get() = loader.countRemaining(group)

    @get:Synchronized
    override val estimatedExternalSize: Int
        get() = estimateIfExpired().lastEstimatedExternalSize.coerceAtLeast(0)

    @get:Synchronized
    val freeSlots
        get() = capacity - urlCache.size

    @get:Synchronized
    val isFull
        get() = freeSlots == 0

    fun isExpired(delay: Duration): Boolean {
        return lastLoadTime + delay < Instant.now()
    }

    fun expire() {
        lastLoadTime = Instant.EPOCH
        lastEstimateTime = Instant.EPOCH
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
        return if (freeSlots > 0) {
            lastLoadTime = Instant.now()
            loader.loadToNow(urlCache, freeSlots, group, transformer).also {
                ++loadCount
                estimateNow()
            }
        } else listOf()
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
        loader.saveAll(urls, group)
        savedCount += urls.size
        estimateNow()
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
