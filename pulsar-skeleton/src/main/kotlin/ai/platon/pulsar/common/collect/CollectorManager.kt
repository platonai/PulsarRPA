package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CollectorManager(val fetchIterable: MultiSourceHyperlinkIterable) {
    private val dcLogger = getLogger(DataCollector::class)
    private val fetchCaches get() = fetchIterable.fetchCaches

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

    fun addFetchCacheCollector(name: String, priority: Int, urlLoader: ExternalUrlLoader): FetchCacheCollector {
        val fetchCache = LoadingFetchCache(name, urlLoader, priority)
        fetchCaches.unorderedCaches.add(fetchCache)
        val collector = FetchCacheCollector(fetchCache, priority).also { it.name = name }

        report(collector)
        fetchIterable.addCollector(collector)

        return collector
    }

    fun addQueueCollector(
        name: String,
        priority: Int,
        queue: Queue<UrlAware> = ConcurrentLinkedQueue()
    ): QueueCollector {
        val collector = QueueCollector(queue, priority).also { it.name = name }

        report(collector)
        fetchIterable.addCollector(collector)

        return collector
    }

    fun remove(name: String) {
        removeAll(listOf(name))
    }

    fun removeAll(names: Iterable<String>) {
        val collectors = fetchIterable.getCollectors(names)
        removeAll(collectors)
    }

    fun removeAll(regex: Regex) {
        val collectors = fetchIterable.getCollectors(regex)
        removeAll(collectors)
    }

    fun removeAll(collectors: Collection<PriorityDataCollector<UrlAware>>) {
        fetchIterable.removeAll(collectors)
        collectors.filterIsInstance<FetchCacheCollector>().map { it.fetchCache }
            .let { fetchCaches.unorderedCaches.removeAll(it) }

        dcLogger.info("Removed collectors:")
        collectors.forEachIndexed { i, c -> dcLogger.info("$i.\t$c") }
    }

    fun report(collector: DataCollector<out UrlAware>, message: String = "") {
        val msg = if (message.isBlank()) "" else " | $message"

        val dcLog = getLogger(DataCollector::class)
        dcLog?.info(
            "Running task <{}> with {}/{} items{}",
            collector.name, collector.size, collector.estimatedSize, msg
        )

        dcLogger.info(
            "Running task <{}> with {}/{} items{}",
            collector.name, collector.size, collector.estimatedSize, msg
        )
        dcLogger.info("{}", collector)
    }
}
