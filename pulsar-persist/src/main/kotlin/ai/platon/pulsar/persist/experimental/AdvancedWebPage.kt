package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Name
import java.time.Instant

/**
 * The core web page structure
 */
open class AdvancedWebPage(
    val page: GWebPage
) : KWebPage(GoraWebAssetImpl(page)) {

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    val fetchMode get() = FetchMode.fromString(metadata[Name.FETCH_MODE])

    override var lastBrowser: BrowserType?
        get() = BrowserType.fromString(page.browser?.toString())
        set(value) = run { page.browser = value?.name }

    override var fetchPriority
        get() = page.fetchPriority ?: AppConstants.FETCH_PRIORITY_DEFAULT
        set(value) = run { page.fetchPriority = value }

    override var generateTime
        get() = Instant.parse(metadata[Name.GENERATE_TIME] ?: "0")
        set(value) = run { metadata[Name.GENERATE_TIME] = value.toString() }

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     */
    override var prevCrawlTime1
        get() = Instant.ofEpochMilli(page.prevCrawlTime1)
        set(value) = run { page.prevCrawlTime1 = value.toEpochMilli() }

    override var modifiedTime
        get() = Instant.ofEpochMilli(page.modifiedTime)
        set(value) = run { page.modifiedTime = value.toEpochMilli() }

    override var prevModifiedTime
        get() = Instant.ofEpochMilli(page.prevModifiedTime)
        set(value) = run { page.prevModifiedTime = value.toEpochMilli() }

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

    val signature get() = page.signature

    val prevSignature get() = page.prevSignature

    var incomingLinks get() = page.inlinks
        set(value) = run { page.inlinks = value }
}
