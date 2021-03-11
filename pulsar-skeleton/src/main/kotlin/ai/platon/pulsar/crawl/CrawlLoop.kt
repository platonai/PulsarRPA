package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.StartStopRunnable
import ai.platon.pulsar.common.collect.DataCollector
import ai.platon.pulsar.common.collect.MultiSourceHyperlinkIterable
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.crawl.common.GlobalCache
import java.util.*

interface CrawlLoop: StartStopRunnable {
    var options: LoadOptions
    val fetchIterable: Iterable<Hyperlink>
    val collectors: Queue<out DataCollector<Hyperlink>>
    val crawler: Crawler
}

abstract class AbstractCrawlLoop(
        val globalCache: GlobalCache,
        override var options: LoadOptions = LoadOptions.create()
) : CrawlLoop {
    /**
     * Data collector lower capacity
     * */
    var dcLowerCapacity = 100
    /**
     * The fetch iterable from which all fetch tasks are taken
     * */
    override val fetchIterable by lazy {
        MultiSourceHyperlinkIterable(globalCache.fetchCacheManager, dcLowerCapacity)
    }
    /**
     * The shortcut for all collectors
     * */
    override val collectors get() = fetchIterable.collectors

    abstract override val crawler: AbstractCrawler
}
