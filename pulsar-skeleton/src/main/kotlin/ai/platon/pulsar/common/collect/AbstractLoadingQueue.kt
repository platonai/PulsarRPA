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
        const val DEFAULT_CAPACITY = 100
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
        val capacity: Int = LoadingQueue.DEFAULT_CAPACITY,
        /**
         * The delay time to load after another load
         * */
        var loadDelay: Duration = Duration.ofSeconds(60),
        val transformer: (UrlAware) -> UrlAware
): AbstractQueue<UrlAware>(), LoadingQueue<UrlAware> {

    protected val implementation = ConcurrentLinkedQueue<UrlAware>()

    @Volatile
    private var _estimatedExternalSize: Int = -1

    @Volatile
    protected var lastLoadTime = Instant.EPOCH

    var loadCount: Int = 0
        protected set

    var savedCount: Int = 0
        protected set

    val isExpired get() = isExpired(loadDelay)

    fun expire() {
        lastLoadTime = Instant.EPOCH
    }

    /**
     * The cache size
     * */
    @get:Synchronized
    override val size: Int
        get() = implementation.size

    /**
     * Query the underlying database, this operation might be slow, try to use estimatedExternalSize
     * */
    @get:Synchronized
    override val externalSize: Int
        get() = loader.countRemaining(group)

    @get:Synchronized
    override val estimatedExternalSize: Int
        get() = _estimatedExternalSize.coerceAtLeast(0)

    @get:Synchronized
    val freeSlots
        get() = capacity - implementation.size

    @get:Synchronized
    val isFull
        get() = freeSlots == 0

    fun isExpired(delay: Duration): Boolean {
        return lastLoadTime + delay < Instant.now()
    }

    @Synchronized
    override fun clear() {
        implementation.clear()
    }

    fun externalClear() {
        loader.deleteAll(group)
    }

    @Synchronized
    override fun load() {
        if (isEmpty() && estimatedExternalSize > 0) {
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
            loader.loadToNow(implementation, freeSlots, group, transformer).also {
                _estimatedExternalSize = externalSize
                ++loadCount
            }
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
            implementation.add(url)
        } else {
            overflow(url)
            true
        }
    }

    @Synchronized
    override fun removeIf(filter: Predicate<in UrlAware>): Boolean {
        return implementation.removeIf(filter)
    }

    @Synchronized
    override fun iterator(): MutableIterator<UrlAware> = refreshIfNecessary().implementation.iterator()

    @Synchronized
    override fun peek(): UrlAware? {
        refreshIfNecessary()
        return implementation.peek()
    }

    @Synchronized
    override fun poll(): UrlAware? {
        refreshIfNecessary()
        return implementation.poll()
    }

    @Synchronized
    override fun overflow(url: UrlAware) {
        loader.save(url, group)
        ++savedCount
        estimate()
    }

    @Synchronized
    override fun overflow(urls: List<UrlAware>) {
        loader.saveAll(urls, group)
        savedCount += urls.size
        estimate()
    }

    private fun estimate() {
        _estimatedExternalSize = externalSize
    }

    private fun refreshIfNecessary(): AbstractLoadingQueue {
        if (_estimatedExternalSize < 0) {
            estimate()
        }

        if (implementation.isEmpty()) {
            load()
        }

        return this
    }
}
