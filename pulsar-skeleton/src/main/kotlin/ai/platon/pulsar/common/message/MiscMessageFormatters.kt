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
                "PT", DateTimes.isoInstantFormat(contentPublishTime) + "," + DateTimes.isoInstantFormat(refContentPublishTime),
                "C", "$refItems,$refChars",
                "S", df.format(contentScore) + "," + df.format(score) + "," + df.format(cash),
                pageCategory.symbol(), StringUtils.substring(url, 0, 80)
        ).withKVDelimiter(":")

        return params.formatAsLine()
    }
}

class CompletedPageFormatter(
        private val page: WebPage,
        private val prefix: String = "Fetched",
        private val withNormUrl: Boolean = false,
        private val withReferer: Boolean = false,
        private val withSymbolicLink: Boolean = false
) {
    val contentBytes get() = page.contentBytes
    val responseTime get() = page.metadata[Name.RESPONSE_TIME]?:""
    val proxy get() = page.metadata[Name.PROXY]
    val activeDomStats = page.activeDomStats
    val m get() = page.pageModel

    val jsSate get() = run {
        val (ni, na, nnm, nst, w, h) = activeDomStats["lastStat"]?: ActiveDomStat()
        String.format(" i/a/nm/st/h:%d/%d/%d/%d/%d", ni, na, nnm, nst, h)
    }

    val label get() = StringUtils.abbreviateMiddle(page.label, "..", 20)
    val formattedLabel get() = if (label.isBlank()) " | " else " | $label | "
    val category get() = page.pageCategory.symbol()
    val numFields get() = String.format("%d/%d/%d", m.numNonBlankFields, m.numNonNullFields, m.numFields)
    val proxyFmt get() = if (proxy == null) "%s" else " | %s"
    val jsFmt get() = if (jsSate.isBlank()) "%s" else "%30s"
    val fieldFmt get() = if (m.numFields == 0) "%s" else " nf:%-10s"
    val failure get() = if (page.protocolStatus.isFailed) String.format(" | %s", page.protocolStatus) else ""
    val symbolicLink get() = AppPaths.uniqueSymbolicLinkForUri(page.url)

    val fmt get() = "%3d. $prefix %s [%4d] %13s in %10s, $jsFmt fc:%-2d$fieldFmt$failure$proxyFmt" +
            " | %s$formattedLabel%s"

    override fun toString(): String {
        return String.format(fmt,
                page.id,
                category,
                page.protocolStatus.minorCode,
                Strings.readableBytes(contentBytes.toLong(), 7, false),
                DateTimes.readableDuration(responseTime),
                jsSate,
                page.fetchCount,
                if (m.numFields == 0) "" else numFields,
                proxy?:"",
                page.variables["privacyContext"]?:"",
                buildLocation()
        )
    }

    private fun buildLocation(): String {
        val expectedLocation = page.href ?: page.url
        val redirected = expectedLocation != page.location
        val normalized = page.href != null && page.href != page.url
        val location = if (redirected) page.location else expectedLocation
        val readableLocation0 = if (redirected) "[R] $location <- $expectedLocation" else location
        var readableLocation = if (normalized) "[N] $readableLocation0" else readableLocation0
        if (withNormUrl) readableLocation = "$readableLocation <- ${page.url}"
        if (withReferer) readableLocation = "$readableLocation <- ${page.referrer}"
        return if (withSymbolicLink) "file://$symbolicLink | $readableLocation" else readableLocation
    }
}

class LoadCompletedPagesFormatter(
        val pages: Collection<WebPage>,
        val startTime: Instant,
        val withSymbolicLink: Boolean = false
) {
    override fun toString(): String {
        val elapsed = DateTimes.elapsedTime(startTime)
        val message = String.format("Fetched total %d pages in %s:\n", pages.size, elapsed.readable())
        val sb = StringBuilder(message)
        pages.forEachIndexed { i, p ->
            sb.append(i.inc()).append(".\t").append(CompletedPageFormatter(p, withSymbolicLink = withSymbolicLink)).append('\n')
        }
        return sb.toString()
    }
}
