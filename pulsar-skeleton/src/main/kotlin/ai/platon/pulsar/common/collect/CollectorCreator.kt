package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CollectorCreator(
    val fetchCaches: FetchCacheManager,
    val collectors: ConcurrentLinkedQueue<PriorityDataCollector<UrlAware>>,
    val urlLoader: ExternalUrlLoader
) {
    private val logger = getLogger(this)

    fun addDefaultCollectors() {
        fetchCaches.caches.forEach { (priority, fetchCache) ->
            collectors += FetchCacheCollector(fetchCache, priority)
        }
    }

    fun addFetchCacheCollector(name: String, priority: Int): FetchCacheCollector {
        val fetchCache = LoadingFetchCache(name, urlLoader, priority)
        fetchCaches.unorderedCaches.add(fetchCache)
        val collector = FetchCacheCollector(fetchCache, priority)

        reportCollector(collector)

        collectors += collector

        return collector
    }

    fun addQueueCollector(name: String, priority: Int, queue: Queue<UrlAware>): QueueCollector {
        val collector = QueueCollector(queue, priority).also {
            it.name = name
        }

        reportCollector(collector)
        collectors += collector

        return collector
    }

    fun reportCollector(collector: DataCollector<out UrlAware>, message: String = "") {
        val msg = if (message.isBlank()) "" else " | $message"

        val dcLog = getLogger(DataCollector::class)
        dcLog?.info(
            "Running task <{}> with {}/{} items{}",
            collector.name, collector.size, collector.estimatedSize, msg
        )

        logger.info(
            "Running task <{}> with {}/{} items{}",
            collector.name, collector.size, collector.estimatedSize, msg
        )
        logger.info("{}", collector)
    }
}
