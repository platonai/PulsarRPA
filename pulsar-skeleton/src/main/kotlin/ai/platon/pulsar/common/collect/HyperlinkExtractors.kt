package ai.platon.pulsar.common.collect

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.url.CrawlableFatLink
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.StatefulHyperlink
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.bestElement
import ai.platon.pulsar.dom.nodes.node.ext.isAnchor
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.dom.select.collectNotNull
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

open class HyperlinkExtractor(
        val page: WebPage,
        val document: FeaturedDocument,
        val cssSelector: String
) {
    private val log = LoggerFactory.getLogger(HyperlinkExtractor::class.java)

    constructor(session: PulsarSession, page: WebPage, cssSelector: String): this(page, session.parse(page), cssSelector)

    fun extract() = extractTo(LinkedHashSet())

    fun extractTo(fetchUrls: MutableCollection<Hyperlink>): MutableCollection<Hyperlink> {
        val selector = appendSelectorIfMissing(cssSelector, "a")
        var i = 0
        val parsedUrls = document.select(selector).mapNotNull { element ->
            element.attr("abs:href").takeIf { Urls.isValidUrl(it) }?.let { url ->
                StatefulHyperlink(url, element.text(), i++).apply { referer = page.url }
            }
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

    constructor(session: PulsarSession,
                page: WebPage,
                restrictCss: String,
                urlPattern: String
    ): this(page, session.parse(page), restrictCss, urlPattern)

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
                    ?.takeIf { Urls.isValidUrl(it) && it.matches(urlRegex) }
                    ?.let { StatefulHyperlink(it, node.bestElement.text(), i++).apply { referer = page.url } }
        }
        parsedUrls.toCollection(fetchUrls)

        reportHyperlink(page, parsedUrls, fetchUrls, log)

        return fetchUrls
    }
}

class FatLinkExtractor(val session: PulsarSession) {

    private val log = LoggerFactory.getLogger(FatLinkExtractor::class.java)

    companion object {

        data class Counters(
                var unfilteredLinks: Int = 0,
                var regexMatchedLinks: Int = 0,
                var allowLinks: Int = 0,
                var freshLinks: Int = 0,
                var lastFailedLinks: Int = 0,
                var expiredLinks: Int = 0,
                var fetchLinks: Int = 0,
                var failedSeeds: Int = 0,
                var loadedSeeds: Int = 0
        )

        val globalCounters = Counters()

        private val gauges = mapOf(
                "unfilteredLinks" to Gauge { globalCounters.unfilteredLinks },
                "regexMatchedLinks" to Gauge { globalCounters.regexMatchedLinks },
                "freshLinks" to Gauge { globalCounters.freshLinks },
                "lastFailedLinks" to Gauge { globalCounters.lastFailedLinks },
                "expiredLinks" to Gauge { globalCounters.expiredLinks },
                "fetchLinks" to Gauge { globalCounters.fetchLinks },
                "loadedSeeds" to Gauge { globalCounters.loadedSeeds }
        )

        init {
            AppMetrics.registerAll(this, gauges)
        }
    }

    private val webDb = session.context.getBean<WebDb>()
    val counters = Counters()

    fun parse(page: WebPage, document: FeaturedDocument, options: LoadOptions) {
        createFatLink(page, document, options)
    }

    fun createFatLink(seed: NormUrl, denyList: Collection<Hyperlink>): Pair<WebPage, CrawlableFatLink>? {
        // TODO: we can use an event handler to extract links
//        val handler = object: HtmlDocumentHandler() {
//            override val name = CapabilityTypes.FETCH_AFTER_EXTRACT_HANDLER
//            override fun invoke(page: WebPage, document: FeaturedDocument) {
//
//            }
//        }
//        seed.options.volatileConfig?.putBean(handler.name, handler)

        val page = session.load(seed).takeIf { it.protocolStatus.isSuccess }
        if (page == null) {
            ++counters.failedSeeds
            ++globalCounters.failedSeeds
            log.warn("Failed to load seed | {} | {}", page?.protocolStatus, seed)
            return null
        }

        ++counters.loadedSeeds
        ++globalCounters.loadedSeeds

//        val now = Instant.now()
//        val duration = Duration.between(page.prevFetchTime, now)
//        if (duration < seed.options.expires) {
//            // the vivid links are OK
//        }

        val document = page.takeIf { it.hasContent() }?.let { session.parse(it) }
        return createFatLink(seed, page, document, denyList)
    }

    fun createFatLink(page: WebPage, document: FeaturedDocument, options: LoadOptions): Pair<WebPage, CrawlableFatLink>? {
        return createFatLink(NormUrl(page.url, options), page, document)
    }

    fun createFatLink(seed: NormUrl, page: WebPage, document: FeaturedDocument): Pair<WebPage, CrawlableFatLink>? {
        return createFatLink(seed, page, document, listOf())
    }

