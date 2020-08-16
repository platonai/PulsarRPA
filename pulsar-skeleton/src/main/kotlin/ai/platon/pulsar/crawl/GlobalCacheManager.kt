package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.ConcurrentNEntrantQueue
import ai.platon.pulsar.common.collect.ConcurrentNonReentrantQueue
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache.Companion.CACHE_CAPACITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.ConcurrentLinkedQueue

class GlobalCacheManager(val conf: ImmutableConfig) {
    val SESSION_PAGE_CACHE_SIZE = "session.page.cache.size"
    val SESSION_DOCUMENT_CACHE_SIZE = "session.document.cache.size"

    val pageCache = ConcurrentExpiringLRUCache<WebPage>(conf.getUint(SESSION_PAGE_CACHE_SIZE, CACHE_CAPACITY))
    val documentCache = ConcurrentExpiringLRUCache<FeaturedDocument>(conf.getUint(SESSION_DOCUMENT_CACHE_SIZE, CACHE_CAPACITY))

    val nonReentrantFetchUrls = ConcurrentNonReentrantQueue<String>()
    val nReentrantFetchUrls = ConcurrentNEntrantQueue<String>(3)
    val reentrantFetchUrls = ConcurrentLinkedQueue<String>()
    val fetchUrls get() = arrayOf(nonReentrantFetchUrls, nReentrantFetchUrls, reentrantFetchUrls)
}
