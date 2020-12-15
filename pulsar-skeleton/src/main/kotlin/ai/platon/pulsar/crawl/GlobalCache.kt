package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.collect.ConcurrentNEntrantQueue
import ai.platon.pulsar.common.collect.ConcurrentNonReentrantQueue
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache.Companion.CACHE_CAPACITY
import ai.platon.pulsar.common.config.CapabilityTypes.SESSION_DOCUMENT_CACHE_SIZE
import ai.platon.pulsar.common.config.CapabilityTypes.SESSION_PAGE_CACHE_SIZE
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

interface FetchCatch {
    val nonReentrantFetchUrls: Queue<UrlAware>
    val nReentrantFetchUrls: Queue<UrlAware>
    val reentrantFetchUrls: Queue<UrlAware>
    val fetchUrls: Array<Queue<UrlAware>> get() = arrayOf(nonReentrantFetchUrls, nReentrantFetchUrls, reentrantFetchUrls)
    val totalSize get() = fetchUrls.sumOf { it.size }
}

open class ConcurrentFetchCatch(conf: ImmutableConfig): FetchCatch {
    override val nonReentrantFetchUrls = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantFetchUrls = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantFetchUrls = ConcurrentLinkedQueue<UrlAware>()
}

typealias PageCatch = ConcurrentExpiringLRUCache<WebPage>

typealias DocumentCatch = ConcurrentExpiringLRUCache<FeaturedDocument>

/**
 * The global cache
 * */
class GlobalCache(val conf: ImmutableConfig) {
    /**
     * The global page cache, a page might be removed if it's expired or the cache is full
     * */
    val pageCache = PageCatch(conf.getUint(SESSION_PAGE_CACHE_SIZE, CACHE_CAPACITY))
    /**
     * The global document cache, a document might be removed if it's expired or the cache is full
     * */
    val documentCache = DocumentCatch(conf.getUint(SESSION_DOCUMENT_CACHE_SIZE, CACHE_CAPACITY))
    /**
     * The priority fetch caches
     * */
    val fetchCaches = ConcurrentSkipListMap<Int, FetchCatch>()

    init {
        // We add the default fetch catches here, but the customer can still add new ones
        Priority.values().forEach { fetchCaches[it.value] = ConcurrentFetchCatch(conf) }
    }

    val lowestFetchCache: FetchCatch get() = fetchCaches[Priority.LOWEST.value]!!
    val lowerFetchCache: FetchCatch get() = fetchCaches[Priority.LOWER.value]!!
    val normalFetchCache: FetchCatch get() = fetchCaches[Priority.NORMAL.value]!!
    val higherFetchCache: FetchCatch get() = fetchCaches[Priority.HIGHER.value]!!
    val highestFetchCache: FetchCatch get() = fetchCaches[Priority.HIGHEST.value]!!

    val totalFetchItems get() = fetchCaches.values.sumOf { it.totalSize }
}
