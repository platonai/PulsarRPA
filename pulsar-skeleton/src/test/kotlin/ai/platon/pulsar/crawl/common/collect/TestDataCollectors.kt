package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.collect.*
import ai.platon.pulsar.common.collect.collector.AbstractPriorityDataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.UrlCacheCollector
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.common.urls.UrlAware
import org.apache.commons.collections4.map.MultiValueMap
import kotlin.test.*
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDataCollectors : TestBase() {

    private val lowerCacheSize = 10

    @Test
    fun `When add a item to queue then queue is not empty`() {
        val source = LoadingUrlCache("", 0, TemporaryLocalFileUrlLoader())
        val sink = mutableListOf<UrlAware>()

        source.nReentrantQueue.add(PlainUrl(AppConstants.EXAMPLE_URL))
        assertTrue { source.size == 1 }
        val collector = UrlCacheCollector(source)
        assertTrue { collector.hasMore() }
        collector.collectTo(sink)

        assertTrue { sink.isNotEmpty() }
    }

    @Test
    fun `When add an item to LoadingurlCache then LoadingIterable has next`() {
        val urlCache = LoadingUrlCache("", 0, TemporaryLocalFileUrlLoader())
        urlCache.nReentrantQueue.add(PlainUrl(AppConstants.EXAMPLE_URL))
        assertEquals(1, urlCache.size)

        val collectors: MutableList<PriorityDataCollector<UrlAware>> = Collections.synchronizedList(LinkedList())
        collectors += UrlCacheCollector(urlCache)
        val fetchQueueIterable = ConcurrentLoadingIterable(ChainedDataCollector(collectors), null, null, 10)

        assertTrue { fetchQueueIterable.regularCollector.hasMore() }
        assertTrue { fetchQueueIterable.iterator().hasNext() }
        assertEquals(AppConstants.EXAMPLE_URL, fetchQueueIterable.iterator().next().url)
    }

    @Test
    fun testDataCollectorSorting() {
        // Object information is erased
        val collectors = mutableListOf<AbstractPriorityDataCollector<UrlAware>>()
        urlPool.orderedCaches.forEach { (priority, urlCache) ->
            collectors += UrlCacheCollector(urlCache)
        }
        assertEquals(urlPool.orderedCaches.size, collectors.size)
        assertTrue { collectors.first().priority < collectors.last().priority }
//        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }

        println("Adding another normal collector ...")
        val priority = Priority13.NORMAL.value
        val normalCollector = UrlCacheCollector(urlPool.normalCache)
        collectors += normalCollector
        assertEquals(2, collectors.count { it.priority == priority })
//        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }

        val normalCollector2 = LocalFileHyperlinkCollector(Paths.get("/tmp/non-exist"), priority)
        collectors += normalCollector2
        assertEquals(3, collectors.count { it.priority == priority })
//        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }
    }

    @Test
    fun testDataCollectorSorting2() {
        val collectors = MultiValueMap<Int, AbstractPriorityDataCollector<UrlAware>>()

        globalCache.urlPool.orderedCaches.forEach { (priority, urlCache) ->
            collectors[priority] = UrlCacheCollector(urlCache)
        }
        assertEquals(urlPool.orderedCaches.size, collectors.keys.size)
//        assertTrue { collectors.first().priority < collectors.last().priority }
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }

        println("Adding 2nd normal collector ...")
        val priority = Priority13.NORMAL.value
        val normalCollector = UrlCacheCollector(urlPool.normalCache)
        collectors[priority] = normalCollector
        assertEquals(2, collectors.size(priority))
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }

        println("Adding 3rd normal collector ...")
        val normalCollector2 = LocalFileHyperlinkCollector(Paths.get("/tmp/non-exist"), priority)
        collectors[priority] = normalCollector2
        assertEquals(3, collectors.size(priority))
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }
    }

    @Test
    fun `When iterate through fetch iterable then items are correct`() {
        val fetchIterable = UrlFeeder(urlPool, lowerCacheSize)
        val resourceURI = ResourceLoader.getResource("seeds/head100/best-sellers.txt")!!.toURI()
        val collector = LocalFileHyperlinkCollector(Paths.get(resourceURI), Priority13.NORMAL)
        fetchIterable.addDefaultCollectors()
        fetchIterable.addCollector(collector)

        val cache = fetchIterable.urlPool.lower2Cache
        LinkExtractors.fromResource("seeds/head100/most-wished-for.txt")
            .mapTo(cache.nonReentrantQueue) { Hyperlink(it) }

        var i = 0
        fetchIterable.forEach {
            if (i < 100) {
                assertTrue("The $i-th urls should contains zgbs but is ${it.url}") { it.url.contains("zgbs") }
            } else {
                assertTrue("The $i-th urls should contains most-wished-for but is ${it.url}") { it.url.contains("most-wished-for") }
            }
            ++i
        }

        assertTrue { collector.hyperlinks.isEmpty() }
        assertTrue { cache.nonReentrantQueue.isEmpty() }
    }
}
