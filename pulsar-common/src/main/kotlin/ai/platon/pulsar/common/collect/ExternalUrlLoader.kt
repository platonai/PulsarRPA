package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant

data class UrlGroup(
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
    val priority: Int
)

/**
 * */
interface ExternalUrlLoader {
    companion object {
        /**
         * Url queues are small because every url uses about 1s to fetch, so do not load too many items each time
         * */
        const val LOAD_SIZE = 100
    }

    /**
     * The delay time to load after another load
     * */
    var loadDelay: Duration
    /**
     * If the loader is cooling down
     * */
    val isExpired: Boolean
    /**
     * Force the loading time to expire
     * */
    fun expire()
    /**
     * Force the loading time to expire
     * */
    fun reset() = expire()
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
}

abstract class AbstractExternalUrlLoader(
    override var loadDelay: Duration = Duration.ofSeconds(30)
): ExternalUrlLoader {

    @Volatile
    protected var lastLoadTime = Instant.EPOCH
    override val isExpired get() = lastLoadTime + loadDelay < Instant.now()

    override fun expire() { lastLoadTime = Instant.EPOCH }
    override fun reset() { lastLoadTime = Instant.EPOCH }
    /**
     * If there are more items in the source
     * */
    override fun hasMore(): Boolean = isExpired && countRemaining() > 0
    /**
     * If there are more items in the source
     * */
    override fun hasMore(group: UrlGroup): Boolean = isExpired && countRemaining(group) > 0

    override fun countRemaining() = 0

    override fun countRemaining(group: UrlGroup) = 0

    override fun saveAll(urls: Iterable<UrlAware>, group: UrlGroup) = urls.forEach { save(it, group) }

    override fun loadToNow(sink: MutableCollection<UrlAware>, size: Int, group: UrlGroup) =
            loadToNow(sink, size, group) { it }

    override fun loadTo(sink: MutableCollection<UrlAware>, size: Int, group: UrlGroup) = loadTo(sink, size, group) { it }

    override fun <T> loadTo(sink: MutableCollection<T>, size: Int, group: UrlGroup, transformer: (UrlAware) -> T) {
        if (!isExpired) {
            return
        }

        lastLoadTime = Instant.now()

        loadToNow(sink, size, group, transformer)
    }
}
