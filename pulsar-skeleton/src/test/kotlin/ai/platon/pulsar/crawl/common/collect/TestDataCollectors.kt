package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.collect.*
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.PlainUrl
import org.apache.commons.collections4.map.MultiValueMap
import org.junit.Test
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDataCollectors: TestBase() {

    @Test
    fun testLoadingFetchCache() {
        val fetchCache = LoadingFetchCache(urlLoader, conf = conf)
        // auto loaded
        assertTrue { fetchCache.totalSize > 0 }
        fetchCache.load()
        assertTrue { fetchCache.totalSize > 0 }
    }

    @Test
    fun `when collect from collector with loading fetch cache then sink has items`() {
        val fetchCache = LoadingFetchCache(urlLoader, conf = conf)
        assertTrue { fetchCache.totalSize > 0 }
        val collector = FetchCacheCollector(fetchCache, fetchCache.priority)
        val sink = mutableListOf<Hyperlink>()
        collector.collectTo(sink)
        assertTrue { sink.isNotEmpty() }
    }

    @Test
    fun `when add a item to queue then queue is not empty`() {
        val fetchCache = LoadingFetchCache(TemporaryLocalFileUrlLoader(), conf = conf)
        fetchCache.nReentrantQueue.add(PlainUrl(AppConstants.EXAMPLE_URL))
        assertTrue { fetchCache.totalSize == 1 }
        val collector = FetchCacheCollector(fetchCache, fetchCache.priority)
        assertTrue { collector.hasMore() }
        val sink = mutableListOf<Hyperlink>()
        collector.collectTo(sink)
        assertTrue { sink.isNotEmpty() }
    }

    @Test
    fun `when add an item to LoadingFetchCache then LoadingIterable has next`() {
        val fetchCache = LoadingFetchCache(TemporaryLocalFileUrlLoader(), conf = conf)
        fetchCache.nReentrantQueue.add(PlainUrl(AppConstants.EXAMPLE_URL))
        assertTrue { fetchCache.totalSize == 1 }

        val collectors: MutableList<PriorityDataCollector<Hyperlink>> = Collections.synchronizedList(LinkedList())
        collectors += FetchCacheCollector(fetchCache, fetchCache.priority)
        val fetchQueueIterable = ConcurrentLoadingIterable(MultiSourceDataCollector(collectors), null, 10)

        assertTrue { fetchQueueIterable.collector.hasMore() }
        assertTrue { fetchQueueIterable.iterator().hasNext() }
        assertEquals(AppConstants.EXAMPLE_URL, fetchQueueIterable.iterator().next().url)
    }

    @Test
    fun testDataCollectorSorting() {
        // Object information is erased
        val collectors = mutableListOf<AbstractPriorityDataCollector<Hyperlink>>()
        fetchCacheManager.fetchCaches.forEach { (priority, fetchCache) ->
            collectors += FetchCacheCollector(fetchCache, priority)
        }
        assertEquals(fetchCacheManager.fetchCaches.size, collectors.size)
        assertTrue { collectors.first().priority < collectors.last().priority }
        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }

        println("Adding another normal collector ...")
        val priority = Priority.NORMAL.value
        val normalCollector = FetchCacheCollector(fetchCacheManager.normalFetchCache, priority)
        collectors += normalCollector
        assertEquals(2, collectors.count { it.priority == priority })
        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }

        val normalCollector2 = LocalFileHyperlinkCollector(Paths.get("/tmp/non-exist"), priority)
        collectors += normalCollector2
        assertEquals(3, collectors.count { it.priority == priority })
        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }
    }

    @Test
    fun testDataCollectorSorting2() {
        val collectors = MultiValueMap<Int, AbstractPriorityDataCollector<Hyperlink>>()

        globalCache.fetchCacheManager.fetchCaches.forEach { (priority, fetchCache) ->
            collectors[priority] = FetchCacheCollector(fetchCache, priority)
        }
        assertEquals(fetchCacheManager.fetchCaches.size, collectors.keys.size)
//        assertTrue { collectors.first().priority < collectors.last().priority }
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }

        println("Adding 2nd normal collector ...")
        val priority = Priority.NORMAL.value
        val normalCollector = FetchCacheCollector(fetchCacheManager.normalFetchCache, priority)
        collectors[priority] = normalCollector
        assertEquals(2, collectors.size(priority))
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }

        println("Adding 3rd normal collector ...")
        val normalCollector2 = LocalFileHyperlinkCollector(Paths.get("/tmp/non-exist"), priority)
        collectors[priority] = normalCollector2
        assertEquals(3, collectors.size(priority))
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }
    }
}