    fun createFatLink(
            seed: NormUrl, page: WebPage, document: FeaturedDocument? = null, denyList: Collection<Hyperlink>
    ): Pair<WebPage, CrawlableFatLink>? {
        val fatLinkSpec = seed.spec
        val options = seed.options
        val selector = options.outLinkSelector

        val vividLinks = if (document != null) {
            parseVividLinks(seed, page, document, denyList)
        } else {
            loadVividLinks(page, options, denyList)
        }

        counters.fetchLinks = vividLinks.size
        globalCounters.fetchLinks += vividLinks.size

        if (vividLinks.isEmpty()) {
            log.info("{}. No new link in portal page({}), prev fetch time: {} | <{}> | {}",
                    page.id,
                    Strings.readableBytes(page.contentBytes.toLong()),
                    Duration.between(page.prevFetchTime, Instant.now()).readable(),
                    selector,
                    seed)
            log.info("{}. {}", page.id, ObjectConverter.asMap(counters).entries.joinToString())

            if (document != null && counters.unfilteredLinks == 0) {
                val path = session.export(page)
                log.info("{}. No any link in the page, exported to {}", page.id, path)
            }

            return null
        }

        // update vivid links
        if (document != null) {
            val now = Instant.now()
            val hyperlinks = vividLinks.map { HyperlinkPersistable(it.url, it.text, it.order) }
            // page.addHyperlinks is optional, it keeps all historical hyperlinks
            // page.addHyperlinks(hyperlinks)
            page.vividLinks = hyperlinks.associate { it.url to "${it.text} createdAt: $now" }
        }

        return page to CrawlableFatLink(options.label, fatLinkSpec, vividLinks)
    }

    private fun parseVividLinks(
            seed: NormUrl, page: WebPage, document: FeaturedDocument, denyList: Collection<Hyperlink>
    ): List<StatefulHyperlink> {
        val now = Instant.now()
        val fatLinkSpec = seed.spec
        val options = seed.options
        val selector = options.outLinkSelector
        val urlRegex = options.outLinkPattern.toRegex()

        return HyperlinkExtractor(page, document, selector).extract()
                .asSequence()
                .onEach { ++counters.unfilteredLinks; ++globalCounters.unfilteredLinks }
                .filter { it.url.matches(urlRegex) }
                .onEach { ++counters.regexMatchedLinks; ++globalCounters.regexMatchedLinks }
                .filter { it !in denyList }
                .onEach { ++counters.allowLinks; ++globalCounters.allowLinks }
                .filter { shouldFetchItemPage(it.url, options.itemExpires, now) }
                .map { StatefulHyperlink(it.url, it.text, it.order).apply { referer = fatLinkSpec } }
                .toList()
    }

    private fun loadVividLinks(
            page: WebPage, options: LoadOptions, denyList: Collection<Hyperlink>
    ): List<StatefulHyperlink> {
        val now = Instant.now()
        // TODO: add flag to indicate whether the vivid links are expired
        return page.vividLinks.asSequence()
                .map { StatefulHyperlink(it.key.toString(), it.value.toString()) }
                .filterNot { it in denyList }
                .filter { shouldFetchItemPage(it.url, options.itemExpires, now) }
                .toList()
    }

    fun shouldFetchItemPage(url: String, expires: Duration, now: Instant): Boolean {
//        if (text != null) {
//            val createdAt = DateTimes.parseInstant(text.substringAfter(" createdAt: "), Instant.EPOCH)
//            if (Duration.between(createdAt, now).toHours() <= 24) {
//                return true
//            }
//        }

        val p = webDb.getOrNull(url)
        return when {
            p == null -> {
                ++counters.freshLinks
                ++globalCounters.freshLinks
                true
            }
            !p.protocolStatus.isSuccess -> {
                ++counters.lastFailedLinks
                ++globalCounters.lastFailedLinks
                true
            }
            p.prevFetchTime + expires < now -> {
                ++counters.expiredLinks
                ++globalCounters.expiredLinks
                true
            }
            else -> false
        }
    }
}

private fun reportHyperlink(
        page: WebPage,
        links: Collection<Hyperlink>,
        fetchUrls: Collection<Hyperlink>,
        log: Logger
) {
    if (page.contentBytes < 100) {
        log.info("Portal page is illegal (too small) | {}", page.url)
    } else {
        val exportLink = AppPaths.uniqueSymbolicLinkForUri(page.url)
        val readableBytes = Strings.readableBytes(page.contentBytes.toLong())
        log.info("{}. There are {} links in portal page ({}), total {} fetch urls | file://{} | {}",
                page.id, links.size, readableBytes, fetchUrls.size, exportLink, page.url)
    }
}
