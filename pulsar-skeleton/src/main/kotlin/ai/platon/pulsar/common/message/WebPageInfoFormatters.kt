package ai.platon.pulsar.common.message

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.persist.ext.options
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

class LoadedPageFormatter(
        private val page: WebPage,
        private val prefix: String = "",
        private val withOptions: Boolean = false,
        private val withNormUrl: Boolean = false,
        private val withReferer: Boolean = false,
        private val withSymbolicLink: Boolean = false
) {
    private val url get() = page.url
    private val href get() = page.href
    private val location get() = page.location
    private val responseTime get() = page.metadata[Name.RESPONSE_TIME]?:""
    private val proxy get() = page.metadata[Name.PROXY]
    private val activeDomStats = page.activeDomStats
    private val m get() = page.pageModel

    private val jsSate: String
        get() {
            val (ni, na, nnm, nst, w, h) = activeDomStats["lastStat"]?: ActiveDomStat()
            return if (ni + na + nnm + nst + h != 0) {
                String.format(" i/a/nm/st/h:%d/%d/%d/%d/%d", ni, na, nnm, nst, h)
            } else ""
        }

    private val prefix0 get() = if (page.isContentUpdated) "Fetched" else "Loaded"
    private val prefix1 get() = prefix.takeIf { it.isNotEmpty() } ?: prefix0
    private val label = StringUtils.abbreviateMiddle(page.options.label, "..", 20)
    private val formattedLabel get() = if (label.isBlank()) " | " else " | $label | "
    private val category get() = page.pageCategory.symbol()
//    private val prevFetchTime get() = if (page.isLoaded) " | ${page.prevFetchTime} | " else ""
    private val prevFetchTime get() = " | ${page.prevFetchTime} | "
    private val numFields get() = String.format("%d/%d/%d", m.numNonBlankFields, m.numNonNullFields, m.numFields)
    private val proxyFmt get() = if (proxy == null) "%s" else " | %s"
    private val jsFmt get() = if (jsSate.isBlank()) "%s" else "%30s"
    private val fetchCount get() =
        if (page.protocolStatus.isRetry) {
            String.format("%d/%d", page.fetchRetries, page.fetchCount)
        } else {
            String.format("%d", page.fetchCount)
        }
    private val fieldFmt get() = if (m.numFields == 0) "%s" else " nf:%-10s"
    private val failure get() = if (page.protocolStatus.isFailed) String.format(" | %s", page.protocolStatus) else ""
    private val symbolicLink get() = AppPaths.uniqueSymbolicLinkForUri(page.url)

    private val fmt get() = "%3d. $prefix1 %s [%4d] %13s in %10s, $jsFmt fc:$fetchCount $prevFetchTime$fieldFmt$failure$proxyFmt" +
            " | %s$formattedLabel%s"

    override fun toString(): String {
        return String.format(fmt,
                page.id,
                category,
                page.protocolStatus.minorCode,
                buildContentBytes(),
                DateTimes.readableDuration(responseTime),
                jsSate,
                if (m.numFields == 0) "" else numFields,
                proxy?:"",
                page.variables["privacyContext"]?:"",
                buildLocation()
        )
    }

    private fun buildContentBytes(): String {
        return if (page.lastContentBytes == 0L || page.lastContentBytes == page.contentLength) {
            readableBytes(page.contentLength)
        } else {
            readableBytes(page.contentLength).trim() + " <- " + readableBytes(page.lastContentBytes).trim()
        }
    }

    private fun readableBytes(bytes: Long): String {
        return Strings.readableBytes(bytes, 7, false)
    }

    private fun buildLocation(): String {
        val expectedLocation = href ?: url
        val redirected = href != null && href != location
        val normalized = href != null && href != url
        var location = if (redirected) location else expectedLocation
        if (withOptions) location += " ${page.args}"
        val readableLocation0 = if (redirected) "[R] $location <- $expectedLocation" else location
        var readableLocation = if (normalized) "[N] $readableLocation0" else readableLocation0
        if (withNormUrl) readableLocation = "$readableLocation <- $url"
        if (withReferer) readableLocation = "$readableLocation <- ${page.referrer}"
        return if (withSymbolicLink) "file://$symbolicLink | $readableLocation" else readableLocation
    }
}

class LoadedPagesFormatter(
        val pages: Collection<WebPage>,
        val startTime: Instant,
        val withSymbolicLink: Boolean = false
) {
    override fun toString(): String {
        val elapsed = DateTimes.elapsedTime(startTime)
        val message = String.format("Fetched total %d pages in %s:\n", pages.size, elapsed.readable())
        val sb = StringBuilder(message)
        pages.forEachIndexed { i, p ->
            sb.append(i.inc()).append(".\t").append(LoadedPageFormatter(p, withSymbolicLink = withSymbolicLink)).append('\n')
        }
        return sb.toString()
    }
}
