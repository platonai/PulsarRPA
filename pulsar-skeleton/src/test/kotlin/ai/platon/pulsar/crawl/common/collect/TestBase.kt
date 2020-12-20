package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.collect.TemporaryLocalFileUrlLoader
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.Hyperlink
import org.junit.Before
import org.junit.BeforeClass

open class TestBase {
    protected val conf = ImmutableConfig()
    protected val queueSize = 100
    protected lateinit var urlLoader: TemporaryLocalFileUrlLoader

    @Before
    fun setUp() {
        urlLoader = TemporaryLocalFileUrlLoader()
        val hyperlinks = IntRange(1, queueSize).map { AppConstants.EXAMPLE_URL + "/$it" }
                .mapIndexed { i, url -> Hyperlink(url, order = 1 + i) }
        urlLoader.saveAll(hyperlinks)
    }
}
