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
import ai.platon.pulsar.persist.data.BrowserJsData
import ai.platon.pulsar.persist.data.DomStatistics
import ai.platon.pulsar.persist.data.LabeledHyperLink
import ai.platon.pulsar.persist.metadata.PageCategory
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
        val sb = StringBuilder()
        sb.appendln("-----------------")
        sb.appendln(page.url)
        val fields = page.pageModel.first()?.fields ?: return
        fields.entries.joinTo(sb, "\n") {
            String.format("%30s: %s", it.key, it.value)
        }
        sb.appendln()

        // show all javascript DOM urls, we should know the exact differences between them
        val urls = page.browserJsData?.urls
        if (urls != null) {
            sb.append("URL:         ").appendln(urls.URL)
            sb.append("baseURI:     ").appendln(urls.baseURI)
            sb.append("documentURI: ").appendln(urls.documentURI)
            sb.append("location:    ").appendln(urls.location)
        }

        sb.append("\n").append("Total ${fields.size} fields")
        sb.append("\n\n")
        writeLineTo(sb.toString(), "page-extracted-fields.txt")
    }

    fun reportGeneratedHosts(hostNames: Set<String>) {
        var report = ("# Total " + hostNames.size + " hosts generated : \n")
        report += hostNames.asSequence()
                .map { Urls.reverseHost(it) }.sorted().map { Urls.unreverseHost(it) }
                .map { String.format("%40s", it) }
                .joinToString("\n") { it }
        writeLineTo(report, "generate-hosts.txt")
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

    fun debugRedirects(url: String, urls: BrowserJsData.Urls) {
        val report = StringBuilder()
        report.appendln(url)
                .append("URL:         ").appendln(urls.URL)
                .append("baseURI:     ").appendln(urls.baseURI)
                .append("documentURI: ").appendln(urls.documentURI)
                .append("location:    ").appendln(urls.location)
                .append("\n\n\n")
        writeLineTo(report.toString(), "redirect.txt")
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
        private val prevFetchTime: Instant
        private val fetchTime: Instant
        private val fetchInterval: Duration
        private val distance: Int
        private val fetchCount: Int
        private val contentPublishTime: Instant
        private val refContentPublishTime: Instant
        private val pageCategory: PageCategory
        private val refArticles: Int
        private val refChars: Int
        private val contentScore: Double
        private val score: Double
        private val cash: Double
        private val url: String
        override fun toString(): String {
            val pattern = "yyyy-MM-dd HH:mm:ss"
            val fetchTimeString = (DateTimeUtil.format(prevFetchTime, pattern) + "->" + DateTimeUtil.format(fetchTime, pattern)
                    + "," + DurationFormatUtils.formatDuration(fetchInterval.toMillis(), "DdTH:mm:ss"))
            val params = Params.of(
                    "T", fetchTimeString,
                    "DC", "$distance,$fetchCount",
                    "PT", DateTimeUtil.isoInstantFormat(contentPublishTime.truncatedTo(ChronoUnit.SECONDS))
                    + "," + DateTimeUtil.isoInstantFormat(refContentPublishTime.truncatedTo(ChronoUnit.SECONDS)),
                    "C", "$refArticles,$refChars",
                    "S", df.format(contentScore) + "," + df.format(score) + "," + df.format(cash),
                    "U", StringUtils.substring(url, 0, 80)
            ).withKVDelimiter(":")
            return params.formatAsLine()
        }

        init {
            prevFetchTime = page.prevFetchTime
            fetchTime = page.fetchTime
            fetchInterval = page.fetchInterval
            distance = page.distance
            fetchCount = page.fetchCount
            contentPublishTime = page.contentPublishTime
            refContentPublishTime = page.refContentPublishTime
            pageCategory = page.pageCategory
            refArticles = page.pageCounters.get(PageCounters.Ref.item)
            refChars = page.pageCounters.get(PageCounters.Ref.ch)
            contentScore = page.contentScore.toDouble()
            score = page.score.toDouble()
            cash = page.cash.toDouble()
            url = page.url
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(MetricsSystem::class.java)
        val REPORT_LOG = MetricsReporter.LOG_NON_ADDITIVITY
    }
}