package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Name
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The core web page structure
 */
open class AdvancedWebPage(
    page: GWebPage
) : KWebPage(page) {
    
    val isSeed get() = metadata.contains(Name.IS_SEED)

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    val fetchMode get() = FetchMode.fromString(metadata[Name.FETCH_MODE])

    override val lastBrowser get() = BrowserType.fromString(page.browser?.toString())

    override val fetchPriority get() = page.fetchPriority ?: AppConstants.FETCH_PRIORITY_DEFAULT

    override val generateTime get() = Instant.parse(metadata[Name.GENERATE_TIME] ?: "0")

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     */
    override val prevCrawlTime1 get() = Instant.ofEpochMilli(page.prevCrawlTime1)

    /**
     * Get fetch interval
     */
    override val fetchInterval: Duration
        get() = if (page.fetchInterval > 0) {
            Duration.ofSeconds(page.fetchInterval.toLong())
        } else ChronoUnit.CENTURIES.duration

    val reprUrl get() = if (page.reprUrl == null) "" else page.reprUrl.toString()

    override val modifiedTime get() = Instant.ofEpochMilli(page.modifiedTime)

    override val prevModifiedTime get() = Instant.ofEpochMilli(page.prevModifiedTime)

    open fun getFetchTimeHistory(defaultValue: String) = metadata[Name.FETCH_TIME_HISTORY] ?: defaultValue
    
    val contentTitle get() = page.contentTitle?.toString() ?: ""

    val pageText get() = page.pageText?.toString() ?: ""

    val contentText get() = page.contentText?.toString() ?: ""

    val contentTextLen get() = page.contentTextLen

    val contentPublishTime get() = Instant.ofEpochMilli(page.contentPublishTime)

    val prevContentPublishTime get() = Instant.ofEpochMilli(page.prevContentPublishTime)

    val refContentPublishTime get() = Instant.ofEpochMilli(page.refContentPublishTime)

    val contentModifiedTime get() = Instant.ofEpochMilli(page.contentModifiedTime)

    val prevContentModifiedTime get() = Instant.ofEpochMilli(page.prevContentModifiedTime)

    val prevRefContentPublishTime get() = Instant.ofEpochMilli(page.prevRefContentPublishTime)

    val score get() = page.score

    val contentScore get() = page.contentScore ?: 0.0

    val sortScore get() = page.sortScore?.toString() ?: ""

    val cash get() = metadata.getFloat(Name.CASH_KEY, 0.0f)

    val signature get() = page.signature

    val prevSignature get() = page.prevSignature

    val pageCounters get() = PageCounters.box(page.pageCounters)
    
    var incomingLinks get() = page.inlinks
        set(value) = run { page.inlinks = value }
}
