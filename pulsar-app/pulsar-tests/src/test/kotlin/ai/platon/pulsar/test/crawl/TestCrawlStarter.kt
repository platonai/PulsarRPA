package ai.platon.pulsar.test.crawl

import ai.platon.pulsar.crawl.StreamingCrawlStarter
import ai.platon.pulsar.test.MockListenableHyperlink
import ai.platon.pulsar.test.TestBase
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

class TestCrawlStarter: TestBase() {

    private val crawlStarter get() = StreamingCrawlStarter(globalCache, session.unmodifiedConfig)
    private val fetchQueue get() = globalCache.fetchCacheManager.normalCache.nReentrantQueue

    @Before
    fun setup() {
        crawlStarter.start()
    }

    @After
    fun tearDown() {
        crawlStarter.stop()
    }

    @Test
    fun `When load a listenable link then the events are trigger`() {
        val url = MockListenableHyperlink("https://www.jd.com")
        fetchQueue.add(url)
        url.await()
        assertTrue(url.isDone())
    }
}
