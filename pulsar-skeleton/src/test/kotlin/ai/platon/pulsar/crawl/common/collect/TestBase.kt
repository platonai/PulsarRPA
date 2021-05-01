package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.collect.TemporaryLocalFileUrlLoader
import ai.platon.pulsar.common.collect.UrlGroup
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.common.GlobalCache
import org.junit.Before

open class TestBase {
    protected val conf = ImmutableConfig()
    protected val group = UrlGroup("", 0, 0)
    protected val queueSize = 100
    protected lateinit var urlLoader: TemporaryLocalFileUrlLoader

    protected val globalCache = GlobalCache(conf)
    protected val fetchCacheManager get() = globalCache.fetchCacheManager

    @Before
    fun setUp() {
        urlLoader = TemporaryLocalFileUrlLoader()
        val hyperlinks = IntRange(1, queueSize).map { AppConstants.EXAMPLE_URL + "/$it" }
                .mapIndexed { i, url -> Hyperlink(url, order = 1 + i) }
        urlLoader.saveAll(hyperlinks, group)
    }
}
