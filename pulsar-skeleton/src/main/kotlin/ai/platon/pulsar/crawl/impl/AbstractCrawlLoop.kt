package ai.platon.pulsar.crawl.impl

import ai.platon.pulsar.common.collect.UrlFeeder
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.CrawlLoop
import ai.platon.pulsar.crawl.Crawler
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractCrawlLoop(
    override val name: String,
    override val config: ImmutableConfig
) : CrawlLoop {
    companion object {
        private val idGen = AtomicInteger()
    }

    override val id: Int = idGen.incrementAndGet()

    /**
     * Data collector lower capacity
     * */
    override var defaultOptions: LoadOptions = LoadOptions.create(config.toVolatileConfig())

    /**
     * The fetch iterable from which all fetch tasks are taken
     * */
    abstract override val urlFeeder: UrlFeeder
    /**
     * The shortcut for all collectors
     * */
    override val collectors: List<PriorityDataCollector<UrlAware>>
        get() = urlFeeder.collectors

    abstract override val crawler: Crawler

    override val abstract: String get() = urlFeeder.abstract

    override val report: String get() = urlFeeder.report
}
