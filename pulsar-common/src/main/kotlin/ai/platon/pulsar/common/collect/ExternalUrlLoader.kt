package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant

data class UrlGroup constructor(
    /**
     * The job id
     * */
    val jobId: String,
    /**
     * The queue group
     * */
    val group: Int,
    /**
     * The priority
     * */
    val priority: Int,
    /**
     * The page size
     * */
    val pageSize: Int,
    /**
     * The loaded count
     * */
    var loadedCount: Int = 0,
    /**
     * The remaining count
     * */
    var remainingCount: Int = 0
)

class UrlGroupComparator {
    companion object : Comparator<UrlGroup> {
        override fun compare(o1: UrlGroup, o2: UrlGroup): Int {
            return keyOf(o1).compareTo(keyOf(o2))
        }

        fun keyOf(g: UrlGroup): String {
            return g.jobId + "." + g.group + "." + g.priority
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
    fun save(url: UrlAware, group: UrlGroup)
    /**
     * Save all the url to the external repository
     * */
    fun saveAll(urls: Iterable<UrlAware>, group: UrlGroup)
    /**
     * If there are more items in the source
     * */
    fun hasMore(): Boolean
    /**
     * If there are more items in the source
     * */
    fun hasMore(group: UrlGroup): Boolean
    /**
     * Count remaining size
     * */
    fun countRemaining(): Int
    /**
     * Count remaining size
     * */
    fun countRemaining(group: UrlGroup): Int
    /**
     * Estimate the size of remaining items, this operation should be very fast
     * */
    fun estimateRemaining(): Int
    /**
     * Estimate the size of remaining items, this operation should be very fast
     * */
    fun estimateRemaining(group: UrlGroup): Int
    /**
     * Load items from the source to the sink
     * */
    fun loadToNow(sink: MutableCollection<UrlAware>, size: Int, group: UrlGroup): Collection<UrlAware>
    /**
     * Load items from the source to the sink
     * */
    fun <T> loadToNow(sink: MutableCollection<T>, size: Int, group: UrlGroup, transformer: (UrlAware) -> T): Collection<T>
    /**
     * Load items from the source to the sink
     * */
    fun loadTo(sink: MutableCollection<UrlAware>, size: Int, group: UrlGroup)
    /**
     * Load items from the source to the sink
     * */
    fun <T> loadTo(sink: MutableCollection<T>, size: Int, group: UrlGroup, transformer: (UrlAware) -> T)

    fun deleteAll(group: UrlGroup): Long
}

abstract class AbstractExternalUrlLoader: ExternalUrlLoader {
    /**
     * If there are more items in the source
     * */
    override fun hasMore(): Boolean = countRemaining() > 0
    /**
     * If there are more items in the source
     * */
    override fun hasMore(group: UrlGroup): Boolean = countRemaining(group) > 0

    override fun saveAll(urls: Iterable<UrlAware>, group: UrlGroup) = urls.forEach { save(it, group) }

    override fun loadToNow(sink: MutableCollection<UrlAware>, size: Int, group: UrlGroup) =
        loadToNow(sink, size, group) { it }

    override fun loadTo(sink: MutableCollection<UrlAware>, size: Int, group: UrlGroup) =
        loadTo(sink, size, group) { it }
}

abstract class DelayExternalUrlLoader(
    val countDelay: Duration = Duration.ofMillis(500),
    val loadDelay: Duration = Duration.ofSeconds(5),
): AbstractExternalUrlLoader() {

    protected val counts = ConcurrentExpiringLRUCache<String, Int>(1000, countDelay)

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
    override fun hasMore(group: UrlGroup): Boolean = estimateRemaining(group) > 0

    abstract fun doCountRemaining(): Int

    abstract fun doCountRemaining(group: UrlGroup): Int

    override fun estimateRemaining() = counts.computeIfAbsent("") { countRemaining() }

    override fun estimateRemaining(group: UrlGroup): Int {
        val key = UrlGroupComparator.keyOf(group)
        return counts.computeIfAbsent(key) { countRemaining(group) }
    }

    override fun countRemaining(): Int {
        val count = doCountRemaining()
        counts.putDatum("", count)
        return count
    }

    override fun countRemaining(group: UrlGroup): Int {
        val count = doCountRemaining(group)
        val key = UrlGroupComparator.keyOf(group)
        counts.putDatum(key, count)
        return count
    }

    override fun <T> loadTo(sink: MutableCollection<T>, size: Int, group: UrlGroup, transformer: (UrlAware) -> T) {
        if (!isExpired) {
            return
        }

        lastLoadTime = Instant.now()
        loadToNow(sink, size, group, transformer)
    }
}

abstract class OneLoadExternalUrlLoader: AbstractExternalUrlLoader() {

    override fun countRemaining(): Int = 0

    override fun countRemaining(group: UrlGroup): Int = 0

    override fun estimateRemaining(): Int = 0

    override fun estimateRemaining(group: UrlGroup): Int = 0

    override fun <T> loadTo(sink: MutableCollection<T>, size: Int, group: UrlGroup, transformer: (UrlAware) -> T) {
        loadToNow(sink, size, group, transformer)
    }

    override fun reset() {
    }
}
