package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.collect.AbstractPriorityDataCollector
import ai.platon.pulsar.common.collect.GlobalCachedHyperlinkCollector
import ai.platon.pulsar.common.collect.LoadingFetchCache
import ai.platon.pulsar.common.collect.LocalFileHyperlinkCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.crawl.common.GlobalCache
import org.apache.commons.collections4.map.MultiValueMap
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDataCollectors: TestBase() {

    val globalCache = GlobalCache(conf)
    val fetchCacheManager get() = globalCache.fetchCacheManager

    @Test
    fun testLoadingFetchCache() {
        val fetchCache = LoadingFetchCache(urlLoader, Priority.NORMAL.value, conf)
        // auto loaded
        assertTrue { fetchCache.totalSize > 0 }
        fetchCache.load()
        assertTrue { fetchCache.totalSize > 0 }
    }

    @Test
    fun testFetchQueue() {
        val fetchCache = LoadingFetchCache(urlLoader, Priority.NORMAL.value, conf)
        assertTrue { fetchCache.totalSize > 0 }
        val collector = GlobalCachedHyperlinkCollector(fetchCache, fetchCache.priority)
        val sink = mutableListOf<Hyperlink>()
        collector.collectTo(sink)
        assertTrue { sink.isNotEmpty() }
    }

    @Test
    fun testDataCollectorSorting() {
        // Object information is erased
        val collectors = mutableListOf<AbstractPriorityDataCollector<Hyperlink>>()
        fetchCacheManager.fetchCaches.forEach { (priority, fetchCache) ->
            collectors += GlobalCachedHyperlinkCollector(fetchCache, priority)
        }
        assertEquals(fetchCacheManager.fetchCaches.size, collectors.size)
        assertTrue { collectors.first().priority < collectors.last().priority }
        collectors.sortedBy { it.priority }.forEach { println("$it ${it.priority}") }

        println("Adding another normal collector ...")
        val priority = Priority.NORMAL.value
        val normalCollector = GlobalCachedHyperlinkCollector(fetchCacheManager.normalFetchCache, priority)
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
            collectors[priority] = GlobalCachedHyperlinkCollector(fetchCache, priority)
        }
        assertEquals(fetchCacheManager.fetchCaches.size, collectors.keys.size)
//        assertTrue { collectors.first().priority < collectors.last().priority }
        collectors.keys.sorted().forEach { p -> println("$p ${collectors[p]}") }

        println("Adding 2nd normal collector ...")
        val priority = Priority.NORMAL.value
        val normalCollector = GlobalCachedHyperlinkCollector(fetchCacheManager.normalFetchCache, priority)
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
