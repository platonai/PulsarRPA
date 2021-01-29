package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.url.Hyperlink
import java.util.*

class MultiSourceHyperlinkIterable(
        val fetchCacheManager: FetchCatchManager,
        val lowerCacheSize: Int = 100
) : Iterable<Hyperlink> {
    val loadingIterable = createLoadingIterable()
    val cacheSize get() = loadingIterable.cacheSize
    val collectors get() = (loadingIterable.collector as MultiSourceDataCollector<Hyperlink>).collectors

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

    private fun createLoadingIterable(): ConcurrentLoadingIterable<Hyperlink> {
        val collectors: MutableList<PriorityDataCollector<Hyperlink>> = Collections.synchronizedList(LinkedList())
        val normalCollector = MultiSourceDataCollector(collectors)
        val realTimeCollector = FetchCacheCollector(fetchCacheManager.realTimeCache, Priority13.HIGHEST)
        return ConcurrentLoadingIterable(normalCollector, realTimeCollector, lowerCacheSize)
    }
}
