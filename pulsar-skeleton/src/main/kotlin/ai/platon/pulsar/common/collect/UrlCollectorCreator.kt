package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class UrlCollectorCreator(
    val fetchCacheManager: FetchCatchManager,
    val collectors: ConcurrentLinkedQueue<PriorityDataCollector<UrlAware>>,
    val urlLoader: ExternalUrlLoader
) {
    private val logger = getLogger(this)

    fun addDefaultCollectors() {
        fetchCacheManager.caches.forEach { (priority, fetchCache) ->
            collectors += FetchCacheCollector(fetchCache, priority)
        }
    }

    fun addFetchCacheCollector(name: String, priority: Int, queue: Queue<UrlAware>): FetchCacheCollector {
        val fetchCache = LoadingFetchCache(name, urlLoader, priority)
        fetchCacheManager.unorderedCaches.add(fetchCache)
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

    private fun reportCollector(collector: DataCollector<UrlAware>) {
        val dcLog = getLogger(DataCollector::class)
        logger.info("Running asin task <{}> with {}/{} items", collector.name, collector.size, collector.estimatedSize)
        logger.info("{}", collector)
        dcLog.info("Running asin task <{}> with {}/{} items", collector.name, collector.size, collector.estimatedSize)
    }
}
