package ai.platon.pulsar.skeleton.crawl.impl

import ai.platon.pulsar.common.collect.UrlFeeder
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.skeleton.crawl.CrawlLoop
import ai.platon.pulsar.skeleton.crawl.Crawler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractCrawlLoop(
    override val name: String,
    override val config: ImmutableConfig
) : CrawlLoop {
    companion object {
        private val ID_SUPPLIER = AtomicInteger()
    }

    protected val running = AtomicBoolean()

    override val id: Int = ID_SUPPLIER.incrementAndGet()

    override val isRunning: Boolean
        get() = running.get()

    /**
     * The url feeder is used by the crawl loop to feed urls to the crawler.
     * */
    abstract override val urlFeeder: UrlFeeder
    /**
     * The shortcut for all collectors
     * */
    override val collectors: List<PriorityDataCollector<UrlAware>>
        get() = urlFeeder.collectors

    abstract override val crawler: Crawler

    override val display: String get() = "CrawlLoop#$id:$name"

    override val abstract: String get() = urlFeeder.abstract

    override val report: String get() = urlFeeder.report
}
