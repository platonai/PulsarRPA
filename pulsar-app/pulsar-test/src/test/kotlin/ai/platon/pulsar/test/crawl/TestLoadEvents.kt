package ai.platon.pulsar.test.crawl

import ai.platon.pulsar.test.MockListenableHyperlink
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.test.TestBase
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

class TestLoadEvents: TestBase() {

    private val crawlLoop get() = StreamingCrawlLoop(session, globalCache)
    private val fetchQueue get() = globalCache.fetchCacheManager.normalCache.nReentrantQueue

    @Before
    fun setup() {
        crawlLoop.start()
    }

    @After
    fun tearDown() {
        crawlLoop.stop()
    }

    @Test
    fun `When load a listenable link then the events are trigger`() {
        val url = MockListenableHyperlink("https://www.jd.com")
        fetchQueue.add(url)
        url.await()
        assertTrue(url.isDone())
    }
}
