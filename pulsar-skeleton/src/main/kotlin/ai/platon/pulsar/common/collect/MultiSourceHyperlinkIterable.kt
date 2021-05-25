package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*

class MultiSourceHyperlinkIterable(
    val fetchCaches: FetchCatchManager,
    val lowerCacheSize: Int = 100,
) : Iterable<UrlAware> {
    private val realTimeCollector = FetchCacheCollector(fetchCaches.realTimeCache, Priority13.HIGHEST)
        .apply { name = "FetchCacheCollector@RealTime" }
    private val delayCollector = DelayCacheCollector(fetchCaches.delayCache, Priority13.HIGHER5)
        .apply { name = "DelayCacheCollector@Delay" }
    private val multiSourceCollector = MultiSourceDataCollector<UrlAware>()

    val loadingIterable =
        ConcurrentLoadingIterable(multiSourceCollector, realTimeCollector, delayCollector, lowerCacheSize)
    val cacheSize get() = loadingIterable.cacheSize

    val collectors: Queue<PriorityDataCollector<UrlAware>> get() = multiSourceCollector.collectors

    /**
     * Add a hyperlink to the very beginning of the fetch queue, so it will be served immediately
     * */
    fun addHead(url: UrlAware) = loadingIterable.addHead(url)

    override fun iterator(): Iterator<UrlAware> = loadingIterable.iterator()

    fun addDefaultCollectors(): MultiSourceHyperlinkIterable {
        // TODO: use a single collector to collect all caches in FetchCatchManager
        fetchCaches.caches.forEach { (priority, fetchCache) ->
            collectors += FetchCacheCollector(fetchCache, priority)
        }
        return this
    }

    fun addDataCollector(collector: PriorityDataCollector<UrlAware>): MultiSourceHyperlinkIterable {
        collectors += collector
        return this
    }
}
