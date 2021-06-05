package ai.platon.pulsar.test.crawl

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.test.MockListenableHyperlink
import ai.platon.pulsar.test.TestBase
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

class TestCrawlLoop : TestBase() {

    private val fetchQueue get() = globalCache.fetchCaches.normalCache.nReentrantQueue

    @Before
    fun setup() {
        // active CrawlLoop bean
        PulsarContexts.activate().crawlLoop.start()
    }

    @Test
    fun `When load a listenable link then the events are trigger`() {
        val url = MockListenableHyperlink("https://www.jd.com")
        fetchQueue.add(url)
        url.await()
        assertTrue(url.isDone())
    }
}
