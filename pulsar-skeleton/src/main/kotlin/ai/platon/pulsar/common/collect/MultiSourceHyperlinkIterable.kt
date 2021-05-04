package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.urls.Hyperlink

class MultiSourceHyperlinkIterable(
    val fetchCacheManager: FetchCatchManager,
    val lowerCacheSize: Int = 100,
) : Iterable<Hyperlink> {
    private val realTimeCollector = FetchCacheCollector(fetchCacheManager.realTimeCache, Priority13.HIGHEST)
        .apply { name = "FetchCacheCollector@RealTime" }
    private val delayCollector = DelayCacheCollector(fetchCacheManager.delayCache, Priority13.HIGHER5)
        .apply { name = "DelayCacheCollector@Delay" }
    private val multiSourceCollector = MultiSourceDataCollector<Hyperlink>()

    val loadingIterable =
        ConcurrentLoadingIterable(multiSourceCollector, realTimeCollector, delayCollector, lowerCacheSize)
    val cacheSize get() = loadingIterable.cacheSize

    // TODO: add realTimeCollector and delayCollector
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
