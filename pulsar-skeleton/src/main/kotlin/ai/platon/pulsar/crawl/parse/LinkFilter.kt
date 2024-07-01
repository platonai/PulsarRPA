package ai.platon.pulsar.crawl.parse

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.LinkOptions
import ai.platon.pulsar.common.options.LinkOptions.Companion.parse
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.common.URLUtil.GroupMode
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Predicate

/**
 * Created by vincent on 17-5-21.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class LinkFilter(private val crawlFilters: CrawlFilters, val conf: ImmutableConfig) : Parameterized {
    private val groupMode = conf.getEnum(CapabilityTypes.FETCH_QUEUE_MODE, GroupMode.BY_HOST)
    private val ignoreExternalLinks = conf.getBoolean(CapabilityTypes.PARSE_IGNORE_EXTERNAL_LINKS, false)
    private val maxUrlLength = conf.getInt(CapabilityTypes.PARSE_MAX_URL_LENGTH, 1024)
    private var sourceHost: String? = null
    private var linkOptions: LinkOptions? = null
    private var reparseLinks = false
    private var noFilter = false
    private var debugLevel = 0
    private val links: MutableSet<String> = TreeSet()
    private val mutableFilterReport: MutableList<String> = mutableListOf()

    val filterReport: List<String> get() = mutableFilterReport

    override fun getParams(): Params {
        return Params.of(
                "groupMode", groupMode,
                "ignoreExternalLinks", ignoreExternalLinks,
                "maxUrlLength", maxUrlLength,
                "defaultAnchorLenMin", conf[CapabilityTypes.PARSE_MIN_ANCHOR_LENGTH],
                "defaultAnchorLenMax", conf[CapabilityTypes.PARSE_MAX_ANCHOR_LENGTH]
        )
    }

    fun reset(page: WebPage) {
        // TODO: LinkOptions.parse() should be very fast and highly optimized, JCommand is not a good way
        linkOptions = parse(page.args.toString(), conf)
        sourceHost = if (ignoreExternalLinks) URLUtil.getHost(page.url, groupMode) else ""
        reparseLinks = page.variables.contains(Name.REPARSE_LINKS)
        noFilter = page.variables.contains(Name.PARSE_NO_LINK_FILTER)
        debugLevel = page.variables.get(Name.PARSE_LINK_FILTER_DEBUG_LEVEL, 0)
        links.clear()
        page.links.forEach { l: CharSequence -> links.add(l.toString()) }
        mutableFilterReport.clear()
    }

    fun asPredicate(page: WebPage): Predicate<HyperlinkPersistable> {
        reset(page)
        return Predicate { l: HyperlinkPersistable ->
            val r = this.filter(l)
            if (debugLevel > 0) {
                mutableFilterReport.add(r.toString() + " <- " + l.url + "\t" + l.text)
            }
            0 == r
        }
    }

    fun filter(link: HyperlinkPersistable): Int {
        if (noFilter) {
            return 0
        }
        var url = link.url
        if (link.url.isEmpty()) {
            return 110
        }
        if (link.url.length > maxUrlLength) {
            return 112
        }
        val destHost = URLUtil.getHost(url, groupMode)
        if (destHost.isNullOrEmpty()) {
            return 104
        }
        if (ignoreExternalLinks && sourceHost != destHost) {
            return 106
        }
        val r = linkOptions!!.filter(link.url, link.text)
        if (r > 0) {
            return 2000 + r
        }
        if (!reparseLinks && links.contains(link.url)) {
            return 118
        }
        url = crawlFilters.normalizeToEmpty(link.url)
        if (url.isEmpty()) {
            return 1000
        }
        return if (!reparseLinks && links.contains(url)) {
            120
        } else 0
    }

    companion object {
        val LOG = LoggerFactory.getLogger(LinkFilter::class.java)
    }
}
