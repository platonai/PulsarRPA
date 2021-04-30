package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.collect.ConcurrentNEntrantLoadingQueue
import ai.platon.pulsar.common.collect.FetchCacheCollector
import ai.platon.pulsar.common.collect.LoadingFetchCache
import ai.platon.pulsar.common.urls.Hyperlink
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestLoadingQueues: TestBase() {

    @Test
    fun `When create a LoadingFetchCache then the first page is loaded`() {
        val fetchCache = LoadingFetchCache("", urlLoader)
        // auto loaded
        assertTrue { fetchCache.size > 0 }
        fetchCache.load()
        assertTrue { fetchCache.size > 0 }
    }

    @Test
    fun `When collect from collector with loading fetch cache then sink has items`() {
        val source = LoadingFetchCache("", urlLoader)
        val sink = mutableListOf<Hyperlink>()

        assertTrue { source.size > 0 }
        assertTrue { sink.isEmpty() }

        val collector = FetchCacheCollector(source, source.priority)
        collector.collectTo(sink)

        assertTrue { sink.isNotEmpty() }
    }

    @Test
    fun testConcurrentNEntrantLoadingQueue() {
        val queue = ConcurrentNEntrantLoadingQueue(urlLoader)
        queue.load()

        assertTrue { queue.isNotEmpty() }
        var i = 0
        while (i++ < queueSize) {
            val v = queue.poll()
            assertNotNull(v)
            assertTrue { v is Hyperlink }
        }
        assertTrue { queue.isEmpty() }
    }
}
