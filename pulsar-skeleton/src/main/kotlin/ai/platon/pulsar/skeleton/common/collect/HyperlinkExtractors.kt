package ai.platon.pulsar.skeleton.common.collect

import ai.platon.pulsar.skeleton.session.PulsarSession
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.StatefulHyperlink
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.preprocess.UrlNormalizer
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.bestElement
import ai.platon.pulsar.dom.nodes.node.ext.isAnchor
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.dom.select.collectNotNull
import ai.platon.pulsar.persist.WebPage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class HyperlinkExtractor(
    val page: WebPage,
    val document: FeaturedDocument,
    val selector: String,
    val normalizer: UrlNormalizer? = null
) {
    private val log = LoggerFactory.getLogger(HyperlinkExtractor::class.java)

    fun extract() = extractTo(LinkedHashSet())

    fun extractTo(fetchUrls: MutableCollection<Hyperlink>): MutableCollection<Hyperlink> {
        val selector0 = appendSelectorIfMissing(selector, "a")

        var i = 0
        val parsedUrls = document.select(selector0).mapNotNull { element ->
            element.attr("abs:href").takeIf { UrlUtils.isStandard(it) }
                ?.let { Pair(it, normalizer?.invoke(it) ?: it) }
                ?.let { Hyperlink(it.second, element.text(), i++, referrer = page.url, href = it.first) }
        }
        parsedUrls.toCollection(fetchUrls)

        reportHyperlink(page, parsedUrls, fetchUrls, log)

        return fetchUrls
    }
}

open class RegexHyperlinkExtractor(
    val page: WebPage,
    val document: FeaturedDocument,
    val restrictCss: String,
    val urlPattern: String
) {
    private val log = LoggerFactory.getLogger(RegexHyperlinkExtractor::class.java)

    private val urlRegex = urlPattern.toRegex()

    constructor(
        session: PulsarSession,
        page: WebPage,
        restrictCss: String,
        urlPattern: String
    ) : this(page, session.parse(page), restrictCss, urlPattern)

    fun extract(): MutableCollection<Hyperlink> {
        return extractTo(LinkedHashSet())
    }

    fun extractTo(fetchUrls: MutableCollection<Hyperlink>): MutableCollection<Hyperlink> {
        val restrictedSection = document.document.selectFirst(restrictCss)

        if (restrictedSection == null) {
            log.warn("There is no restricted section <{}> | {}", restrictCss, page.url)
            return fetchUrls
        }

        var i = 0
        val parsedUrls = restrictedSection.collectNotNull { node ->
            node.takeIf { it.isAnchor }?.attr("abs:href")
                ?.takeIf { UrlUtils.isStandard(it) && it.matches(urlRegex) }
                ?.let { StatefulHyperlink(it, node.bestElement.text(), i++, referrer = page.url) }
        }
        parsedUrls.toCollection(fetchUrls)

        reportHyperlink(page, parsedUrls, fetchUrls, log)

        return fetchUrls
    }
}

private fun reportHyperlink(
    page: WebPage,
    links: Collection<Hyperlink>,
    fetchUrls: Collection<Hyperlink>,
    log: Logger
) {
    if (page.contentLength < 100) {
        log.info("Portal page is illegal (too small) | {}", page.url)
    } else {
        val exportLink = AppPaths.uniqueSymbolicLinkForUri(page.url)
        val readableBytes = Strings.compactFormat(page.contentLength)
        log.info(
            "{}. There are {} links in portal page ({}), total {} fetch urls | file://{} | {}",
            page.id, links.size, readableBytes, fetchUrls.size, exportLink, page.url
        )
    }
}
