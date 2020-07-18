package ai.platon.pulsar.common.message

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.MultiSinkMessageWriter
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.common.WeakPageIndexer
import ai.platon.pulsar.persist.HyperLink
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.ActiveDomUrls
import ai.platon.pulsar.persist.model.DomStatistics
import ai.platon.pulsar.persist.model.LabeledHyperLink
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * Write misc messages into misc sinks
 */
class MiscMessageWriter(val webDb: WebDb, conf: ImmutableConfig) : MultiSinkMessageWriter(conf) {
    private val log = LoggerFactory.getLogger(MiscMessageWriter::class.java)
    private val hostname = NetUtil.getHostname()
    // TODO: job name is set in job setup phrase, so it's not available unless this is a [JobInitialized] class
    private val weakIndexer = WeakPageIndexer(AppConstants.CRAWL_LOG_HOME_URL, webDb)
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME, DateTimes.now("HHmm")]
    private val urlPrefix = AppConstants.CRAWL_LOG_INDEX_URL + "/" + DateTimes.now("yyyy/MM/dd") + "/" + jobIdent + "/" + hostname
    private var reportCount = 0
    // We need predictable iteration order, LinkedHashSet is all right
    private val metricsPageUrls: MutableSet<CharSequence> = LinkedHashSet()
    private val metricsPages: MutableMap<String, WebPage> = HashMap()
    private val closed = AtomicBoolean()

    constructor(conf: ImmutableConfig): this(WebDb(conf), conf)

    fun report(page: WebPage) {
        var category = page.pageCategory.name.toLowerCase()
        if (page.isSeed) {
            category = "seed"
        }
        val fileSuffix = "urls-$category.txt"
        report(fileSuffix, page)
    }

    /**
     * TODO : use WeakIndexer
     */
    fun report(reportGroup: String, page: WebPage) {
        val metricsPageUrl = "$urlPrefix/$reportGroup"
        val metricsPage = getOrCreateMetricsPage(metricsPageUrl)
        metricsPage.addLiveLink(HyperLink(page.url))
        metricsPage.setContent(metricsPage.contentAsString + PageFormatter(page) + "\n")
        metricsPageUrls.add(metricsPageUrl)
        metricsPages[metricsPageUrl] = metricsPage
        if (++reportCount > 40) {
            commit()
            reportCount = 0
        }
    }

    private fun getOrCreateMetricsPage(url: String): WebPage {
        var metricsPage = metricsPages[url]
        if (metricsPage == null) {
            metricsPage = WebPage.newInternalPage(url, "Pulsar Metrics Page")
            metricsPage.setContent("")
            metricsPage.metadata["JobName"] = conf[CapabilityTypes.PARAM_JOB_NAME, "job-unknown-" + DateTimes.now("MMdd.HHmm")]
        }
        return metricsPage
    }

    fun reportPageFromSeedersist(report: String) {
        // String reportString = seedUrl + " -> " + url + "\n";
        write(report, "fetch-urls-from-seed-persist.txt")
    }

    fun reportPageFromSeed(report: String) {
        // String reportString = seedUrl + " -> " + url + "\n";
        write(report, "fetch-urls-from-seed.txt")
    }

    private fun reportFetchTimeHistory(fetchTimeHistory: String) {
        write(fetchTimeHistory, "fetch-time-history.txt")
    }

    fun debugFetchHistory(page: WebPage) {
        // Debug fetch time history
        val fetchTimeHistory = page.getFetchTimeHistory("")
        if (fetchTimeHistory.contains(",")) {
            val report = "CST: ${page.crawlStatus}\tFTH: $fetchTimeHistory | ${page.url}"
            reportFetchTimeHistory(report)
        }
    }

    fun debugDeadOutgoingPage(deadUrl: String, page: WebPage) {
        val report = deadUrl + " <= " + page.url
        write(report, "generate-sort-score.txt")
    }

    fun debugExtractedFields(page: WebPage) {
        if (page.pageModel.isEmpty) {
            return
        }

        val sb = StringBuilder()
        sb.appendln("-----------------")
        sb.appendln(page.url)
        val fields = page.pageModel.firstOrNull()?.fields ?: return
        page.pageModel.fieldGroups.forEach {
            sb.append("Group ").append(it.name)
            it.fields.entries.joinTo(sb, "\n") {
                String.format("%30s: %s", it.key, it.value)
            }
            sb.appendln()
        }

        sb.appendln()

        // show all javascript DOM urls, we should know the exact differences between them
        // they look like all the same
        val urls = page.activeDomUrls
        if (urls != null) {
            sb.append('\n')
            // NOTE: it seems they are all the same
            val location = urls.location
            if (location != page.url)         sb.append("location:    ").appendln(location)
            if (urls.URL != location)         sb.append("URL:         ").appendln(urls.URL)
            if (urls.baseURI != location)     sb.append("baseURI:     ").appendln(urls.baseURI)
            if (urls.documentURI != location) sb.append("documentURI: ").appendln(urls.documentURI)
        }

        sb.append("\n").append("Total ${fields.size} fields")
        sb.append("\n\n")
        write(sb.toString(), "page-extracted-fields.txt")
    }

    fun reportGeneratedHosts(hostNames: Set<String>) {
        val report = StringBuilder("# Total " + hostNames.size + " hosts generated : \n")
        hostNames.asSequence()
                .map { Urls.reverseHost(it) }.sorted().map { Urls.unreverseHost(it) }
                .joinTo(report, "\n") { String.format("%40s", it) }
        write(report.toString(), "generate-hosts.txt")
    }

    fun reportDOMStatistics(page: WebPage, stat: DomStatistics) {
        val report = String.format("%s | a:%-4d i:%-4d mi:%-4d ai:%-4d ia:%-4d | %s",
                page.pageCategory.symbol(),
                stat.anchor, stat.img, stat.mediumImg, stat.anchorImg, stat.imgAnchor,
                page.url)

        write(report, "document-statistics.txt")
    }

    fun reportLabeledHyperLinks(hyperLinks: Set<LabeledHyperLink>) {
        if (hyperLinks.isEmpty()) return
        val groupedHyperLinks = hyperLinks.groupBy { it.label }
        groupedHyperLinks.keys.forEach { label ->
            val links = groupedHyperLinks[label]?:return@forEach

            val report = links.joinToString("\n") {
                String.format("%4d %4d | %-50s | %s", it.depth, it.order, it.anchor, it.url)
            }

            val ident = label.toLowerCase()
            write(report, "labeled-links-$ident.txt")
        }
    }

    fun debugSortScore(page: WebPage) {
        val report = page.sortScore + "\t\t" + page.url
        write(report, "generate-sort-score.txt")
    }

    fun debugFetchLaterSeeds(page: WebPage) {
        val pageReport = PageFormatter(page)
        write(pageReport.toString(), "seeds-fetch-later.txt")
    }

    fun debugDepthUpdated(report: String) {
        write(report, "depth-updated.txt")
    }

    fun debugIllegalLastFetchTime(page: WebPage) {
        val report = String.format("ft: {} lft: {}, fc: {} fh: {} status: {} mk: {}",
                page.fetchTime,
                page.getLastFetchTime(Instant.now()),
                page.fetchCount,
                page.getFetchTimeHistory(""),
                page.protocolStatus,
                page.marks)

        write(report, "illegal-last-fetch-time.txt")
    }

    fun debugRedirects(url: String, urls: ActiveDomUrls) {
        val location = urls.location
        if (location == url && urls.URL == url && urls.baseURI == url && urls.documentURI == url) {
            // no redirect
            return
        }

        val report = StringBuilder(url)

        report.append('\n')
        // NOTE: it seems they are all the same
        if (location != url)              report.append("location:    ").appendln(location)
        if (urls.URL != location)         report.append("URL:         ").appendln(urls.URL)
        if (urls.baseURI != location)     report.append("baseURI:     ").appendln(urls.baseURI)
        if (urls.documentURI != location) report.append("documentURI: ").appendln(urls.documentURI)

        write(report.toString(), "browser-redirects.txt")
    }

    fun reportRedirects(report: String) {
        write(report, "fetch-redirects-txt")
    }

    fun reportFlawParsedPage(page: WebPage, verbose: Boolean) {
        val report = getFlawParsedPageReport(page, verbose)
        write(report, "parse-flaw.txt")
    }

    private fun getFlawParsedPageReport(page: WebPage, verbose: Boolean): String {
        val pageCounters = page.pageCounters
        val parseStatus = page.parseStatus

        val params = Params.of(
                "parseStatus", parseStatus.toString(),
                "parseErr", pageCounters.get(Self.parseErr),
                "extractErr", pageCounters.get(Self.extractErr),
                "U", page.url
        ).withKVDelimiter(":")

        var report = params.formatAsLine()
        if (verbose) {
            report = page.referrer + " -> " + page.url + report
            report += "\n" + page + "\n\n"
        }

        return report
    }

    fun reportBrokenEntity(url: String, message: String) {
        val now = LocalDateTime.now()
        write("$now\t$message\t$url", "broken-entity.txt")
    }

    fun reportPerformance(url: String?, elapsed: String) {
        write("$elapsed | $url", "performance.txt")
    }

    fun debugLongUrls(report: String) {
        write(report, "urls-long.txt")
    }

    fun debugIndexDocTime(timeStrings: String) {
        write(timeStrings, "index-doc-time.txt")
    }

    fun reportFetchSchedule(page: WebPage, verbose: Boolean) {
        var report = PageFormatter(page).toString()
        if (verbose) {
            report = page.referrer + " -> " + page.url + "\n" + report
            report += "\n" + page + "\n\n"
        }

        val prefix = if (verbose) "verbose-" else ""
        val category = page.pageCategory.name.toLowerCase()
        write(report, prefix + "fetch-schedule-$category.txt")
    }

    fun reportForceRefetchSeeds(report: String) {
        write(report, "force-refetch-urls.txt")
    }

    fun reportBadModifiedTime(report: String) {
        write(report, "bad-modified-urls.txt")
    }

    fun commit() {
        // TODO : save only if dirty
        metricsPages.values.forEach { webDb.put(it) }
        webDb.flush()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            commit()
            if (metricsPageUrls.isNotEmpty()) {
                weakIndexer.indexAll(metricsPageUrls)
                weakIndexer.commit()
            }
        }
    }
}
