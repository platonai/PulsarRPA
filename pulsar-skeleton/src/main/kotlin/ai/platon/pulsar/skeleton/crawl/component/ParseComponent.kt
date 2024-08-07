
package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.crawl.filter.CrawlFilters
import ai.platon.pulsar.skeleton.crawl.parse.PageParser
import ai.platon.pulsar.skeleton.crawl.parse.ParseResult
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * The parse component.
 */
class ParseComponent(
        val crawlFilters: CrawlFilters,
        val pageParser: PageParser,
        val globalCacheFactory: GlobalCacheFactory,
        val conf: ImmutableConfig
) {
    companion object {
        val numParses = AtomicInteger()
        val numParsed = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(ParseComponent::class.java)
    private var traceInfo: ConcurrentHashMap<String, Any>? = null

    constructor(globalCacheFactory: GlobalCacheFactory, conf: ImmutableConfig): this(CrawlFilters(conf), PageParser(conf), globalCacheFactory, conf)

    fun parse(page: WebPage, reparseLinks: Boolean = false, noLinkFilter: Boolean = true): ParseResult {
        beforeParse(page, reparseLinks, noLinkFilter)
        return pageParser.parse(page).also { afterParse(page, it) }
    }

    private fun beforeParse(page: WebPage, reparseLinks: Boolean, noLinkFilter: Boolean) {
        numParses.incrementAndGet()

        if (reparseLinks) {
            page.variables[Name.FORCE_FOLLOW] = AppConstants.YES_STRING
            page.variables[Name.REPARSE_LINKS] = AppConstants.YES_STRING
            page.variables[Name.PARSE_LINK_FILTER_DEBUG_LEVEL] = 1
        }
        if (noLinkFilter) {
            page.variables[Name.PARSE_NO_LINK_FILTER] = AppConstants.YES_STRING
        }
        traceInfo?.clear()
    }

    private fun afterParse(page: WebPage, result: ParseResult) {
        page.variables.remove(Name.REPARSE_LINKS)
        page.variables.remove(Name.FORCE_FOLLOW)
        page.variables.remove(Name.PARSE_LINK_FILTER_DEBUG_LEVEL)
        page.variables.remove(Name.PARSE_NO_LINK_FILTER)

        val document = result.document
        if (document != null) {
            globalCacheFactory.globalCache.documentCache.putDatum(page.url, document)
        }

        numParsed.incrementAndGet()
    }

    fun getTraceInfo(): Map<String, Any> {
        if (traceInfo == null) {
            traceInfo = ConcurrentHashMap()
        }

        traceInfo?.also {
            it.clear()
        }

        return traceInfo?: mapOf()
    }
}
