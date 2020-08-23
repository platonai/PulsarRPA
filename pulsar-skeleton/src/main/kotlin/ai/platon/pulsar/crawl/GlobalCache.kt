package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.ConcurrentNEntrantQueue
import ai.platon.pulsar.common.collect.ConcurrentNonReentrantQueue
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache.Companion.CACHE_CAPACITY
import ai.platon.pulsar.common.config.CapabilityTypes.SESSION_DOCUMENT_CACHE_SIZE
import ai.platon.pulsar.common.config.CapabilityTypes.SESSION_PAGE_CACHE_SIZE
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.ConcurrentLinkedQueue

class GlobalCache(val conf: ImmutableConfig) {

    val pageCache = ConcurrentExpiringLRUCache<WebPage>(conf.getUint(SESSION_PAGE_CACHE_SIZE, CACHE_CAPACITY))
    val documentCache = ConcurrentExpiringLRUCache<FeaturedDocument>(conf.getUint(SESSION_DOCUMENT_CACHE_SIZE, CACHE_CAPACITY))

    val nonReentrantFetchUrls = ConcurrentNonReentrantQueue<String>()
    val limitedReentrantFetchUrls = ConcurrentNEntrantQueue<String>(3)
    val reentrantFetchUrls = ConcurrentLinkedQueue<String>()
    val fetchUrls get() = arrayOf(nonReentrantFetchUrls, limitedReentrantFetchUrls, reentrantFetchUrls)
}
