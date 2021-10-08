package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.collect.collector.FetchCacheCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.QueueCollector
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CollectorHelper(val fetchIterable: MultiSourceHyperlinkIterable) {
    private val dcLogger = getLogger(DataCollector::class)
    private val fetchCaches get() = fetchIterable.fetchCaches

    fun getCollectors(name: String): List<PriorityDataCollector<UrlAware>> = fetchIterable.getCollectors(name)

    fun getCollectors(names: Iterable<String>): List<PriorityDataCollector<UrlAware>> =
        fetchIterable.getCollectors(names)

    fun getCollectors(regex: Regex): List<PriorityDataCollector<UrlAware>> = fetchIterable.getCollectors(regex)

    fun getCollectorsLike(name: String): List<PriorityDataCollector<UrlAware>> = fetchIterable.getCollectorsLike(name)

    fun contains(name: String): Boolean = getCollectors(name).isNotEmpty()

    fun contains(names: Iterable<String>): Boolean = getCollectors(names).isNotEmpty()

    fun contains(regex: Regex): Boolean = getCollectors(regex).isNotEmpty()

    fun containsLike(name: String): Boolean = getCollectorsLike(name).isNotEmpty()

    fun addDefaults() {
        fetchIterable.addDefaultCollectors()
    }

    fun add(collector: PriorityDataCollector<UrlAware>) {
        addAll(listOf(collector))
    }

    fun addAll(collectors: Iterable<PriorityDataCollector<UrlAware>>) {
        collectors.filterIsInstance<FetchCacheCollector>().forEach {
            fetchCaches.unorderedCaches.add(it.fetchCache)
        }
        collectors.forEach { report(it) }
        fetchIterable.addCollectors(collectors)
    }

    fun addFetchCacheCollector(priority: Int, urlLoader: ExternalUrlLoader): FetchCacheCollector {
        return addFetchCacheCollector("", priority, urlLoader).also { it.name = "LFC@" + it.id }
    }

    fun addFetchCacheCollector(name: String, priority: Int, urlLoader: ExternalUrlLoader): FetchCacheCollector {
        val fetchCache = LoadingFetchCache(name, priority, urlLoader)
        fetchCaches.unorderedCaches.add(fetchCache)
        val collector = FetchCacheCollector(fetchCache).also { it.name = name }

        report(collector)
        fetchIterable.addCollector(collector)

        return collector
    }

    fun addFetchCacheCollector(priority: Int): FetchCacheCollector {
        return addFetchCacheCollector("", priority).also { it.name = "FC@" + it.id }
    }

    fun addFetchCacheCollector(name: String, priority: Int): FetchCacheCollector {
        val fetchCache = ConcurrentFetchCache(name)
        fetchCaches.unorderedCaches.add(fetchCache)
        val collector = FetchCacheCollector(fetchCache).also { it.name = name }

        fetchIterable.addCollector(collector)
        report(collector)

        return collector
    }

    fun addQueueCollector(
        name: String,
        priority: Int,
        queue: Queue<UrlAware> = ConcurrentLinkedQueue()
    ): QueueCollector {
        val collector = QueueCollector(queue, priority).also { it.name = name }

        fetchIterable.addCollector(collector)
        report(collector)

        return collector
    }

    fun remove(name: String): DataCollector<UrlAware>? {
        return removeAll(listOf(name)).firstOrNull()
    }

    fun removeAll(names: Iterable<String>): Collection<DataCollector<UrlAware>> {
        val collectors = fetchIterable.getCollectors(names)
        return removeAll(collectors)
    }

    fun removeAll(regex: Regex): Collection<DataCollector<UrlAware>> {
        val collectors = fetchIterable.getCollectors(regex)
        return removeAll(collectors)
    }

    fun removeAllLike(name: String): Collection<DataCollector<UrlAware>> {
        return removeAll(".*$name.*".toRegex())
    }

    fun removeAll(collectors: Collection<DataCollector<UrlAware>>): Collection<DataCollector<UrlAware>> {
        fetchIterable.removeAll(collectors)
        collectors.filterIsInstance<FetchCacheCollector>().map { it.fetchCache }
            .let { fetchCaches.unorderedCaches.removeAll(it) }

        if (collectors.isNotEmpty()) {
            dcLogger.info("Removed collectors: " + collectors.joinToString { it.name })
            collectors.forEachIndexed { i, c -> dcLogger.info("${i + 1}.\t$c") }
            dcLogger.info("")
        }
        return collectors
    }

    fun report(collector: DataCollector<out UrlAware>, message: String = "") {
        val msg = if (message.isBlank()) "" else " | $message"

        dcLogger.info("Task <{}> has {}/{} items{}, adding to {}@{}",
            collector.name, collector.size, collector.estimatedSize, msg,
            fetchIterable.openCollectors.javaClass.simpleName,
            fetchIterable.openCollectors.hashCode())
        dcLogger.info("{}", collector)
    }
}
