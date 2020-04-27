package ai.platon.pulsar.common.message

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.ActiveDomStat
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import java.text.DecimalFormat
import java.time.Instant

class PageFormatter(val page: WebPage) {
    companion object {
        private val df = DecimalFormat("0.0")
    }
    private val prevFetchTime get() = page.prevFetchTime
    private val fetchTime get() = page.fetchTime
    private val fetchInterval get() = page.fetchInterval
    private val distance get() = page.distance
    private val fetchCount get() = page.fetchCount
    private val contentPublishTime get() = page.contentPublishTime
    private val refContentPublishTime get() = page.refContentPublishTime
    private val pageCategory get() = page.pageCategory
    private val refItems get() = page.pageCounters.get(PageCounters.Ref.item)
    private val refChars get() = page.pageCounters.get(PageCounters.Ref.ch)
    private val contentScore get() = page.contentScore.toDouble()
    private val score get() = page.score.toDouble()
    private val cash get() = page.cash.toDouble()
    private val url get() = page.url

    override fun toString(): String {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val fetchTimeString = (DateTimes.format(prevFetchTime, pattern) + "->" + DateTimes.format(fetchTime, pattern)
                + "," + DurationFormatUtils.formatDuration(fetchInterval.toMillis(), "DdTH:mm:ss"))

        val params = Params.of(
                "T", fetchTimeString,
                "DC", "$distance,$fetchCount",
                "PT", DateTimes.isoInstantFormat(contentPublishTime)
                + "," + DateTimes.isoInstantFormat(refContentPublishTime),
                "C", "$refItems,$refChars",
                "S", df.format(contentScore) + "," + df.format(score) + "," + df.format(cash),
                pageCategory.symbol(), StringUtils.substring(url, 0, 80)
        ).withKVDelimiter(":")

        return params.formatAsLine()
    }
}

class CompletedPageFormatter(
        private val page: WebPage,
        private val verbose: Boolean = false
) {
    val contentBytes get() = page.contentBytes
    val responseTime get() = page.metadata[Name.RESPONSE_TIME]?:""
    val proxy get() = page.metadata[Name.PROXY]
    val jsData get() = page.activeDomMultiStatus
    val jsSate get() = if (jsData != null) {
        val (ni, na, nnm, nst) = jsData?.lastStat?: ActiveDomStat()
        String.format(" i/a/nm/st:%d/%d/%d/%d", ni, na, nnm, nst)
    } else ""

    val redirected get() = page.url != page.location
    val category get() = page.pageCategory.symbol()
    val numFields get() = page.pageModel.first()?.fields?.size?:0
    val proxyFmt get() = if (proxy == null) "%s" else "%26s"
    val jsFmt get() = if (jsSate.isBlank()) "%s" else "%30s"
    val fieldFmt get() = if (numFields == 0) "%s" else "%-3s"
    val fmt get() = "Fetched %3d %s [%4d] %13s in %10s$proxyFmt, $jsFmt fc:%-2d nf:$fieldFmt | %s"
    val link get() = AppPaths.uniqueSymbolicLinkForUri(page.url)
    val url get() = if (redirected) page.location else page.url
    val readableUrl get() = if (redirected) "[R] $url" else url
    val readableLinks get() = if (verbose) "file://$link | $readableUrl" else readableUrl

    override fun toString(): String {
        return String.format(fmt,
                page.sequence,
                category,
                page.protocolStatus.minorCode,
                Strings.readableBytes(contentBytes.toLong(), 7, false),
                DateTimes.readableDuration(responseTime),
                if (proxy == null) "" else " via $proxy",
                jsSate,
                page.fetchCount,
                if (numFields == 0) "0" else numFields.toString(),
                readableLinks
        )
    }
}

class LoadCompletedPagesFormatter(
        val pages: Collection<WebPage>,
        val startTime: Instant,
        val verbose: Boolean = false
) {
    override fun toString(): String {
        val elapsed = DateTimes.elapsedTime(startTime)
        val message = String.format("Fetched total %d pages in %s:\n", pages.size, elapsed.readable())
        val sb = StringBuilder(message)
        var i = 0
        pages.forEach { sb.append(++i).append(".\t").append(CompletedPageFormatter(it, verbose)).append('\n') }
        return sb.toString()
    }
}
