package ai.platon.pulsar.common.message

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.PulsarParams.VAR_PRIVACY_CONTEXT_NAME
import ai.platon.pulsar.common.PulsarParams.VAR_FETCH_STATE
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.emoji.UnicodeEmoji
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.common.FetchState
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.ActiveDOMStat
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant

class FetchStatusFormatter(val page: WebPage) {
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

class LoadStatusFormatter(
        private val page: WebPage,
        private val prefix: String = "",
        private val withOptions: Boolean = false,
        private val withNormUrl: Boolean = false,
        private val withReferer: Boolean = false,
        private val withSymbolicLink: Boolean = false
) {
    private val verboseCount = 200
    private val url get() = page.url
    private val href get() = page.href
    private val location get() = page.location
    private val responseTime get() = page.metadata[Name.RESPONSE_TIME]?:""
    private val proxy get() = page.proxy
    private val protocolStatus get() = page.protocolStatus
    private val activeDOMStatTrace = page.activeDOMStatTrace
    private val m get() = page.pageModel

    private val jsSate: String
        get() {
            val (ni, na, nnm, nst, w, h) = activeDOMStatTrace["lastStat"]?: ActiveDOMStat()
            val divisor = if (page.id < verboseCount) 10 else verboseCount
            val prefix = if (page.id % divisor == 0) {
                "i/a/nm/st/h:"
            } else ""
            return if (ni + na + nnm + nst + h != 0) {
                String.format("$prefix%d/%d/%d/%d/%d", ni, na, nnm, nst, h)
            } else ""
        }

    private val fetchReason get() = buildFetchReason()
    private val loadStatusSymbol get() = when {
        page.isCanceled -> UnicodeEmoji.CANCELLATION_X // canceled
        page.isFetched && page.fetchCount == 1 -> UnicodeEmoji.LIGHTNING // fetched new
        page.isFetched -> UnicodeEmoji.CIRCLE_ARROW_1 // fetched, reload
        page.isCached -> UnicodeEmoji.HOT_BEVERAGE // cached
        page.isLoaded -> UnicodeEmoji.OPTICAL_DISC   // load from db
        else -> UnicodeEmoji.BUG  // BUG symbol
    }
    private val loadStatusText get() = when {
        page.isCanceled -> "Canceled"
        page.isFetched && page.fetchCount == 1 -> "New"
        page.isFetched -> "Updated"
        page.isCached -> "Cached"
        page.isLoaded -> "Loaded"
        else -> "Unknown"
    }
    private val loadStatus: String get() = when {
        page.id < verboseCount && page.id % 10 == 0 -> "$loadStatusText $loadStatusSymbol"
        page.id > verboseCount && page.id % verboseCount == 0 -> "$loadStatusText $loadStatusSymbol"
        else -> loadStatusSymbol.toString()
    }
    private val loadMessagePrefix get() = prefix.takeIf { it.isNotEmpty() } ?: loadStatus
    private val successSymbol: String get() = when {
        page.isCanceled -> "${UnicodeEmoji.CANCELLATION_X} " // 'CANCELLATION X' (U+1F5D9)
        protocolStatus.isSuccess -> "${UnicodeEmoji.HUNDRED_POINTS} " // 100 score
        else -> "${UnicodeEmoji.BROKEN_HEART} " // broken heart
    }
    private val label = StringUtils.abbreviateMiddle(page.options.label, "..", 20)
    private val formattedLabel get() = if (label.isBlank()) "" else " | $label"
    private val category get() = page.pageCategory.symbol()
    private val prevFetchTimeBeforeUpdate = page.getVar(PulsarParams.VAR_PREV_FETCH_TIME_BEFORE_UPDATE) as? Instant ?: page.prevFetchTime
    private val prevFetchTimeDuration: Duration get() = Duration.between(prevFetchTimeBeforeUpdate, Instant.now())
    private val prevFetchTimeReport: String get() = when {
        prevFetchTimeDuration.toDays() > 20 * 360 -> ""
        else -> " last fetched ${prevFetchTimeDuration.readable()} ago,"
    }
    private val fieldCount: String get() = when {
        m == null -> ""
        m?.numFields == 0 -> ""
        else -> String.format("%d/%d/%d", m?.numNonBlankFields?:0, m?.numNonNullFields?:0, m?.numFields?:0)
    }
    private val proxyFmt get() = if (proxy.isNullOrBlank()) "%s" else " | %s"
    private val jsFmt get() = if (jsSate.isBlank()) "%s" else " | %s"
    private val fetchCount get() = when {
        page.fetchRetries > 0 -> String.format("%d/%d", page.fetchRetries, page.fetchCount)
        else -> String.format("%d", page.fetchCount)
    }
    private val fieldCountFmt get() = if (m == null || m?.numFields == 0) "%s" else " | nf:%-10s"
    private val failure get() = if (page.protocolStatus.isFailed) String.format(" %s", page.protocolStatus) else ""
    private val symbolicLink get() = AppPaths.uniqueSymbolicLinkForUri(page.url)
    private val contextName get() = page.variables[VAR_PRIVACY_CONTEXT_NAME]?.let { " | $it" } ?: ""

    private val fmt get() = "%3d. $successSymbol$loadMessagePrefix %s $fetchReason got %d %s in %s," +
            "$prevFetchTimeReport fc:$fetchCount$failure" +
            "$jsFmt$fieldCountFmt$proxyFmt$contextName$formattedLabel | %s"

    data class Record(
        val name: String,
        val value: Any,
        val prefix: String = "",
        val postfix: String = "",
        val width: Int = 0,
    )

    fun explain() {
        listOf(
            Record("id", page.id, width = 3),
            Record("successSymbol", successSymbol, width = 1),
            Record("prefix", loadMessagePrefix),
            Record("minorCode", page.protocolStatus.minorCode),
            Record("fetchReason", fetchReason, width = 1),
        )
    }

    override fun toString(): String {
        return String.format(fmt,
                page.id,
                category,
                page.protocolStatus.minorCode,
                buildContentBytes(),
                DateTimes.readableDuration(responseTime),
                jsSate,
                fieldCount,
                proxy?:"",
                buildLocation()
        )
    }

    private fun buildFetchReason(): String {
        val state = page.variables[VAR_FETCH_STATE] as? CheckState
        val code = state?.code ?: FetchState.DO_NOT_FETCH
        return FetchState.toSymbol(code).takeIf { it.isNotBlank() }?.let { "for $it" } ?: ""
    }

    private fun buildContentBytes(): String {
        var contentBytes = if (page.lastContentLength == 0L || page.lastContentLength == page.contentLength) {
            readableBytes(page.contentLength).trim()
        } else {
            readableBytes(page.contentLength).trim() + " <- " + readableBytes(page.lastContentLength).trim()
        }

        if (page.content == null) {
            contentBytes = "0 <- $contentBytes"
        }

        return contentBytes
    }

    private fun readableBytes(bytes: Long): String {
        return if (bytes == 0L) "0" else Strings.readableBytes(bytes, 7, false)
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
        val doWithSymbolicLink = page.id < verboseCount || withSymbolicLink
        return if (doWithSymbolicLink) "file://$symbolicLink | $readableLocation" else readableLocation
    }
}

class LoadedPagesStatusFormatter(
        val pages: Collection<WebPage>,
        val startTime: Instant,
        val withSymbolicLink: Boolean = false
) {
    override fun toString(): String {
        val elapsed = DateTimes.elapsedTime(startTime)
        val message = String.format("Fetched total %d pages in %s:\n", pages.size, elapsed.readable())
        val sb = StringBuilder(message)
        pages.forEachIndexed { i, p ->
            sb.append(i.inc()).append(".\t").append(LoadStatusFormatter(p, withSymbolicLink = withSymbolicLink)).append('\n')
        }
        return sb.toString()
    }
}
