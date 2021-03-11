package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class MultiSourceHyperlinkIterable(
        val fetchCacheManager: FetchCatchManager,
        val lowerCacheSize: Int = 100
) : Iterable<Hyperlink> {
    private val realTimeCollector = FetchCacheCollector(fetchCacheManager.realTimeCache, Priority13.HIGHEST)
    private val multiSourceCollector = MultiSourceDataCollector<Hyperlink>()

    val loadingIterable = ConcurrentLoadingIterable(multiSourceCollector, realTimeCollector, lowerCacheSize)
    val cacheSize get() = loadingIterable.cacheSize
    val collectors get() = multiSourceCollector.collectors

    /**
     * Add a hyperlink to the very beginning of the fetch queue, so it will be served immediately
     * */
    fun addHead(hyperlink: Hyperlink) = loadingIterable.addHead(hyperlink)

    override fun iterator(): Iterator<Hyperlink> = loadingIterable.iterator()

    fun addDefaultCollectors(): MultiSourceHyperlinkIterable {
        fetchCacheManager.caches.forEach { (priority, fetchCache) ->
            collectors += FetchCacheCollector(fetchCache, priority)
        }
        return this
    }

    fun addDataCollector(collector: PriorityDataCollector<Hyperlink>): MultiSourceHyperlinkIterable {
        collectors += collector
        return this
    }
}
