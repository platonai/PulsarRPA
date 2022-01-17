package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.collect.collector.FetchCacheCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollectorsFormatter
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlAware

class MultiSourceHyperlinkIterable(
    val fetchCaches: FetchCacheManager,
    val lowerCacheSize: Int = 100,
    val enableDefaults: Boolean = false
) : Iterable<UrlAware> {
    private val logger = getLogger(this)

    private val realTimeCollector = FetchCacheCollector(fetchCaches.realTimeCache)
        .apply { name = "FCC#RealTime" }
    private val delayCollector = DelayCacheCollector(fetchCaches.delayCache, Priority13.HIGHER5)
        .apply { name = "DelayCC#Delay" }

    val loadingIterable =
        ConcurrentLoadingIterable(CombinedDataCollector(), realTimeCollector, delayCollector, lowerCacheSize)
    val cacheSize get() = loadingIterable.cacheSize

    private val combinedDataCollector get() = loadingIterable.regularCollector as CombinedDataCollector

    val openCollectors: Collection<PriorityDataCollector<UrlAware>>
        get() = combinedDataCollector.collectors

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

    /**
     * Estimate the order to fetch for the next task to add with priority [priority]
     * */
    fun estimatedOrder(priority: Int): Int {
        return kotlin.runCatching { doEstimatedOrder(priority) }
            .onFailure { logger.warn(it.stringify()) }
            .getOrDefault(-2)
    }

    private fun doEstimatedOrder(priority: Int): Int {
        val priorCount = collectors.asSequence()
            .filter { it.priority < priority }
            .filterNot { it is DelayCacheCollector }
            .sumOf { it.size }

        val now = System.currentTimeMillis()
        // the fetch speed, pages per second
        val speed = 1.0
        var delayQueueCount = delayCollector.queue.count { it.startTime - now < speed * priorCount }
        // if a task in delayed queue is ready to run in 15 seconds, count it
        if (delayQueueCount == 0) {
            delayQueueCount = delayCollector.queue.count { it.startTime - now < 15_000 }
        }

        val competitorCollectors = collectors.filter { it.priority == priority }
        val competitorCount = competitorCollectors.sumOf { it.size }

        return priorCount + delayQueueCount + competitorCount / competitorCollectors.size
    }

    fun addDefaultCollectors(): MultiSourceHyperlinkIterable {
        combinedDataCollector.collectors.removeIf { it is FetchCacheCollector }
        fetchCaches.orderedCaches.values.forEach { fetchCache ->
            addCollector(FetchCacheCollector(fetchCache).apply { name = "FCC.$id" })
        }
        return this
    }

    fun addCollector(collector: PriorityDataCollector<UrlAware>): MultiSourceHyperlinkIterable {
        combinedDataCollector.collectors += collector
        return this
    }

    fun addCollectors(collectors: Iterable<PriorityDataCollector<UrlAware>>): MultiSourceHyperlinkIterable {
        combinedDataCollector.collectors += collectors
        return this
    }

    fun getCollectors(name: String): List<PriorityDataCollector<UrlAware>> {
        return combinedDataCollector.collectors.filter { it.name == name }
    }

    fun getCollectors(names: Iterable<String>): List<PriorityDataCollector<UrlAware>> {
        return combinedDataCollector.collectors.filter { it.name in names }
    }

    fun getCollectors(regex: Regex): List<PriorityDataCollector<UrlAware>> {
        return combinedDataCollector.collectors.filter { it.name.matches(regex) }
    }

    fun getCollectorsLike(name: String): List<PriorityDataCollector<UrlAware>> {
        return getCollectors(".*$name.*".toRegex())
    }

    fun remove(collector: DataCollector<UrlAware>): Boolean {
        return combinedDataCollector.collectors.remove(collector)
    }

    fun removeAll(collectors: Collection<DataCollector<UrlAware>>): Boolean {
        return combinedDataCollector.collectors.removeAll(collectors)
    }

    fun clear() {
        loadingIterable.clear()
        realTimeCollector.fetchCache.clear()
        delayCollector.queue.clear()
        combinedDataCollector.clear()
    }
}
