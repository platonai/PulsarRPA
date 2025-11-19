package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollectorsFormatter
import ai.platon.pulsar.common.collect.collector.UrlCacheCollector
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.warnInterruptible
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * The url feeder collects urls from the url pool and feed them to the crawlers.
 *
 * The url feed collect urls using [DataCollector], each [DataCollector] collect urls
 * from exactly one [UrlCache].
 *
 * The user can register multiple [UrlCache]s and [DataCollector]s for different type of tasks.
 * */
class UrlFeeder(
    val urlPool: UrlPool,
    val lowerCacheSize: Int = 100,
    val enableDefaults: Boolean = false
) : Iterable<UrlAware> {
    private val logger = getLogger(this)

    private val realTimeCollector = UrlCacheCollector(urlPool.realTimeCache)
        .apply { name = "FCC#RealTime" }
    private val delayCollector = DelayCacheCollector(urlPool.delayCache, Priority13.HIGHER5)
        .apply { name = "DelayCC#Delay" }

    private val chainedDataCollector get() = loadingIterable.regularCollector as ChainedDataCollector

    val id get() = urlPool.id

    /**
     * Open collectors
     * */
    val openCollectors: Collection<PriorityDataCollector<UrlAware>>
        get() = chainedDataCollector.collectors

    /**
     * A copy of all collectors, including the real-time collector, delay collector and open collectors.
     * */
    val collectors: List<PriorityDataCollector<UrlAware>>
        get() = mutableListOf<PriorityDataCollector<UrlAware>>().also {
            it += realTimeCollector
            it += delayCollector
            it += openCollectors
        }.sortedBy { it.priority }

    /**
     * The loading iterable is a concurrent loading iterable, which loads urls from the url pool
     * and feeds them to the crawlers.
     * */
    val loadingIterable =
        ConcurrentLoadingIterable(ChainedDataCollector(), realTimeCollector, delayCollector, lowerCacheSize)

    val cacheSize get() = loadingIterable.cacheSize

    val size get() = loadingIterable.size

    /**
     * The estimated size of the fetch queue, which is the sum of the size of all collectors.
     * When the collector loads urls from external sources, retrieving exact size of the fetch queue
     * is not possible. So we have to estimate the size.
     * */
    val estimatedSize get() = loadingIterable.estimatedSize

    val abstract: String
        get() = PriorityDataCollectorsFormatter(collectors).abstract()

    val report: String
        get() = PriorityDataCollectorsFormatter(collectors).toString()

    init {
        if (enableDefaults && openCollectors.isEmpty()) {
            addDefaultCollectors()
        }
    }

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun isNotEmpty() = !isEmpty()

    /**
     * Add a hyperlink to the very beginning of the fetch queue, so it will be served first
     * */
    fun addFirst(url: UrlAware) = loadingIterable.addFirst(url)

    /**
     * Add a hyperlink to the end of the fetch queue, so it will be served last
     * */
    fun addLast(url: UrlAware) = loadingIterable.addLast(url)

    override fun iterator(): Iterator<UrlAware> = loadingIterable.iterator()

    /**
     * Estimate the order to fetch for the next task to add with priority [priority].
     * */
    fun estimatedOrder(priority: Int): Int {
        return kotlin.runCatching { doEstimatedOrder(priority) }
            .onFailure { warnInterruptible(this, it) }
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
        var delayQueueCount = delayCollector.queue.count { it.delayExpireAt - now < speed * priorCount }
        // if a task in delayed queue is ready to run in 15 seconds, count it
        if (delayQueueCount == 0) {
            delayQueueCount = delayCollector.queue.count { it.delayExpireAt - now < 15_000 }
        }

        val competitorCollectors = collectors.filter { it.priority == priority }
        val competitorCount = competitorCollectors.sumOf { it.size }

        return priorCount + delayQueueCount + competitorCount / competitorCollectors.size
    }

    fun addDefaultCollectors(): UrlFeeder {
        chainedDataCollector.collectors.removeIf { it is UrlCacheCollector }
        urlPool.orderedCaches.values.forEach { urlCache ->
            addCollector(UrlCacheCollector(urlCache).apply { name = "FCC.$id" })
        }
        return this
    }

    fun addCollector(collector: PriorityDataCollector<UrlAware>): UrlFeeder {
        chainedDataCollector.collectors += collector
        return this
    }

    fun addCollectors(collectors: Iterable<PriorityDataCollector<UrlAware>>): UrlFeeder {
        chainedDataCollector.collectors += collectors
        return this
    }

    fun findByName(name: String): List<PriorityDataCollector<UrlAware>> {
        return chainedDataCollector.collectors.filter { it.name == name }
    }

    fun findByName(names: Iterable<String>): List<PriorityDataCollector<UrlAware>> {
        return chainedDataCollector.collectors.filter { it.name in names }
    }

    fun findByName(regex: Regex): List<PriorityDataCollector<UrlAware>> {
        return chainedDataCollector.collectors.filter { it.name.matches(regex) }
    }

    fun findByNameLike(name: String): List<PriorityDataCollector<UrlAware>> {
        return findByName(".*$name.*".toRegex())
    }

    fun remove(collector: DataCollector<UrlAware>): Boolean {
        return chainedDataCollector.collectors.remove(collector)
    }

    fun removeAll(collectors: Collection<DataCollector<UrlAware>>): Boolean {
        return chainedDataCollector.collectors.removeAll(collectors)
    }

    fun clear() {
        loadingIterable.clear()
        realTimeCollector.urlCache.clear()
        delayCollector.queue.clear()
        chainedDataCollector.clear()
    }
}
