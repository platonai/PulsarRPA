
package ai.platon.pulsar.skeleton.crawl.filter

import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory

/**
 * Creates and caches [CrawlUrlFilter] implementing plugins.
 */
class CrawlUrlFilters(
    val urlFilters: List<CrawlUrlFilter>,
    val conf: ImmutableConfig
) {
    val LOG = LoggerFactory.getLogger(CrawlUrlFilters::class.java)

    constructor(conf: ImmutableConfig): this(listOf(), conf)

    /**
     * Run all defined urlFilters. Assume logical AND.
     */
    fun filter(url: String): String? {
        var u: String? = url
        for (urlFilter in urlFilters) {
            if (u != null) {
                u = urlFilter.filter(u)
                if (u == null) {
                    LOG.debug("Url is deleted by {} | {}", urlFilter.javaClass.simpleName, url)
                    break
                }
            }
        }
        return u
    }

    override fun toString(): String {
        return urlFilters.joinToString { it.javaClass.simpleName }
    }
}