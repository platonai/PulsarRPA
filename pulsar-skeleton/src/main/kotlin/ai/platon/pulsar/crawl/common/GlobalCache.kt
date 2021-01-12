package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.collect.ConcurrentFetchCatchManager
import ai.platon.pulsar.common.collect.FetchCatchManager
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache.Companion.CACHE_CAPACITY
import ai.platon.pulsar.common.config.CapabilityTypes.SESSION_DOCUMENT_CACHE_SIZE
import ai.platon.pulsar.common.config.CapabilityTypes.SESSION_PAGE_CACHE_SIZE
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.ConcurrentSkipListSet

typealias PageCatch = ConcurrentExpiringLRUCache<WebPage>

typealias DocumentCatch = ConcurrentExpiringLRUCache<FeaturedDocument>

/**
 * The global cache
 * */
open class GlobalCache(val conf: ImmutableConfig) {
    /**
     * The fetch cache manager, hold on queues of fetch items
     * */
    open val fetchCacheManager: FetchCatchManager = ConcurrentFetchCatchManager(conf).apply { initialize() }
    /**
     * The global page cache, a page might be removed if it's expired or the cache is full
     * */
    open val pageCache = PageCatch(conf.getUint(SESSION_PAGE_CACHE_SIZE, CACHE_CAPACITY))
    /**
     * The global document cache, a document might be removed if it's expired or the cache is full
     * */
    open val documentCache = DocumentCatch(conf.getUint(SESSION_DOCUMENT_CACHE_SIZE, CACHE_CAPACITY))

    open val fetchingUrls = ConcurrentSkipListSet<String>()

    fun isFetching(url: String) = fetchingUrls.contains(url)

    fun isFetching(url: UrlAware) = isFetching(url.url)
}
