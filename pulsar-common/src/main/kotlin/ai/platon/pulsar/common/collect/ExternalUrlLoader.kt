package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.collect.queue.LoadingQueue
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant

data class UrlTopic constructor(
    /**
     * The topic name
     * */
    val name: String,
    /**
     * The queue group
     * */
    val group: Int,
    /**
     * The priority
     * */
    val priority: Int,
) {
    /**
     * The page size
     * */
    var pageSize: Int = LoadingQueue.DEFAULT_CAPACITY
    /**
     * The loaded count
     * */
    var loadedCount: Int = 0
    /**
     * The remaining count
     * */
    var remainingCount: Int = 0

    constructor(name: String, group: Int, priority: Int, pageSize: Int): this(name, group, priority) {
        this.pageSize = pageSize
    }

    override fun toString(): String = "$name.$group.$priority"
}

class UrlGroupComparator {
    companion object : Comparator<UrlTopic> {
        override fun compare(o1: UrlTopic, o2: UrlTopic): Int {
            return o1.toString().compareTo(o2.toString())
        }
    }
}

/**
 *
 * */
interface ExternalUrlLoader {
    /**
     * Force the loading time to expire
     * */
    fun reset()
    /**
     * Save the url to the external repository
     * */
    fun save(url: UrlAware, topic: UrlTopic)
    /**
     * Save all the url to the external repository
     * */
    fun saveAll(urls: Iterable<UrlAware>, topic: UrlTopic)
    /**
     * If there are more items in the source
     * */
    fun hasMore(): Boolean
    /**
     * If there are more items in the source
     * */
    fun hasMore(topic: UrlTopic): Boolean
    /**
     * Count remaining size
     * */
    fun countRemaining(): Int
    /**
     * Count remaining size
     * */
    fun countRemaining(topic: UrlTopic): Int
    /**
     * Estimate the size of remaining items, this operation should be very fast
     * */
    fun estimateRemaining(): Int
    /**
     * Estimate the size of remaining items, this operation should be very fast
     * */
    fun estimateRemaining(topic: UrlTopic): Int
    /**
     * Load items from the source to the sink
     * */
    fun loadToNow(sink: MutableCollection<UrlAware>, size: Int, topic: UrlTopic): Collection<UrlAware>
    /**
     * Load items from the source to the sink
     * */
    fun <T> loadToNow(sink: MutableCollection<T>, size: Int, topic: UrlTopic, transformer: (UrlAware) -> T): Collection<T>
    /**
     * Load items from the source to the sink
     * */
    fun loadTo(sink: MutableCollection<UrlAware>, size: Int, topic: UrlTopic)
    /**
     * Load items from the source to the sink
     * */
    fun <T> loadTo(sink: MutableCollection<T>, size: Int, topic: UrlTopic, transformer: (UrlAware) -> T)

    fun deleteAll(topic: UrlTopic): Long
}

abstract class AbstractExternalUrlLoader: ExternalUrlLoader {
    /**
     * If there are more items in the source
     * */
    override fun hasMore(): Boolean = countRemaining() > 0
    /**
     * If there are more items in the source
     * */
    override fun hasMore(topic: UrlTopic): Boolean = countRemaining(topic) > 0

    override fun saveAll(urls: Iterable<UrlAware>, topic: UrlTopic) = urls.forEach { save(it, topic) }

    override fun loadToNow(sink: MutableCollection<UrlAware>, size: Int, topic: UrlTopic) =
        loadToNow(sink, size, topic) { it }

    override fun loadTo(sink: MutableCollection<UrlAware>, size: Int, topic: UrlTopic) =
        loadTo(sink, size, topic) { it }
}

abstract class DelayExternalUrlLoader(
    val countDelay: Duration = Duration.ofSeconds(1),
    val loadDelay: Duration = Duration.ofSeconds(5),
): AbstractExternalUrlLoader() {

    protected val counts = ConcurrentExpiringLRUCache<String, Int>(countDelay, 1000)

    @Volatile
    protected var lastLoadTime = Instant.EPOCH

    val isExpired get() = lastLoadTime + loadDelay < Instant.now()

//    val isIdle get() = lastLoadTime > Instant.EPOCH

    open fun expire() {
        lastLoadTime = Instant.EPOCH
        counts.clear()
    }

    override fun reset() {
        expire()
    }
    /**
     * If there are more items in the source
     * */
    override fun hasMore(): Boolean = estimateRemaining() > 0
    /**
     * If there are more items in the source
     * */
    override fun hasMore(topic: UrlTopic): Boolean = estimateRemaining(topic) > 0

    abstract fun doCountRemaining(): Int

    abstract fun doCountRemaining(topic: UrlTopic): Int

    override fun estimateRemaining() = counts.computeIfAbsent("") { countRemaining() }

    override fun estimateRemaining(topic: UrlTopic): Int {
        return counts.computeIfAbsent(topic.toString()) { countRemaining(topic) }
    }

    override fun countRemaining(): Int {
        val count = doCountRemaining()
        counts.putDatum("", count)
        return count
    }

    override fun countRemaining(topic: UrlTopic): Int {
        val count = doCountRemaining(topic)
        val key = topic.toString()
        counts.putDatum(key, count)
        return count
    }

    override fun <T> loadTo(sink: MutableCollection<T>, size: Int, topic: UrlTopic, transformer: (UrlAware) -> T) {
        if (!isExpired) {
            return
        }

        lastLoadTime = Instant.now()
        loadToNow(sink, size, topic, transformer)
    }
}

abstract class OneLoadExternalUrlLoader: AbstractExternalUrlLoader() {

    override fun countRemaining(): Int = 0

    override fun countRemaining(topic: UrlTopic): Int = 0

    override fun estimateRemaining(): Int = 0

    override fun estimateRemaining(topic: UrlTopic): Int = 0

    override fun <T> loadTo(sink: MutableCollection<T>, size: Int, topic: UrlTopic, transformer: (UrlAware) -> T) {
        loadToNow(sink, size, topic, transformer)
    }

    override fun reset() {
    }
}
