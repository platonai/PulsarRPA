package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.FetchCacheCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollectorsFormatter
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*

class MultiSourceHyperlinkIterable(
    val fetchCaches: FetchCacheManager,
    val lowerCacheSize: Int = 100,
    val enableDefaults: Boolean = false
) : Iterable<UrlAware> {
    private val logger = getLogger(this)

    private val realTimeCollector = FetchCacheCollector(fetchCaches.realTimeCache, Priority13.HIGHEST)
        .apply { name = "FCC@RealTime" }
    private val delayCollector = DelayCacheCollector(fetchCaches.delayCache, Priority13.HIGHER5)
        .apply { name = "DelayCC@Delay" }

    val loadingIterable =
        ConcurrentLoadingIterable(MultiSourceDataCollector(), realTimeCollector, delayCollector, lowerCacheSize)
    val cacheSize get() = loadingIterable.cacheSize

    private val multiSourceCollector get() = loadingIterable.regularCollector as MultiSourceDataCollector

    val openCollectors: Collection<PriorityDataCollector<UrlAware>>
        get() = multiSourceCollector.collectors

    val collectors: List<PriorityDataCollector<UrlAware>>
        get() = mutableListOf<PriorityDataCollector<UrlAware>>().also {
            it += realTimeCollector
            it += delayCollector
            it += openCollectors
        }.sortedBy { it.priority }

    init {
        if (enableDefaults && openCollectors.isEmpty()) {
            addDefaultCollectors()
        }
    }

    val abstract: String
        get() = PriorityDataCollectorsFormatter(collectors).abstract()

    val report: String
        get() = PriorityDataCollectorsFormatter(collectors).toString()

    /**
     * Add a hyperlink to the very beginning of the fetch queue, so it will be served immediately
     * */
    fun addFirst(url: UrlAware) = loadingIterable.addFirst(url)

    fun addLast(url: UrlAware) = loadingIterable.addLast(url)

    override fun iterator(): Iterator<UrlAware> = loadingIterable.iterator()

    fun estimatedOrder(priority: Int): Int {
        val priorCount = collectors.filter { it.priority > priority }.sumBy { it.estimatedSize }
        val competitorCollectors = collectors.filter { it.priority == priority }
        val competitorCount = competitorCollectors.sumBy { it.estimatedSize }

        return priorCount + competitorCount / competitorCollectors.size
    }

    fun addDefaultCollectors(): MultiSourceHyperlinkIterable {
        multiSourceCollector.collectors.removeIf { it is FetchCacheCollector }
        fetchCaches.caches.forEach { (priority, fetchCache) ->
            val collector = FetchCacheCollector(fetchCache, priority)
            collector.name = "FC@" + collector.id
            addCollector(collector)
        }
        return this
    }

    fun addCollector(collector: PriorityDataCollector<UrlAware>): MultiSourceHyperlinkIterable {
        multiSourceCollector.collectors += collector
        return this
    }

    fun addCollectors(collectors: Iterable<PriorityDataCollector<UrlAware>>): MultiSourceHyperlinkIterable {
        multiSourceCollector.collectors += collectors
        return this
    }

    fun getCollectors(name: String): List<PriorityDataCollector<UrlAware>> {
        return multiSourceCollector.collectors.filter { it.name == name }
    }

    fun getCollectors(names: Iterable<String>): List<PriorityDataCollector<UrlAware>> {
        return multiSourceCollector.collectors.filter { it.name in names }
    }

    fun getCollectors(regex: Regex): List<PriorityDataCollector<UrlAware>> {
        return multiSourceCollector.collectors.filter { it.name.matches(regex) }
    }

    fun getCollectorsLike(name: String): List<PriorityDataCollector<UrlAware>> {
        return getCollectors(".*$name.*".toRegex())
    }

    fun remove(collector: PriorityDataCollector<UrlAware>): Boolean {
        return multiSourceCollector.collectors.remove(collector)
    }

    fun removeAll(collectors: Collection<PriorityDataCollector<UrlAware>>): Boolean {
        return multiSourceCollector.collectors.removeAll(collectors)
    }

    fun clear() {
        loadingIterable.clear()
        realTimeCollector.fetchCache.clear()
        delayCollector.queue.clear()
        multiSourceCollector.clear()
    }
}
