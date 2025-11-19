package ai.platon.pulsar.skeleton.context.support

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.crawl.component.*
import ai.platon.pulsar.skeleton.crawl.filter.ChainedUrlNormalizer
import ai.platon.pulsar.skeleton.crawl.impl.StreamingCrawlLoop
import ai.platon.pulsar.persist.WebDb

class ContextDefaults {

    /**
     * The default unmodified config
     * */
    val configuration = ImmutableConfig(loadDefaults = true)
    /**
     * Url default normalizer
     * */
    val urlNormalizer = ChainedUrlNormalizer()
    /**
     * The default web db
     * */
    val webDb = WebDb(configuration)

    /**
     * The default global cache
     * */
    val globalCacheFactory = GlobalCacheFactory(configuration)
    /**
     * The default fetch component
     * */
    val fetchComponent = BatchFetchComponent(webDb, configuration)
    /**
     * The default parse component
     * */
    val parseComponent: ParseComponent = ParseComponent(globalCacheFactory, configuration)
    /**
     * The default update component
     * */
    val updateComponent = UpdateComponent(webDb, configuration)
    /**
     * The default load component
     * */
    val loadComponent = LoadComponent(
        webDb, globalCacheFactory, fetchComponent, parseComponent, updateComponent, configuration)
    /**
     * The default main loop
     * */
    val crawlLoops = CrawlLoops(StreamingCrawlLoop(configuration))
}
