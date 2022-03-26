package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.StartStopRunnable
import ai.platon.pulsar.common.collect.MultiSourceHyperlinkIterable
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import java.util.concurrent.atomic.AtomicInteger

interface CrawlLoop: StartStopRunnable {
    val id: Int
    val name: String
    val config: ImmutableConfig
    val defaultOptions: LoadOptions
    val fetchTaskIterable: Iterable<UrlAware>
    val collectors: List<out DataCollector<UrlAware>>
    val crawler: Crawler
    val abstract: String
    val report: String
}

abstract class AbstractCrawlLoop(
    override val name: String,
    override val config: ImmutableConfig
) : CrawlLoop {
    companion object {
        val idGen = AtomicInteger()
    }

    override val id: Int = idGen.incrementAndGet()

    /**
     * Data collector lower capacity
     * */
    override var defaultOptions: LoadOptions = LoadOptions.create(config.toVolatileConfig())

    /**
     * The fetch iterable from which all fetch tasks are taken
     * */
    abstract override val fetchTaskIterable: MultiSourceHyperlinkIterable
    /**
     * The shortcut for all collectors
     * */
    override val collectors: List<PriorityDataCollector<UrlAware>>
        get() = fetchTaskIterable.collectors

    abstract override val crawler: AbstractCrawler

    override val abstract: String get() = fetchTaskIterable.abstract

    override val report: String get() = fetchTaskIterable.report
}
