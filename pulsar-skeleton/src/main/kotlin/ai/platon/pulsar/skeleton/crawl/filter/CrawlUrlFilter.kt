
package ai.platon.pulsar.skeleton.crawl.filter

/**
 * Interface used to limit which URLs enter AppConstants. Used by the injector and the
 * db updater.
 */
interface CrawlUrlFilter {
    /*
     * Interface for a filter that transforms a URL: it can pass the original URL
     * through or "delete" the URL by returning null
     */
    fun filter(url: String): String?

    fun isValid(urlString: String): Boolean {
        return filter(urlString) != null
    }
}
