package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.collect.ConcurrentFetchCatchManager
import ai.platon.pulsar.common.collect.FetchCatchManager
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache.Companion.CACHE_CAPACITY
import ai.platon.pulsar.common.config.CapabilityTypes.GLOBAL_DOCUMENT_CACHE_SIZE
import ai.platon.pulsar.common.config.CapabilityTypes.GLOBAL_PAGE_CACHE_SIZE
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.ConcurrentSkipListSet

/**
 * TODO: WebPage is very large, we need a loading cache
 * */
typealias PageCatch = ConcurrentExpiringLRUCache<WebPage>

/**
 * TODO: FeaturedDocument is very large, we need a loading cache
 * */
typealias DocumentCatch = ConcurrentExpiringLRUCache<FeaturedDocument>

class FetchingCache {

    private val fetchingUrls = ConcurrentSkipListSet<String>()

    fun isFetching(url: String) = fetchingUrls.contains(url)

    fun add(url: String) {
        fetchingUrls.add(url)
    }

    fun addAll(urls: Iterable<String>) {
        fetchingUrls.addAll(urls)
    }

    fun remove(url: String) {
        fetchingUrls.remove(url)
    }

    fun removeAll(urls: Iterable<String>) {
        fetchingUrls.removeAll(urls)
    }

    operator fun contains(url: String) = fetchingUrls.contains(url)
}

/**
 * The global cache
 * */
open class GlobalCache(val conf: ImmutableConfig) {
    /**
     * The page cache capacity
     * */
    private val pageCacheCapacity = conf.getUint(GLOBAL_PAGE_CACHE_SIZE, CACHE_CAPACITY)
    /**
     * The document cache capacity
     * */
    private val documentCacheCapacity = conf.getUint(GLOBAL_DOCUMENT_CACHE_SIZE, CACHE_CAPACITY)
    /**
     * The fetch cache manager, hold on queues of fetch items
     * */
    open val fetchCaches: FetchCatchManager = ConcurrentFetchCatchManager(conf).apply { initialize() }
    /**
     * The global page cache, a page might be removed if it's expired or the cache is full
     * */
    open val pageCache = PageCatch(pageCacheCapacity)
    /**
     * The global document cache, a document might be removed if it's expired or the cache is full
     * */
    open val documentCache = DocumentCatch(documentCacheCapacity)

    open val fetchingCache = FetchingCache()

    /**
     * Put page and document to cache
     * */
    fun putPDCache(page: WebPage, document: FeaturedDocument) {
        val url = page.url
        pageCache.putDatum(url, page)
        documentCache.putDatum(url, document)
    }

    /**
     * Remove item from page cache and document cache
     * */
    fun removePDCache(url: String) {
        pageCache.remove(url)
        documentCache.remove(url)
    }
}
