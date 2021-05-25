package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.StartStopRunnable
import ai.platon.pulsar.common.collect.DataCollector
import ai.platon.pulsar.common.collect.MultiSourceHyperlinkIterable
import ai.platon.pulsar.common.collect.PriorityDataCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.common.GlobalCache
import java.util.*

interface CrawlLoop: StartStopRunnable {
    val unmodifiedConfig: ImmutableConfig
    val defaultOptions: LoadOptions
    val fetchIterable: Iterable<UrlAware>
    val collectors: Queue<out DataCollector<UrlAware>>
    val crawler: Crawler
    val abstract: String
    val report: String
}

abstract class AbstractCrawlLoop(
        val globalCache: GlobalCache,
        override val unmodifiedConfig: ImmutableConfig
) : CrawlLoop {
    /**
     * Data collector lower capacity
     * */
    override var defaultOptions: LoadOptions = LoadOptions.create(unmodifiedConfig.toVolatileConfig())
    /**
     * The fetch iterable from which all fetch tasks are taken
     * */
    override val fetchIterable by lazy {
        MultiSourceHyperlinkIterable(globalCache.fetchCaches)
    }
    /**
     * The shortcut for all collectors
     * */
    override val collectors: Queue<PriorityDataCollector<UrlAware>> get() = fetchIterable.collectors

    abstract override val crawler: AbstractCrawler
}
