package ai.platon.pulsar.common.collect

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.ObjectConverter
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.message.LoadedPageFormatter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.common.urls.*
import ai.platon.pulsar.common.urls.preprocess.UrlNormalizerPipeline
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.Gauge
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

data class PageFatLink(
    val page: WebPage,
    val fatLink: CrawlableFatLink
)

class FatLinkExtractor(
    val session: PulsarSession,
    val normalizer: UrlNormalizerPipeline = UrlNormalizerPipeline()
) {
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
            var badSeeds: Int = 0,
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
            AppMetrics.reg.registerAll(this, gauges)
        }
    }

    private val webDb = session.context.webDb
    val counters = Counters()

    fun parse(page: WebPage, document: FeaturedDocument, options: LoadOptions) {
        createFatLink(page, document, options)
    }

    fun createFatLink(seed: NormUrl): PageFatLink? = createFatLink(seed, listOf())

    fun createFatLink(seed: NormUrl, page: WebPage, denyList: Collection<UrlAware>): PageFatLink? {
        ++counters.loadedSeeds
        ++globalCounters.loadedSeeds

        if (page.content == null) {
            return null
        }

        val document = session.parse(page)

        return createFatLink(seed, page, document, denyList)
    }

    fun createFatLink(seed: NormUrl, denyList: Collection<UrlAware>): PageFatLink? {
        // TODO: we can use an event handler to extract links
//        val handler = object: HtmlDocumentHandler() {
//            override val name = CapabilityTypes.FETCH_AFTER_EXTRACT_HANDLER
//            override fun invoke(page: WebPage, document: FeaturedDocument) {
//
//            }
//        }
//        seed.options.volatileConfig?.putBean(handler.name, handler)

        val page = session.load(seed)
        if (!page.protocolStatus.isSuccess) {
            ++counters.badSeeds
            ++globalCounters.badSeeds
            LoadedPageFormatter(page, prefix = "Bad seed", withOptions = true).also { log.warn(it.toString()) }
            return null
        }

        return createFatLink(seed, page, denyList)
    }

    fun createFatLink(page: WebPage, document: FeaturedDocument, options: LoadOptions): PageFatLink? {
        return createFatLink(NormUrl(page.url, options), page, document)
    }

    fun createFatLink(seed: NormUrl, page: WebPage, document: FeaturedDocument): PageFatLink? {
        return createFatLink(seed, page, document, listOf())
    }

    /**
     * Create a fat link.
     * If the document is not null, parse links from the document, or if the document is null, try to load the page's
     * vivid link, the vivid link can be parsed and saved recently
     * */
    fun createFatLink(
        seed: NormUrl, page: WebPage, document: FeaturedDocument? = null, denyList: Collection<UrlAware>
    ): PageFatLink? {
        val fatLinkSpec = seed.spec
        val normalizedFatLink = normalizer.invoke(fatLinkSpec) ?: fatLinkSpec
        val options = seed.options
        val args = if (options.label.isNotBlank()) "-label ${options.label}" else ""
        val selector = options.outLinkSelector
        val now = Instant.now()

        val vividLinks = if (document != null) {
            parseVividLinks(seed, page, document, denyList).also { page.fetchedLinkCount = 0 }
        } else {
            loadVividLinks(page, options, denyList)
        }

        counters.fetchLinks = vividLinks.size
        globalCounters.fetchLinks += vividLinks.size

        if (vividLinks.isEmpty()) {
            log.info(
                "{}. No new link in portal page({}), latest fetch at: {} | <{}> | {}",
                page.id,
                Strings.readableBytes(page.contentLength),
                Duration.between(page.prevFetchTime, now).readable(),
                selector,
                seed
            )
            log.info("{}. {}", page.id, ObjectConverter.asMap(counters).entries.joinToString())

            if (document != null && counters.unfilteredLinks == 0) {
                val path = session.export(page)
                log.info("{}. No any link in the page, exported to {}", page.id, path)
            }

            val fatLink = CrawlableFatLink(normalizedFatLink, href = fatLinkSpec)
            return PageFatLink(page, fatLink)
        }

        // update vivid links
        if (document != null) {
            val hyperlinks = vividLinks.map { HyperlinkPersistable(it.url, it.text, it.order) }
            page.vividLinks = hyperlinks.associate { it.url to "${it.text} createdAt: $now" }
        }

        val fatLink = CrawlableFatLink(normalizedFatLink, href = fatLinkSpec, args = args, tailLinks = vividLinks)
        return PageFatLink(page, fatLink)
    }

    private fun parseVividLinks(
        seed: NormUrl, page: WebPage, document: FeaturedDocument, denyList: Collection<UrlAware>
    ): List<StatefulHyperlink> {
        val now = Instant.now()
        val fatLinkSpec = seed.spec
        val options = seed.options
        val selector = options.outLinkSelector
        val urlRegex = options.outLinkPattern.toRegex()

        return HyperlinkExtractor(page, document, selector, normalizer).extract()
            .asSequence()
            .onEach { ++counters.unfilteredLinks; ++globalCounters.unfilteredLinks }
            .filter { it.url.matches(urlRegex) }
            .onEach { ++counters.regexMatchedLinks; ++globalCounters.regexMatchedLinks }
            .filter { it !in denyList }
            .onEach { ++counters.allowLinks; ++globalCounters.allowLinks }
            .mapNotNull { normalizeOrNull(it) }
            .filter { shouldFetchVividPage(it.url, options.itemExpires, now) }
            .map { StatefulHyperlink(it.url, it.text, it.order, referer = fatLinkSpec) }
            .onEach { it.args = "-i 0s" }
            .toList()
    }

    private fun normalizeOrNull(hyperlink: Hyperlink): Hyperlink? {
        val normUrl = normalizer(hyperlink.url)
        return if (normUrl != null) {
            hyperlink.also { it.url = normUrl }
        } else null
    }

    private fun loadVividLinks(
        page: WebPage, options: LoadOptions, denyList: Collection<UrlAware>
    ): List<StatefulHyperlink> {
        val now = Instant.now()
        val urlRegex = options.outLinkPattern.toRegex()
        return page.vividLinks
            .asSequence()
            .mapNotNull { normalizer(it.key.toString())?.let { u -> u to it.value.toString() } }
            .map { StatefulHyperlink(it.first, it.second, 0, referer = page.url) }
            .filterNot { it in denyList }
            .filter { it.url.matches(urlRegex) }
            .filter { shouldFetchVividPage(it.url, options.itemExpires, now) }
            .onEach { it.args = "-i 0s" }
            .toList()
    }

    /**
     * TODO: the logic is different from the one in LoadComponent
     * */
    fun shouldFetchVividPage(url: String, expires: Duration, now: Instant): Boolean {
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
