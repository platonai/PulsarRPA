package ai.platon.pulsar.parse.html.sites

import ai.platon.pulsar.common.MetricsCounters
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.ParseFilter
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.html.JsoupParser
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.ParseStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.persist.data.DomStatistics
import ai.platon.pulsar.persist.data.LabeledHyperLink
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory

/**
 * Created by vincent on 16-9-14.
 *
 * Parse Web page using Jsoup if and only if WebPage.query is specified
 *
 * Selector filter, Css selector, XPath selector and Scent selectors are supported
 */
class AmazonExtractor(
        val metricsCounters: MetricsCounters,
        val conf: ImmutableConfig
) : ParseFilter {

    companion object {
        val indexLinkPattens = arrayOf("&node=\\d+").map { it.toRegex() }
        val detailLinkPattens = arrayOf("/gp/slredirect/").map { it.toRegex() }
        val reviewLinkPieces = arrayOf("customer-reviews", "product-reviews", "/review/", "create-review")
        val profileLinkPieces = arrayOf("/profile/")
    }

    private var log = LoggerFactory.getLogger(AmazonExtractor::class.java)
    private val domain = "amazon.com"

    /**
     * Extract all fields in the page
     */
    override fun filter(parseContext: ParseContext) {
        val page = parseContext.page
        if (!page.url.contains(domain)) {
            return
        }

        val document = parseContext.document?: JsoupParser(page, conf).parse()
        parseContext.document = document
        val parseResult = parseContext.parseResult

        processAmazon(page, document, parseResult)

        parseResult.majorCode = ParseStatus.SUCCESS
    }

    private fun processAmazon(page: WebPage, document: FeaturedDocument, parseResult: ParseResult) {
        val url = page.url

        // TODO: can be collected in js side
        val stat = DomStatistics()

        document.document.body().forEachElement { e ->
            if (e.isImage) {
                ++stat.img
                if (e.parent().width in 150..350 && e.parent().height in 150..350) {
                    ++stat.mediumImg
                }
            } else if (e.isAnchor) {
                ++stat.anchor
            }

            if (e.isAnchorImage) {
                ++stat.anchorImg
            } else if (e.isImageAnchor) {
                ++stat.imgAnchor
            }

            if (e.id() == "leftNav") {
                collectSearchNavigateLinks(e)
                return@forEachElement
            }
            // filter out searching area
            if (e.right < 200) {
                return@forEachElement
            }
            if (!e.isBlock && e.tagName() !in arrayOf("a", "img")) {
                return@forEachElement
            }

            // detect breadcrumb area
            if (e.hasClass("a-breadcrumb")) {
                collectCategoryNavigateLinks(e)
            }

            // detect detail page
            if (!page.pageCategory.isDetail) {
                checkPageCategoryIsDetail(page, e)
            }

            if (e.isList) {
                if (e.width >= 500 && e.height > 1000) {
                    page.pageCategory = PageCategory.INDEX
                }
            }
        }

        if (detailLinkPattens.any { url.contains(it) }) {
            page.pageCategory = PageCategory.DETAIL
        } else if (indexLinkPattens.any { url.contains(it) }) {
            page.pageCategory = PageCategory.INDEX
        } else if (reviewLinkPieces.any { url.contains(it) }) {
            page.pageCategory = PageCategory.REVIEW
        } else if (profileLinkPieces.any { url.contains(it) }) {
            page.pageCategory = PageCategory.PROFILE
        } else if (!page.pageCategory.isDetail && stat.mediumImg > 50) {
            page.pageCategory = PageCategory.INDEX
        }

        if (page.pageCategory.isDetail) {
            extractAmazonItemPage(page, document, parseResult)
        }

        if (page.pageCategory.isIndex || page.pageCategory.isDetail) {
            var order = 0
            document.document.forEachElement { e ->
                if (e.isList && e.width >= 500 && e.childNodeSize() >= 8) {
                    e.forEachElement {
                        collectHyperLinks(e, ++order, parseResult)
                    }
                }
            }
        }

        parseResult.domStatistics = stat
    }

    private fun collectHyperLinks(ele: Element, order: Int, parseResult: ParseResult) {
        if (ele.isAnchor && ele.hasClass("a-link-normal")) {
            val href = ele.attr("abs:href")
            // println("${order + 1}.\t$href")
            if (href.contains(domain) && !href.contains("/vdp/")) {
                val hypeLink = HypeLink(href, ele.text(), order)
                parseResult.hypeLinks.add(hypeLink)
            }
        }
    }

    private fun collectSearchNavigateLinks(root: Element) {
        var order = 0
        root.forEachElement {
            if (it.isAnchor) {
                val href = it.attr("abs:href")
                if (href.contains(domain)) {
                    val anchor = it.text()
                    val depth = it.depth - root.depth
                    val labeledLink = LabeledHyperLink("SER", depth, ++order, anchor, href)
                    ParseResult.labeledHypeLinks.add(labeledLink)
                }
            }
        }
    }

    private fun collectCategoryNavigateLinks(root: Element) {
        var order = 0
        root.forEachElement {
            if (it.isAnchor) {
                val href = it.attr("abs:href")
                if (href.contains(domain)) {
                    val anchor = it.text()
                    val depth = it.depth - root.depth

                    val labeledLink = LabeledHyperLink("CAT", depth, ++order, anchor, href)
                    ParseResult.labeledHypeLinks.add(labeledLink)
                }
            }
        }
    }

    private fun extractAmazonItemPage(page: WebPage, document: FeaturedDocument, parseResult: ParseResult) {
        val body = document.body
        val fields = HashMap<CharSequence, CharSequence>()

        val textExtractors = mapOf(
                "breadcrumbs" to "#wayfinding-breadcrumbs_container ul",
                "title" to "#productTitle",
                "bylineInfo" to "#bylineInfo",
                "averageCustomerReviews" to "#averageCustomerReviews_feature_div",
                "averageCustomerReviews" to "#averageCustomerReviews",
                "acrCustomerReviewText" to "#acrCustomerReviewText",
                "askATF" to "#askATFLink",
                "acBadge" to "#acBadge_feature_div .ac-for-text",
                "price" to "#priceblock_ourprice",
                "color" to "#variation_color_name",
                "style" to "#variation_style_name",
                "features" to "#feature-bullets",
                "compareLink" to "#HLCXComparisonJumplink_feature_div",
                "olp-upd-new-used" to "#olp-upd-new-used"
        )

        textExtractors.entries.map { it.key to body.first(it.value) { it.text() } }
                .filterNot { it.second.isNullOrBlank() }
                .associateTo(fields) { it.first to it.second!! }
        page.pageModel.emplace(0, 0, "item", fields)
    }

    private fun checkPageCategoryIsDetail(page: WebPage, e: Element) {
        if (e.isImage && e.hasClass("a-dynamic-image")) {
            page.pageCategory = PageCategory.DETAIL
        } else if (e.id() == "imgTagWrapperId" && e.height > 500) {
            // the big image wrapper: 934 * 700
            page.pageCategory = PageCategory.DETAIL
        } else if (e.id() == "add-to-cart-button" && e.width > 150) {
            // have a big add-to-cart button
            page.pageCategory = PageCategory.DETAIL
        } else if (e.id() == "title" && e.top < 500 && e.width >= 500 && e.height >= 80) {
            page.pageCategory = PageCategory.DETAIL
        }
    }
}
