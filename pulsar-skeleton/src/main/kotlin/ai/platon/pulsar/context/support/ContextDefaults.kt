package ai.platon.pulsar.context.support

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.component.*
import ai.platon.pulsar.crawl.filter.ChainedUrlNormalizer
import ai.platon.pulsar.crawl.impl.StreamingCrawlLoop
import ai.platon.pulsar.persist.WebDb

class ContextDefaults {

    /**
     * The default unmodified config
     * */
    val unmodifiedConfig = ImmutableConfig()
    /**
     * Url default normalizer
     * */
    val urlNormalizer = ChainedUrlNormalizer()
    /**
     * The default web db
     * */
    val webDb = WebDb(unmodifiedConfig)
    /**
     * The default global cache
     * */
    val globalCacheFactory = GlobalCacheFactory(unmodifiedConfig)
    /**
     * The default injection component
     * */
    val injectComponent = InjectComponent(webDb, unmodifiedConfig)
    /**
     * The default fetch component
     * */
    val fetchComponent = BatchFetchComponent(webDb, unmodifiedConfig)
    /**
     * The default parse component
     * */
    val parseComponent: ParseComponent = ParseComponent(globalCacheFactory, unmodifiedConfig)
    /**
     * The default update component
     * */
    val updateComponent = UpdateComponent(webDb, unmodifiedConfig)
    /**
     * The default load component
     * */
    val loadComponent = LoadComponent(
        webDb, globalCacheFactory, fetchComponent, parseComponent, updateComponent, unmodifiedConfig)
    /**
     * The default main loop
     * */
    val crawlLoops = CrawlLoops(StreamingCrawlLoop(unmodifiedConfig))
}
