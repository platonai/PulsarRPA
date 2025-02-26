package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.UrlPool.Companion.REAL_TIME_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.common.urls.UrlAware
import com.google.common.primitives.Ints
import org.apache.commons.collections4.queue.SynchronizedQueue
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The delay url. A delay url is a url with a delay.
 * Delay urls can work with a [DelayQueue], so every time when we retrieve an item from the queue,
 * only the items with delay expired are available.
 * */
open class DelayUrl(
    val url: UrlAware,
    val delay: Duration,
) : Delayed {
    // The time at which the delay expires
    val delayExpireAt = System.currentTimeMillis() + delay.toMillis()

    constructor(url: String, delay: Duration): this(PlainUrl(url), delay)

    override fun compareTo(other: Delayed): Int {
        return Ints.saturatedCast(delayExpireAt - (other as DelayUrl).delayExpireAt)
    }

    override fun getDelay(unit: TimeUnit): Long {
        val diff = delayExpireAt - System.currentTimeMillis()
        return unit.convert(diff, TimeUnit.MILLISECONDS)
    }
}

/**
 * A [UrlPool] contains many [UrlCache]s, the urls added to the pool will be processed in crawl loops.
 *
 * Different [UrlCache]s in [UrlPool] have different priorities, there are real time cache, delay cache,
 * ordered caches and unordered caches.
 * */
interface UrlPool {
    companion object {
        val REAL_TIME_PRIORITY = Priority13.HIGHEST.value
    }
    
    val id: String
    
    /**
     * The real time url cache in which urls have the highest priority of all.
     * */
    val realTimeCache: UrlCache
    /**
     * An unbounded queue of [Delayed] urls, in which an element can only be taken
     * when its delay has expired.
     *
     * Delay cache has higher priority than all ordered caches and is usually used for retrying tasks.
     * */
    val delayCache: Queue<DelayUrl>
    /**
     * The ordered url caches.
     *
     * Ordered caches has higher priority than unordered caches.
     * */
    val orderedCaches: MutableMap<Int, UrlCache>
    /**
     * The unordered url caches, tasks in unordered caches have the lowest priority.
     *
     * Unordered caches has the lowest priority of all
     * */
    val unorderedCaches: MutableList<UrlCache>
    /**
     * Total number of items in all url caches.
     * */
    val totalCount: Int
    /**
     * A shortcut to the cache with the lowest priority in the ordered caches
     * */
    val lowestCache: UrlCache
    /**
     * A shortcut to the cache that is 5 priority lower than the normal cache in the ordered caches.
     * */
    val lower5Cache: UrlCache
    /**
     * A shortcut to the cache that is 4 priority lower than the normal cache in the ordered caches.
     * */
    val lower4Cache: UrlCache
    /**
     * A shortcut to the cache that is 3 priority lower than the normal cache in the ordered caches.
     * */
    val lower3Cache: UrlCache
    /**
     * A shortcut to the cache that is 2 priority lower than the normal cache in the ordered caches.
     * */
    val lower2Cache: UrlCache
    /**
     * A shortcut to the cache that is 1 priority lower than the normal cache in the ordered caches.
     * */
    val lowerCache: UrlCache
    /**
     * A shortcut to the cache has the default priority in the ordered caches.
     * */
    val normalCache: UrlCache
    /**
     * A shortcut to the cache that is 1 priority higher than the normal cache in the ordered caches.
     * */
    val higherCache: UrlCache
    /**
     * A shortcut to the cache that is 2 priority higher than the normal cache in the ordered caches.
     * */
    val higher2Cache: UrlCache
    /**
     * A shortcut to the cache that is 3 priority higher than the normal cache in the ordered caches.
     * */
    val higher3Cache: UrlCache
    /**
     * A shortcut to the cache that is 4 priority higher than the normal cache in the ordered caches.
     * */
    val higher4Cache: UrlCache
    /**
     * A shortcut to the cache that is 5 priority higher than the normal cache in the ordered caches.
     * */
    val higher5Cache: UrlCache
    /**
     * A shortcut to the cache with the highest priority in the ordered caches.
     * */
    val highestCache: UrlCache
    /**
     * Initialize the url pool
     * */
    fun initialize()
    /**
     * Add a url to the pool
     * */
    fun add(url: String, priority: Priority13 = Priority13.NORMAL): Boolean
    /**
     * Add a url to the pool
     * */
    fun add(url: UrlAware): Boolean
    /**
     * Add urls to the pool
     * */
    fun addAll(urls: Iterable<String>, priority: Priority13 = Priority13.NORMAL): Boolean
    /**
     * Add urls to the pool
     * */
    fun addAll(urls: Iterable<UrlAware>): Boolean
    /**
     * Remove deceased urls, such as URLs that are past the deadline.
     * */
    fun removeDeceased()
    /**
     * Clear the pool
     * */
    fun clear()
    /**
     * Check if there is more items in the url pool
     * */
    fun hasMore(): Boolean
}

