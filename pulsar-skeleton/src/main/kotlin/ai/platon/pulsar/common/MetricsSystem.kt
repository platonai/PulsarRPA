package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.common.WeakPageIndexer
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.*
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * Created by vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * A very simple metrics system
 *
 * TODO: Use com.codahale.metrics.MetricRegistry or Spark Metrics System
 */
class MetricsSystem(val webDb: WebDb, private val conf: ImmutableConfig) : AutoCloseable {
    private val df = DecimalFormat("0.0")
    private val hostname = NetUtil.getHostname()
    private val timeIdent = DateTimeUtil.format(System.currentTimeMillis(), "yyyyMMdd")
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME, DateTimeUtil.now("HHmm")]
    private val reportDir = AppPaths.get(AppPaths.REPORT_DIR, timeIdent, jobIdent)
    // TODO: job name is set in job setup phrase, so it's not available unless this is a [JobInitialized] class
    private val weakIndexer = WeakPageIndexer(AppConstants.CRAWL_LOG_HOME_URL, webDb)
    private val urlPrefix = AppConstants.CRAWL_LOG_INDEX_URL + "/" + DateTimeUtil.now("yyyy/MM/dd") + "/" + jobIdent + "/" + hostname
    private var reportCount = 0
    // We need predictable iteration order, LinkedHashSet is all right
    private val metricsPageUrls: MutableSet<CharSequence> = LinkedHashSet()
    private val metricsPages: MutableMap<String, WebPage> = HashMap()

    init {
        try {
            Files.createDirectories(reportDir)
        } catch (e: IOException) {
            LOG.error(e.toString())
        }
    }

    constructor(conf: ImmutableConfig): this(WebDb(conf), conf)

    fun commit() {
        // TODO : save only if dirty
        metricsPages.values.forEach { webDb.put(it) }
        webDb.flush()
    }

    override fun close() {
        commit()
        if (metricsPageUrls.isNotEmpty()) {
            weakIndexer.indexAll(metricsPageUrls)
            weakIndexer.commit()
        }
    }

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
        metricsPage.addLiveLink(HypeLink(page.url))
        metricsPage.setContent(metricsPage.contentAsString + PageReport(page) + "\n")
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
            metricsPage.metadata["JobName"] = conf[CapabilityTypes.PARAM_JOB_NAME, "job-unknown-" + DateTimeUtil.now("MMdd.HHmm")]
        }
        return metricsPage
    }

    fun getPageReport(page: WebPage): String {
        return PageReport(page).toString()
    }

    fun getPageReport(page: WebPage, verbose: Boolean): String {
        val pageReport = PageReport(page)
        var report = pageReport.toString()
        if (verbose) {
            report = page.referrer + " -> " + page.url + "\n" + report
            report += "\n" + page + "\n\n\n"
        }
        return report
    }

    fun reportPageFromSeedersist(report: String) { // String reportString = seedUrl + " -> " + url + "\n";
        writeLineTo(report, "fetch-urls-from-seed-persist.txt")
    }

    fun reportPageFromSeed(report: String) { // String reportString = seedUrl + " -> " + url + "\n";
        writeLineTo(report, "fetch-urls-from-seed.txt")
    }

    private fun reportFetchTimeHistory(fetchTimeHistory: String) {
        writeLineTo(fetchTimeHistory, "fetch-time-history.txt")
    }

    fun debugFetchHistory(page: WebPage) { // Debug fetch time history
        val fetchTimeHistory = page.getFetchTimeHistory("")
        if (fetchTimeHistory.contains(",")) {
            val report = "CST: ${page.crawlStatus}\tFTH: $fetchTimeHistory | ${page.url}"
            reportFetchTimeHistory(report)
        }
    }

    fun debugDeadOutgoingPage(deadUrl: String, page: WebPage) {
        val report = deadUrl + " <= " + page.url
        writeLineTo(report, "generate-sort-score.txt")
    }

    fun debugExtractedFields(page: WebPage) {
        if (page.pageModel.isEmpty) {
            return
        }

        val sb = StringBuilder()
        sb.appendln("-----------------")
        sb.appendln(page.url)
        val fields = page.pageModel.first()?.fields ?: return
        page.pageModel.list().forEach {
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
        writeLineTo(sb.toString(), "page-extracted-fields.txt")
    }

    fun reportGeneratedHosts(hostNames: Set<String>) {
        val report = StringBuilder("# Total " + hostNames.size + " hosts generated : \n")
        hostNames.asSequence()
                .map { Urls.reverseHost(it) }.sorted().map { Urls.unreverseHost(it) }
                .joinTo(report, "\n") { String.format("%40s", it) }
        writeLineTo(report.toString(), "generate-hosts.txt")
    }

    fun reportDOMStatistics(page: WebPage, stat: DomStatistics) {
        val report = String.format("%s | a:%-4d i:%-4d mi:%-4d ai:%-4d ia:%-4d | %s",
                page.pageCategory.symbol(),
                stat.anchor, stat.img, stat.mediumImg, stat.anchorImg, stat.imgAnchor,
                page.url)

        writeLineTo(report, "document-statistics.txt")
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
            writeLineTo(report, "labeled-links-$ident.txt")
        }
    }

    fun debugSortScore(page: WebPage) {
        val report = page.sortScore + "\t\t" + page.url + "\n"
        writeLineTo(report, "generate-sort-score.txt")
    }

    fun debugFetchLaterSeeds(page: WebPage) {
        val pageReport = PageReport(page)
        writeLineTo(pageReport.toString() + "\n", "seeds-fetch-later.txt")
    }

    fun debugDepthUpdated(report: String) {
        writeLineTo(report, "depth-updated.txt")
    }

    fun debugIllegalLastFetchTime(page: WebPage) {
        val report = String.format("ft: {} lft: {}, fc: {} fh: {} status: {} mk: {}",
                page.fetchTime,
                page.getLastFetchTime(Instant.now()),
                page.fetchCount,
                page.getFetchTimeHistory(""),
                page.protocolStatus,
                page.marks)

        writeLineTo(report, "illegal-last-fetch-time.txt")
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

        writeLineTo(report.toString(), "browser-redirects.txt")
    }

    fun reportRedirects(report: String) {
        writeLineTo(report, "fetch-redirects-txt")
    }

    fun reportFlawParsedPage(page: WebPage, verbose: Boolean) {
        val report = getFlawParsedPageReport(page, verbose)
        writeLineTo(report, "parse-flaw.txt")
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
            report = page.referrer + " -> " + page.url + "\n" + report
            report += "\n" + page + "\n\n\n"
        }

        return report
    }

    fun reportBrokenEntity(url: String, message: String) {
        val now = LocalDateTime.now()
        writeLineTo("$now\t$message\t$url", "broken-entity.txt")
    }

    fun reportPerformance(url: String?, elapsed: String) {
        writeLineTo("$elapsed | $url", "performance.txt")
    }

    fun debugLongUrls(report: String) {
        writeLineTo(report, "urls-long.txt")
    }

    fun debugIndexDocTime(timeStrings: String) {
        writeLineTo(timeStrings, "index-doc-time.txt")
    }

    fun reportFetchSchedule(page: WebPage, verbose: Boolean) {
        val report = getPageReport(page, verbose)
        val prefix = if (verbose) "verbose-" else ""
        val category = page.pageCategory.name.toLowerCase()
        writeLineTo(report, prefix + "fetch-schedule-$category.txt")
    }

    fun reportForceRefetchSeeds(report: String) {
        writeLineTo(report, "force-refetch-urls.txt")
    }

    fun reportBadModifiedTime(report: String) {
        writeLineTo(report, "bad-modified-urls.txt")
    }

    private fun writeLineTo(message: String, fileSuffix: String) {
        val reportFile = Paths.get(reportDir.toString(), fileSuffix)
        appendLineTo(reportFile, message)
    }

    /**
     * TODO: we may need a cache
     * */
    @Synchronized
    private fun appendLineTo(file: Path, message: String) {
        val options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        try {
            Files.newBufferedWriter(file, StandardCharsets.UTF_8, *options).use { 
                it.write(message)
                it.write("\n")
            }
        } catch (e: IOException) {
            LOG.error("Failed to write report : $e")
        }
    }

    private inner class PageReport(page: WebPage) {
        private val prevFetchTime = page.prevFetchTime
        private val fetchTime = page.fetchTime
        private val fetchInterval = page.fetchInterval
        private val distance = page.distance
        private val fetchCount = page.fetchCount
        private val contentPublishTime = page.contentPublishTime
        private val refContentPublishTime = page.refContentPublishTime
        private val pageCategory = page.pageCategory
        private val refItems = page.pageCounters.get(PageCounters.Ref.item)
        private val refChars = page.pageCounters.get(PageCounters.Ref.ch)
        private val contentScore = page.contentScore.toDouble()
        private val score = page.score.toDouble()
        private val cash = page.cash.toDouble()
        private val url = page.url

        override fun toString(): String {
            val pattern = "yyyy-MM-dd HH:mm:ss"
            val fetchTimeString = (DateTimeUtil.format(prevFetchTime, pattern) + "->" + DateTimeUtil.format(fetchTime, pattern)
                    + "," + DurationFormatUtils.formatDuration(fetchInterval.toMillis(), "DdTH:mm:ss"))

            val params = Params.of(
                    "T", fetchTimeString,
                    "DC", "$distance,$fetchCount",
                    "PT", DateTimeUtil.isoInstantFormat(contentPublishTime)
                    + "," + DateTimeUtil.isoInstantFormat(refContentPublishTime),
                    "C", "$refItems,$refChars",
                    "S", df.format(contentScore) + "," + df.format(score) + "," + df.format(cash),
                    pageCategory.symbol(), StringUtils.substring(url, 0, 80)
            ).withKVDelimiter(":")

            return params.formatAsLine()
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(MetricsSystem::class.java)
        val REPORT_LOG = MetricsReporter.LOG_NON_ADDITIVITY

        fun getFetchCompleteReport(page: WebPage, verbose: Boolean = false): String {
            val bytes = page.contentBytes
            if (bytes < 0) {
                return ""
            }

            val responseTime = page.metadata[Name.RESPONSE_TIME]?:""
            val proxy = page.metadata[Name.PROXY]
            val jsData = page.activeDomMultiStatus
            var jsSate = ""
            if (jsData != null) {
                val (ni, na, nnm, nst) = jsData.lastStat?: ActiveDomStat()
                jsSate = String.format(" i/a/nm/st:%d/%d/%d/%d", ni, na, nnm, nst)
            }

            val redirected = page.url != page.location
            val category = page.pageCategory.symbol()
            val numFields = page.pageModel.first()?.fields?.size?:0
            val proxyFmt = if (proxy == null) "%s" else "%26s"
            val jsFmt = if (jsSate.isBlank()) "%s" else "%30s"
            val fieldFmt = if (numFields == 0) "%s" else "%-3s"
            val fmt = "Fetched %s [%4d] %13s in %10s$proxyFmt, $jsFmt fc:%-2d nf:$fieldFmt | %s"
            val link = AppPaths.uniqueSymbolicLinkForURI(page.url)
            val url = if (redirected) page.location else page.url
            val readableUrl = if (redirected) "[R] $url" else url
            val readableLinks = if (verbose) "file://$link | $readableUrl" else readableUrl
            return String.format(fmt,
                    category,
                    page.protocolStatus.minorCode,
                    Strings.readableBytes(bytes.toLong(), 7, false),
                    DateTimeUtil.readableDuration(responseTime),
                    if (proxy == null) "" else " via $proxy",
                    jsSate,
                    page.fetchCount,
                    if (numFields == 0) "0" else numFields.toString(),
                    readableLinks
            )
        }

        fun getBatchCompleteReport(pages: Collection<WebPage>, startTime: Instant, verbose: Boolean = false): StringBuilder {
            val elapsed = DateTimeUtil.elapsedTime(startTime)
            val message = String.format("Fetched total %d pages in %s:\n", pages.size, DateTimeUtil.readableDuration(elapsed))
            val sb = StringBuilder(message)
            var i = 0
            pages.forEach { sb.append(++i).append(".\t").append(getFetchCompleteReport(it, verbose)).append('\n') }
            return sb
        }
    }
}
