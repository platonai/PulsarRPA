package ai.platon.pulsar.parse.html

import ai.platon.pulsar.common.MetricsCounters
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.EntityOptions
import ai.platon.pulsar.crawl.parse.ParseFilter
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.collectIf
import ai.platon.pulsar.dom.select.collectIfTo
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.dom.select.firstOrNull
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.PageCategory
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

/**
 * Created by vincent on 16-9-14.
 *
 * Parse Web page using Jsoup if and only if WebPage.query is specified
 *
 * Selector filter, Css selector, XPath selector and Scent selectors are supported
 */
class ExtractFilter(
        val metricsCounters: MetricsCounters,
        val conf: ImmutableConfig
) : ParseFilter {

    companion object {
        enum class Counter { jsoupFailure, noEntity, brokenEntity, brokenSubEntity }
        init { MetricsCounters.register(Counter::class.java) }
    }

    fun filter(page: WebPage, parseResult: ParseResult) {
        filter(ParseContext(page, parseResult))
    }

    /**
     * Extract all fields in the page
     */
    override fun filter(parseContext: ParseContext) {
        val page = parseContext.page
        val parseResult = parseContext.parseResult
        val parser = JsoupParser(page, conf)

        val document = parseContext.document?: FeaturedDocument(parser.parse())
        parseResult.document = document

        parseResult.majorCode = ParseStatus.SUCCESS

        // TODO: use config system
        if (page.url.contains("amazon.com")) {
            extractAmazonIndexLinks(page, document, parseResult)
        }

        var query = page.query
        if (query == null) {
            query = page.options.toString()
        }

        val options = EntityOptions.parse(query)
        if (!options.hasRules()) {
            parseResult.minorCode = ParseStatus.SUCCESS_EXT
            return
        }

        val fieldCollections = parser.extractAll(options)
        if (fieldCollections.isEmpty()) {
            return
        }

        // All last extracted fields are cleared, so we just keep the last extracted fields
        // TODO: How to save updated comments?
        // We only save comments extracted from the current page
        // Comments appears in sub pages can not be read in this WebPage, they may be crawled as separated WebPages
        val pageModel = page.pageModel
        var fieldCollection = fieldCollections[0]
        val majorGroup = pageModel.emplace(1, 0, "selector", fieldCollection)
        var loss = fieldCollection.loss

        page.pageCounters.set(Self.missingFields, loss)
        metricsCounters.increase(Counter.brokenEntity, if (loss > 0) 1 else 0)

        var brokenSubEntity = 0
        for (i in 1 until fieldCollections.size) {
            fieldCollection = fieldCollections[i]
            pageModel.emplace(10000 + i.toLong(), majorGroup.id, "selector-sub", fieldCollection)
            loss = fieldCollection.loss
            if (loss > 0) {
                ++brokenSubEntity
            }
        }

        page.pageCounters.set(Self.brokenSubEntity, brokenSubEntity)
        metricsCounters.increase(Counter.brokenSubEntity, brokenSubEntity)
    }

    private fun extractAmazonIndexLinks(page: WebPage, document: FeaturedDocument, parseResult: ParseResult) {
        document.document.body().forEachElement {
            if (it.isList) {
                if (it.width > 500 && it.height > 1000) {
                    page.pageCategory = PageCategory.INDEX
                    it.collectIf {
                        it is Element && it.tagName() == "a" && it.className().contains("a-link-normal")
                    }.mapTo(parseResult.hypeLinks) { HypeLink(it.attr("abs:href"), it.immutableText, it.sequence) }
                }
            }
        }
    }
}
