package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.QueueCollector
import ai.platon.pulsar.common.collect.collector.UrlCacheCollector
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class UrlFeederHelper(val feeder: UrlFeeder) {
    private val dcLogger = getLogger(DataCollector::class)
    private val urlPool get() = feeder.urlPool

    fun findByName(name: String): List<PriorityDataCollector<UrlAware>> = feeder.findByName(name)

    fun findByName(names: Iterable<String>): List<PriorityDataCollector<UrlAware>> =
        feeder.findByName(names)

    fun findByName(regex: Regex): List<PriorityDataCollector<UrlAware>> = feeder.findByName(regex)

    fun findByNameLike(name: String): List<PriorityDataCollector<UrlAware>> = feeder.findByNameLike(name)

    fun contains(name: String): Boolean = findByName(name).isNotEmpty()

    fun contains(names: Iterable<String>): Boolean = findByName(names).isNotEmpty()

    fun contains(regex: Regex): Boolean = findByName(regex).isNotEmpty()

    fun containsLike(name: String): Boolean = findByNameLike(name).isNotEmpty()

    fun addDefaults() {
        feeder.addDefaultCollectors()
    }

    fun add(collector: PriorityDataCollector<UrlAware>) {
        addAll(listOf(collector))
    }

    fun add(
        name: String, priority: Int = Priority13.NORMAL.value, queue: Queue<UrlAware> = ConcurrentLinkedQueue()
    ): QueueCollector {
        val collector = QueueCollector(queue, priority).also { it.name = name }

        feeder.addCollector(collector)
        report(collector)

        return collector
    }

    fun addAll(collectors: Iterable<PriorityDataCollector<UrlAware>>) {
        collectors.filterIsInstance<UrlCacheCollector>().forEach {
            urlPool.orderedCaches[it.priority] = it.urlCache
        }
        collectors.forEach { report(it) }
        feeder.addCollectors(collectors)
    }

    fun create(priority: Int, urlLoader: ExternalUrlLoader): UrlCacheCollector {
        return create("", priority, urlLoader).also { it.name = "LFC@" + it.id }
    }

    fun create(name: String, priority: Int, urlLoader: ExternalUrlLoader): UrlCacheCollector {
        val urlCache = LoadingUrlCache(name, priority, urlLoader)
        urlPool.unorderedCaches.add(urlCache)
        val collector = UrlCacheCollector(urlCache).also { it.name = name }

        report(collector)
        feeder.addCollector(collector)

        return collector
    }

    fun create(priority: Int): UrlCacheCollector {
        return create("", priority).also { it.name = "FC@" + it.id }
    }

    fun create(name: String, priority: Int): UrlCacheCollector {
        val urlCache = ConcurrentUrlCache(name)
        urlPool.unorderedCaches.add(urlCache)
        val collector = UrlCacheCollector(urlCache).also { it.name = name }

        feeder.addCollector(collector)
        report(collector)

        return collector
    }

    fun remove(name: String): DataCollector<UrlAware>? {
        return removeAll(listOf(name)).firstOrNull()
    }

    fun removeAll(names: Iterable<String>): Collection<DataCollector<UrlAware>> {
        val collectors = feeder.findByName(names)
        return removeAll(collectors)
    }

    fun removeAll(regex: Regex): Collection<DataCollector<UrlAware>> {
        val collectors = feeder.findByName(regex)
        return removeAll(collectors)
    }

    fun removeAllLike(name: String): Collection<DataCollector<UrlAware>> {
        return removeAll(".*$name.*".toRegex())
    }

    fun removeAll(collectors: Collection<DataCollector<UrlAware>>): Collection<DataCollector<UrlAware>> {
        feeder.removeAll(collectors)
        collectors.filterIsInstance<UrlCacheCollector>().map { it.urlCache }
            .let { urlPool.unorderedCaches.removeAll(it) }

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
            feeder.openCollectors.javaClass.simpleName,
            feeder.openCollectors.hashCode())
        dcLogger.info("{}", collector)
    }
}
