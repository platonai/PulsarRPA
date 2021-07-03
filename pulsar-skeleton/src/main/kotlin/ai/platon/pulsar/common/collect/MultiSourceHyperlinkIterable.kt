package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.common.collect.PriorityDataCollectorsFormatter
import java.util.*

class MultiSourceHyperlinkIterable(
    val fetchCaches: FetchCacheManager,
    val lowerCacheSize: Int = 100,
) : Iterable<UrlAware> {
    private val realTimeCollector = FetchCacheCollector(fetchCaches.realTimeCache, Priority13.HIGHEST)
        .apply { name = "FetchCacheC@RealTime" }
    private val delayCollector = DelayCacheCollector(fetchCaches.delayCache, Priority13.HIGHER5)
        .apply { name = "DelayCacheC@Delay" }
    private val regularCollector = MultiSourceDataCollector<UrlAware>()

    val loadingIterable =
        ConcurrentLoadingIterable(regularCollector, realTimeCollector, delayCollector, lowerCacheSize)
    val cacheSize get() = loadingIterable.cacheSize

    val regularCollectors: Queue<PriorityDataCollector<UrlAware>> get() = regularCollector.collectors

    val allCollectors: List<PriorityDataCollector<UrlAware>> get() {
        val list = mutableListOf<PriorityDataCollector<UrlAware>>()
        list += realTimeCollector
        list += delayCollector
        list += this.regularCollectors
        list.sortBy { it.priority }
        return list
    }

    val abstract: String get() = PriorityDataCollectorsFormatter(allCollectors).abstract()

    val report: String get() = PriorityDataCollectorsFormatter(allCollectors).toString()

//    val normalCacheSize get() = fetchCaches.caches.entries.sumBy { it.value.size }
//    val normalCacheEstimatedSize get() = fetchCaches.caches.entries.sumBy { it.value.estimatedSize }
//    val totalCacheSize get() = cacheSize + fetchCaches.realTimeCache.size + normalCacheSize

    /**
     * Add a hyperlink to the very beginning of the fetch queue, so it will be served immediately
     * */
    fun addHead(url: UrlAware) = loadingIterable.addHead(url)

    override fun iterator(): Iterator<UrlAware> = loadingIterable.iterator()

    fun addDefaultCollectors(): MultiSourceHyperlinkIterable {
        regularCollectors.removeIf { it is FetchCacheCollector }
        fetchCaches.caches.forEach { (priority, fetchCache) ->
            regularCollectors += FetchCacheCollector(fetchCache, priority)
                .apply { name = "FetchCacheC@" + hashCode() }
        }
        return this
    }

    fun addDataCollector(collector: PriorityDataCollector<UrlAware>): MultiSourceHyperlinkIterable {
        regularCollectors += collector
        return this
    }

    fun addDataCollectors(collectors: Iterable<PriorityDataCollector<UrlAware>>): MultiSourceHyperlinkIterable {
        regularCollectors += collectors
        return this
    }
}
