package ai.platon.pulsar.context.support

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.impl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.ChainedUrlNormalizer
import ai.platon.pulsar.persist.WebDb
import org.springframework.context.support.StaticApplicationContext

class StaticPulsarContext(
    applicationContext: StaticApplicationContext = StaticApplicationContext()
) : BasicPulsarContext(applicationContext) {

    /**
     * The unmodified config
     * */
    override val unmodifiedConfig = getBeanOrNull() ?: ImmutableConfig()
    /**
     * Url normalizers
     * */
    override val urlNormalizers = getBeanOrNull() ?: ChainedUrlNormalizer(unmodifiedConfig)
    /**
     * The web db
     * */
    override val webDb = getBeanOrNull() ?: WebDb(unmodifiedConfig)
    /**
     * The global cache
     * */
    override val globalCacheFactory = getBeanOrNull() ?: GlobalCacheFactory(unmodifiedConfig)
    /**
     * The injection component
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
    override val crawlLoops: CrawlLoops = getBeanOrNull() ?: CrawlLoops(StreamingCrawlLoop(unmodifiedConfig))

    init {
        applicationContext.refresh()
    }
}
