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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

class FetchCatch(val conf: ImmutableConfig) {
    val nonReentrantFetchUrls = ConcurrentNonReentrantQueue<UrlAware>()
    val nReentrantFetchUrls = ConcurrentNEntrantQueue<UrlAware>(3)
    val reentrantFetchUrls = ConcurrentLinkedQueue<UrlAware>()
    val fetchUrls get() = arrayOf(nonReentrantFetchUrls, nReentrantFetchUrls, reentrantFetchUrls)
}

class GlobalCache(val conf: ImmutableConfig) {
    val pageCache = ConcurrentExpiringLRUCache<WebPage>(conf.getUint(SESSION_PAGE_CACHE_SIZE, CACHE_CAPACITY))
    val documentCache = ConcurrentExpiringLRUCache<FeaturedDocument>(conf.getUint(SESSION_DOCUMENT_CACHE_SIZE, CACHE_CAPACITY))
    val fetchCaches = ConcurrentSkipListMap<Int, FetchCatch>()

    init {
        fetchCaches[Priority.LOWER.value] = FetchCatch(conf)
        fetchCaches[Priority.NORMAL.value] = FetchCatch(conf)
        fetchCaches[Priority.HIGHER.value] = FetchCatch(conf)
    }

    val lowerFetchCache: FetchCatch get() = fetchCaches[Priority.LOWER.value]!!
    val normalFetchCache: FetchCatch get() = fetchCaches[Priority.NORMAL.value]!!
    val higherFetchCache: FetchCatch get() = fetchCaches[Priority.HIGHER.value]!!
}