/**
 * The abstract url pool
 * */
abstract class AbstractUrlPool(val conf: ImmutableConfig) : UrlPool {
    protected val initialized = AtomicBoolean()
    
    override val id: String = UUID.randomUUID().toString()
    
    override val totalCount: Int get() {
        val a = ensureInitialized().orderedCaches.values.sumOf { it.size }

        return a + realTimeCache.size + delayCache.size + unorderedCaches.sumOf { it.size }
    }

    override val lowestCache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.LOWEST.value]!!
    override val lower5Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.LOWER5.value]!!
    override val lower4Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.LOWER4.value]!!
    override val lower3Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.LOWER3.value]!!
    override val lower2Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.LOWER2.value]!!
    override val lowerCache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.LOWER.value]!!
    override val normalCache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.NORMAL.value]!!
    override val higherCache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.HIGHER.value]!!
    override val higher2Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.HIGHER2.value]!!
    override val higher3Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.HIGHER3.value]!!
    override val higher4Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.HIGHER4.value]!!
    override val higher5Cache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.HIGHER5.value]!!
    override val highestCache: UrlCache get() = ensureInitialized().orderedCaches[Priority13.HIGHEST.value]!!

    override fun add(url: String, priority: Priority13): Boolean {
        val link = Hyperlink(url, "", priority = priority.value)
        return add(link)
    }

    override fun add(url: UrlAware): Boolean {
        val priority = Priority13.lowerPriority(url.priority).value
        val added = orderedCaches[priority]?.reentrantQueue?.add(url)
        return added == true
    }

    override fun addAll(urls: Iterable<String>, priority: Priority13): Boolean {
        return addAll(urls.map { Hyperlink(it) })
    }

    override fun addAll(urls: Iterable<UrlAware>): Boolean {
        var count = 0
        urls.forEach {
            val priority = Priority13.lowerPriority(it.priority).value
            val added = orderedCaches[priority]?.reentrantQueue?.add(it)
            if (added == true) {
                ++count
            }
        }
        return count > 0
    }

    override fun removeDeceased() {
        ensureInitialized()
        orderedCaches.values.forEach { it.removeDeceased() }
        unorderedCaches.forEach { it.removeDeceased() }
        val now = Instant.now()
        delayCache.removeIf { it.url.deadline < now }
    }

    override fun clear() {
        // orderedCaches.values.forEach { it.clear() }
        orderedCaches.clear()
        // unorderedCaches.forEach { it.clear() }
        unorderedCaches.clear()
        realTimeCache.clear()
        delayCache.clear()
    }

    override fun hasMore(): Boolean {
        return totalCount > 0
    }

    private fun ensureInitialized(): AbstractUrlPool {
        if (initialized.compareAndSet(false, true)) {
            initialize()
        }
        return this
    }
}

/**
 * The concurrent url pool
 * */
open class ConcurrentUrlPool(conf: ImmutableConfig) : AbstractUrlPool(conf) {
    override val realTimeCache: UrlCache = ConcurrentUrlCache("realtime", REAL_TIME_PRIORITY)
    override val delayCache: Queue<DelayUrl> = SynchronizedQueue.synchronizedQueue(DelayQueue())
    override val orderedCaches = ConcurrentSkipListMap<Int, UrlCache>()
    override val unorderedCaches: MutableList<UrlCache> = Collections.synchronizedList(mutableListOf())

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            Priority13.entries.forEach { orderedCaches[it.value] = ConcurrentUrlCache(it.name, it.value) }
        }
    }
}

/**
 * A [LoadingUrlPool] is a [UrlPool], the items can be loaded from external source using [loader].
 * */
open class LoadingUrlPool(
    val loader: ExternalUrlLoader,
    val capacity: Int = 10_000,
    conf: ImmutableConfig,
) : ConcurrentUrlPool(conf) {

    override val realTimeCache: UrlCache = LoadingUrlCache("realtime", REAL_TIME_PRIORITY, loader, capacity)

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            Priority13.entries.forEach {
                orderedCaches[it.value] = LoadingUrlCache(it.name, it.value, loader, capacity)
            }
        }
    }
}
