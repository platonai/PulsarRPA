package ai.platon.pulsar.context.support

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.GlobalCache
import ai.platon.pulsar.crawl.component.BatchFetchComponent
import ai.platon.pulsar.crawl.component.InjectComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.UrlNormalizers
import ai.platon.pulsar.persist.WebDb
import org.springframework.context.support.StaticApplicationContext

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
open class BasicPulsarContext(
        applicationContext: StaticApplicationContext = StaticApplicationContext()
): GenericPulsarContext(applicationContext) {
    /**
     * The unmodified config
     * */
    final override val unmodifiedConfig = ImmutableConfig()
    /**
     * Url normalizers
     * */
    final override val urlNormalizers = UrlNormalizers(unmodifiedConfig)
    /**
     * The web db
     * */
    final override val webDb = WebDb(unmodifiedConfig)
    /**
     * The inject component
     * */
    final override val injectComponent = InjectComponent(webDb, unmodifiedConfig)
    /**
     * The fetch component
     * */
    final override val fetchComponent = BatchFetchComponent(webDb, unmodifiedConfig)
    /**
     * The update component
     * */
    final override val updateComponent = UpdateComponent(webDb, unmodifiedConfig)
    /**
     * The load component
     * */
    final override val loadComponent = LoadComponent(webDb, fetchComponent, updateComponent, unmodifiedConfig)
    /**
     * The global cache manager
     * */
    final override val globalCache = GlobalCache(unmodifiedConfig)

    init {
        applicationContext.refresh()
    }
}
