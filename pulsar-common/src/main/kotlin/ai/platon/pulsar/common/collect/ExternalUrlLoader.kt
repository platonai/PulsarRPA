package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.url.UrlAware

interface ExternalUrlLoader {
    /**
     * The cache size. No more items should be loaded into the memory if the cache is full.
     * */
    var cacheSize: Int
    /**
     * Save the url to the external repository
     * */
    fun save(url: UrlAware)
    /**
     * Save all the url to the external repository
     * */
    fun saveAll(urls: Iterable<UrlAware>)
    /**
     * If there are more items in the source
     * */
    fun hasMore(): Boolean
    /**
     * Load items from the source to the sink
     * */
    fun loadTo(sink: MutableCollection<UrlAware>)
}

abstract class AbstractExternalUrlLoader: ExternalUrlLoader {
    override var cacheSize: Int = Int.MAX_VALUE
    override fun hasMore(): Boolean = true
    abstract override fun save(url: UrlAware)
    abstract override fun saveAll(urls: Iterable<UrlAware>)
    abstract override fun loadTo(sink: MutableCollection<UrlAware>)
}
