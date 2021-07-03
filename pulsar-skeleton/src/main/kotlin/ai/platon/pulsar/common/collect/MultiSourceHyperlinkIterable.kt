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

    val regularCollectors: Collection<PriorityDataCollector<UrlAware>> get() = regularCollector.collectors

    val collectors: List<PriorityDataCollector<UrlAware>> get() {
        val list = mutableListOf<PriorityDataCollector<UrlAware>>()
        list += realTimeCollector
        list += delayCollector
        list += this.regularCollectors
        list.sortBy { it.priority }
        return list
    }

    val abstract: String get() = PriorityDataCollectorsFormatter(collectors).abstract()

    val report: String get() = PriorityDataCollectorsFormatter(collectors).toString()

    /**
     * Add a hyperlink to the very beginning of the fetch queue, so it will be served immediately
     * */
    fun addFirst(url: UrlAware) = loadingIterable.addFirst(url)

    fun addLast(url: UrlAware) = loadingIterable.addLast(url)

    override fun iterator(): Iterator<UrlAware> = loadingIterable.iterator()

    fun addDefaultCollectors(): MultiSourceHyperlinkIterable {
        regularCollector.collectors.removeIf { it is FetchCacheCollector }
        fetchCaches.caches.forEach { (priority, fetchCache) ->
            val collector = FetchCacheCollector(fetchCache, priority)
            collector.name = "FetchCacheC@" + hashCode()
            addCollector(collector)
        }
        return this
    }

    fun addCollector(collector: PriorityDataCollector<UrlAware>): MultiSourceHyperlinkIterable {
        regularCollector.collectors += collector
        return this
    }

    fun addCollectors(collectors: Iterable<PriorityDataCollector<UrlAware>>): MultiSourceHyperlinkIterable {
        regularCollector.collectors += collectors
        return this
    }

    fun clear() {
        realTimeCollector.fetchCache.clear()
        delayCollector.queue.clear()
        regularCollector.collectors.clear()
    }
}
