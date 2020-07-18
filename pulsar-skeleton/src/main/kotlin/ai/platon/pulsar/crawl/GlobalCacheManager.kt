package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.ConcurrentLRUCache
import ai.platon.pulsar.common.collect.ConcurrentNonReentrantQueue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.time.Duration

class GlobalCacheManager(val immutableConfig: ImmutableConfig) {
    companion object {
        val PAGE_CACHE_TTL = Duration.ofMinutes(5)!!
        const val PAGE_CACHE_CAPACITY = 100

        val DOCUMENT_CACHE_TTL = Duration.ofMinutes(10)!!
        const val DOCUMENT_CACHE_CAPACITY = 100
    }

    val pageCacheCapacity = immutableConfig.getUint("session.page.cache.size", PAGE_CACHE_CAPACITY)
    val documentCacheCapacity = immutableConfig.getUint("session.document.cache.size", DOCUMENT_CACHE_CAPACITY)

    val pageCache = ConcurrentLRUCache<String, WebPage>(PAGE_CACHE_TTL.seconds, pageCacheCapacity)
    val documentCache = ConcurrentLRUCache<String, FeaturedDocument>(DOCUMENT_CACHE_TTL.seconds, documentCacheCapacity)

    val fetchUrls = ConcurrentNonReentrantQueue<String>()
}
