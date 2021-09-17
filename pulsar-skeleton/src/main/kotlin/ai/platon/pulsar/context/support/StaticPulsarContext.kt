package ai.platon.pulsar.context.support

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers
import ai.platon.pulsar.persist.WebDb
import org.springframework.context.support.StaticApplicationContext

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
class StaticPulsarContext(
    override val applicationContext: StaticApplicationContext = StaticApplicationContext()
) : BasicPulsarContext(applicationContext) {
    /**
     * The unmodified config
     * */
    override val unmodifiedConfig = getBeanOrNull() ?: ImmutableConfig()
    /**
     * Url normalizers
     * */
    override val urlNormalizers = getBeanOrNull() ?: CrawlUrlNormalizers(unmodifiedConfig)
    /**
     * The web db
     * */
    override val webDb = getBeanOrNull() ?: WebDb(unmodifiedConfig)
    /**
     * The global cache
     * */
    override val globalCacheFactory = getBeanOrNull() ?: GlobalCacheFactory(unmodifiedConfig)
    /**
     * The inject component
     * */
    override val injectComponent = getBeanOrNull() ?: InjectComponent(webDb, unmodifiedConfig)
    /**
     * The fetch component
     * */
    override val fetchComponent = getBeanOrNull() ?: BatchFetchComponent(webDb, unmodifiedConfig)
    /**
     * The parse component
     * */
    override val parseComponent: ParseComponent = getBeanOrNull() ?: ParseComponent(globalCacheFactory, unmodifiedConfig)
    /**
     * The update component
     * */
    override val updateComponent = getBeanOrNull() ?: UpdateComponent(webDb, unmodifiedConfig)
    /**
     * The load component
     * */
    override val loadComponent = getBeanOrNull() ?: LoadComponent(
        webDb, globalCacheFactory, fetchComponent, parseComponent, updateComponent, unmodifiedConfig)

    /**
     * The main loop
     * */
    override val crawlLoops: CrawlLoops = getBeanOrNull() ?: CrawlLoops(mutableListOf(StreamingCrawlLoop(globalCacheFactory, unmodifiedConfig)))

    init {
        applicationContext.refresh()
    }
}
