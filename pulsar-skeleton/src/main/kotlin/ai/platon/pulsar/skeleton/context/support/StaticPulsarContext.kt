package ai.platon.pulsar.skeleton.context.support

import org.springframework.context.support.StaticApplicationContext

class StaticPulsarContext(
    applicationContext: StaticApplicationContext = StaticApplicationContext()
) : BasicPulsarContext(applicationContext) {
    private val defaults = ContextDefaults()

    /**
     * The unmodified config
     * */
    override val unmodifiedConfig get() = getBeanOrNull() ?: defaults.unmodifiedConfig
    /**
     * Url normalizers
     * */
    @Deprecated("Inappropriate name", replaceWith = ReplaceWith("urlNormalizer"))
    override val urlNormalizers get() = getBeanOrNull() ?: defaults.urlNormalizer
    /**
     * Url normalizer
     * */
    override val urlNormalizer get() = getBeanOrNull() ?: defaults.urlNormalizer

    /**
     * The web db
     * */
    override val webDb get() = getBeanOrNull() ?: defaults.webDb
    /**
     * The global cache
     * */
    override val globalCacheFactory get() = getBeanOrNull() ?: defaults.globalCacheFactory
    /**
     * The injection component
     * */
    override val injectComponent get() = getBeanOrNull() ?: defaults.injectComponent
    /**
     * The fetch component
     * */
    override val fetchComponent get() = getBeanOrNull() ?: defaults.fetchComponent
    /**
     * The parse component
     * */
    override val parseComponent get() = getBeanOrNull() ?: defaults.parseComponent
    /**
     * The update component
     * */
    override val updateComponent get() = getBeanOrNull() ?: defaults.updateComponent
    /**
     * The load component
     * */
    override val loadComponent get() = getBeanOrNull() ?: defaults.loadComponent
    /**
     * The main loop
     * */
    override val crawlLoops get() = getBeanOrNull() ?: defaults.crawlLoops

    init {
        applicationContext.refresh()
    }
}
