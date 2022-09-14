package ai.platon.pulsar.common.message

import ai.platon.pulsar.common.MultiSinkWriter
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.ActiveDOMUrls
import ai.platon.pulsar.persist.model.DomStatistics
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * Write misc messages into misc sinks
 */
class MiscMessageWriter(
    conf: ImmutableConfig
) : MultiSinkWriter(conf) {
    private val closed = AtomicBoolean()

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

    fun reportGeneratedHosts(hostNames: Set<String>) {
        val report = StringBuilder("# Total " + hostNames.size + " hosts generated : \n")
        hostNames.asSequence()
            .map { UrlUtils.reverseHost(it) }.sorted().map { UrlUtils.unreverseHost(it) }
            .joinTo(report, "\n") { String.format("%40s", it) }
        write(report.toString(), "generate-hosts.txt")
    }

    fun reportDOMStatistics(page: WebPage, stat: DomStatistics) {
        val report = String.format(
            "%s | a:%-4d i:%-4d mi:%-4d ai:%-4d ia:%-4d | %s",
            page.pageCategory.symbol(),
            stat.anchor, stat.img, stat.mediumImg, stat.anchorImg, stat.imgAnchor,
            page.url
        )

        write(report, "document-statistics.txt")
    }

    fun reportLabeledHyperlinks(hyperLinks: Set<Hyperlink>) {
        if (hyperLinks.isEmpty()) return
        val groupedHyperlinks = hyperLinks.groupBy { it.label }
        groupedHyperlinks.keys.forEach { label ->
            val links = groupedHyperlinks[label] ?: return@forEach

            val report = links.joinToString("\n") {
                String.format("%4d %4d | %-50s | %s", it.depth, it.order, it.text, it.url)
            }

            val ident = label.lowercase(Locale.getDefault())
            write(report, "labeled-links-$ident.txt")
        }
    }

    fun debugSortScore(page: WebPage) {
        val report = page.sortScore + "\t\t" + page.url
        write(report, "generate-sort-score.txt")
    }

    fun debugFetchLaterSeeds(page: WebPage) {
        val pageReport = FetchStatusFormatter(page)
        write(pageReport.toString(), "seeds-fetch-later.txt")
    }

    fun debugDepthUpdated(report: String) {
        write(report, "depth-updated.txt")
    }

    fun debugIllegalLastFetchTime(page: WebPage) {
        val report = String.format(
            "ft: {} lft: {}, fc: {} fh: {} status: {} mk: {}",
            page.fetchTime,
            page.prevFetchTime,
            page.fetchCount,
            page.getFetchTimeHistory(""),
            page.protocolStatus,
            page.marks
        )

        write(report, "illegal-last-fetch-time.txt")
    }

    fun debugRedirects(url: String, urls: ActiveDOMUrls) {
        val location = urls.location
        if (location == url && urls.URL == url && urls.baseURI == url && urls.documentURI == url) {
            // no redirect
            return
        }

        val report = StringBuilder(url)

        report.append('\n')
        // NOTE: it seems they are all the same
        if (location != url) report.append("location:    ").appendln(location)
        if (urls.URL != location) report.append("URL:         ").appendln(urls.URL)
        if (urls.baseURI != location) report.append("baseURI:     ").appendln(urls.baseURI)
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
        var report = FetchStatusFormatter(page).toString()
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

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
