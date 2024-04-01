package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.gora.generated.*
import ai.platon.pulsar.persist.metadata.*
import ai.platon.pulsar.persist.metadata.OpenPageCategory.Companion.parse
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import ai.platon.pulsar.persist.model.Converters.convert
import ai.platon.pulsar.persist.model.PageModel
import ai.platon.pulsar.persist.model.PageModel.Companion.box
import org.apache.gora.util.ByteUtils
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * The core web page structure
 */
open class BasicWebPage(
    val page: GWebPage
) : KWebAsset, WebAssetState {
    companion object {
        private val ID_SUPPLIER = AtomicInteger()
    }
    
    override val id: Int = ID_SUPPLIER.incrementAndGet()

    /**
     * The url is the permanent internal address, and the location is the last working address
     */
    override var url = ""
    
    override var args = ""
    
    /**
     * Web page scope configuration
     */
    override val conf: VolatileConfig = TODO()

    /**
     * Web page scope variables
     */
    override val variables = Variables()

    /**
     * If this page is fetched from internet
     */
    override var isCached = false

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    override var isLoaded = false

    /**
     * If this page is fetched from internet
     */
    override var isFetched = false
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If this page is canceled
     */
    override var isCanceled = false

    /**
     * If this page is fetched and updated
     */
    override val isContentUpdated = false

    /**
     * The delay time to retry if a retry is needed
     */
    override val retryDelay = Duration.ZERO

    val key: String get() = reversedUrl
    /**
     * Get The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     */
    override val href: String? get() = metadata[Name.HREF]
    val isInternal get() = hasMark(Mark.INTERNAL)
    val isNotInternal get() = !isInternal
    
    private var contentCache: ByteBuffer? = null

    /**
     * Check if the page scope temporary variable with name {@name} exist
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    fun hasVal(name: String) = variables.contains(name)

    /**
     * Get a page scope temporary variable
     *
     * @param name a [String] object.
     * @return a Object or null.
     */
    fun getVar(name: String) = variables[name]

    fun hasMark(mark: Mark) = marks[mark] != null

    override val metadata get() = Metadata.box(page.metadata)

    val marks get() = CrawlMarks.box(page.markers)

    // The underlying field should not use name 'args'
    /**
     * Set the local args variable and the persist version, and also clear the load options.
     */
    /**
     * The load arguments is valiant task by task, so the local version is the first choice,
     * while the persisted version is used for historical check only
     *
     * The underlying field should not use name 'args' since it exists already
     * with another gora type, see GProtocolStatus.args and GParseStatus.args
     */
//    override val args: String get() = page.params?.toString() ?: ""

    override val maxRetries: Int get() = metadata.getInt(Name.FETCH_MAX_RETRY, 3)

    override val configuredUrl get() = UrlUtils.mergeUrlArgs(url, args)

    override val fetchedLinkCount get() = metadata.getInt(Name.FETCHED_LINK_COUNT, 0)

    override val zoneId get() = if (page.zoneId == null) DateTimes.zoneId else ZoneId.of(page.zoneId.toString())

    override val batchId get() = page.batchId?.toString() ?: ""

    override val lastBrowser get() = BrowserType.fromString(page.browser?.toString())

    val isResource get() = page.resource != null

    override val htmlIntegrity get() = HtmlIntegrity.fromString(page.htmlIntegrity?.toString())

    override val fetchPriority get() = page.fetchPriority ?: AppConstants.FETCH_PRIORITY_DEFAULT

    override val createTime get() = Instant.ofEpochMilli(page.createTime)

    override val generateTime get() = Instant.parse(metadata[Name.GENERATE_TIME] ?: "0")

    override val fetchCount: Int get() = page.fetchCount

    override val crawlStatus get() = CrawlStatus(page.crawlStatus.toByte())

    /**
     * The baseUrl is as the same as Location.
     *
     * A baseUrl has the same semantic with Jsoup.parse:
     *
     * @return a [String] object.
     * @link {https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse-java.io.File-java.lang.String-java.lang.String-}
     * @see BasicWebPage.getLocation
     */
    override val baseUrl: String
        get() = if (page.baseUrl == null) "" else page.baseUrl.toString()

    /**
     * WebPage.url is the permanent internal address, it might not still available to access the target.
     * And WebPage.location or WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url, it's generally normalized.
     */
    override val location: String get() = baseUrl

    /**
     * The latest fetch time
     *
     * @return The latest fetch time
     */
    override val fetchTime get() = Instant.ofEpochMilli(page.fetchTime)

    /**
     * The previous fetch time, updated at the fetch stage
     *
     * @return The previous fetch time.
     */
    override val prevFetchTime get() = Instant.ofEpochMilli(page.prevFetchTime)

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

    /**
     * Get protocol status
     */
    override val protocolStatus get() = ProtocolStatus.box(page.protocolStatus ?: GProtocolStatus.newBuilder().build())

    /**
     * Header information returned from the web server used to server the content which is subsequently fetched from.
     * This includes keys such as
     * TRANSFER_ENCODING,
     * CONTENT_ENCODING,
     * CONTENT_LANGUAGE,
     * CONTENT_LENGTH,
     * CONTENT_LOCATION,
     * CONTENT_DISPOSITION,
     * CONTENT_MD5,
     * CONTENT_TYPE,
     * LAST_MODIFIED
     * and LOCATION.
     */
    override val headers get() = ProtocolHeaders.box(page.headers)

    override val fetchRetries get() = page.fetchRetries

    override val modifiedTime get() = Instant.ofEpochMilli(page.modifiedTime)

    override val prevModifiedTime get() = Instant.ofEpochMilli(page.prevModifiedTime)

    override val pageCategory: PageCategory
        get() = kotlin.runCatching { PageCategory.parse(page.pageCategory.toString()) }.getOrNull()
            ?: PageCategory.UNKNOWN

    override val openPageCategory: OpenPageCategory
        get() = kotlin.runCatching { parse(page.pageCategory.toString()) }.getOrNull()
            ?: OpenPageCategory("", "")

    /**
     * Get the encoding of the content.
     * Content encoding is detected just before it's parsed.
     */
    override val encoding get() = page.encoding?.toString()
    
    override val content: ByteBuffer? get() = getContentWithLocalCache()
    override val persistContent: ByteBuffer? get() = getPersistContent0()
    override val contentAsBytes: ByteArray? get() = getContentAsBytes0()
    override val contentAsString: String? get() = getContentAsString0()
    override val contentAsInputStream: ByteArrayInputStream get() = getContentAsInputStream0()

    /**
     * Get the page content as sax input source
     */
    override val contentAsSaxInputSource: InputSource get() = getContentAsSaxInputSource0()
    
    /**
     * Get the length of content in bytes.
     *
     * TODO: check consistency with HttpHeaders.CONTENT_LENGTH
     *
     * @return The length of the content in bytes.
     */
    override var contentLength get() = page.contentLength ?: 0

    /**
     * Compute the length of content in bytes.
     */
    private fun computeContentLength(bytes: Long) {
        val lastBytes = contentLength
        page.lastContentLength = lastBytes
        page.contentLength = bytes
        computeAveContentLength(bytes)
    }

    private fun computeAveContentLength(bytes: Long) {
        val count = fetchCount
        val lastAveBytes = page.aveContentLength
        val aveBytes: Long
        aveBytes = if (count > 0 && lastAveBytes == 0L) {
            // old version, average bytes is not calculated
            bytes
        } else {
            (lastAveBytes * count + bytes) / (count + 1)
        }
        page.aveContentLength = aveBytes
    }

    override var persistedContentLength get() = page.persistedContentLength ?: 0

    override var lastContentLength get() = page.lastContentLength ?: 0

    override var aveContentLength get() = page.aveContentLength ?: 0

    override var contentType get() = page.contentType?.toString() ?: ""
        set(value) = run { page.contentType = value }

    /**
     * The last proxy used to fetch the page
     */
    override val proxy get() = page.proxy?.toString()

    override val activeDOMStatus: ActiveDOMStatus?
        get() {
            val s = page.activeDOMStatus ?: return null
            return ActiveDOMStatus(
                s.n,
                s.scroll,
                s.st.toString(),
                s.r.toString(),
                s.idl.toString(),
                s.ec.toString()
            )
        }

    override var activeDOMStatTrace get() = page.activeDOMStatTrace.entries.associate { it.key.toString() to convert(it.value) }
        set(value) = run { page.activeDOMStatTrace = value.entries.associate { it.key to convert(it.value) } }

    override var pageTitle get() = page.pageTitle?.toString() ?: ""
        set(value) = run { page.pageTitle = value }

    override var parseStatus get() = ParseStatus.box(page.parseStatus ?: GParseStatus.newBuilder().build())
        set(value) = run { page.parseStatus = value.unbox() }

    override var liveLinks get() = page.liveLinks
        set(value) = run { page.liveLinks = value }

    val simpleLiveLinks get() = page.liveLinks.keys.map { it.toString() }

    override var vividLinks get() = page.vividLinks
        set(value) = run { page.vividLinks = value }

    val simpleVividLinks get() = page.vividLinks.keys.map { it.toString() }

    override var deadLinks get() = page.deadLinks
        set(value) = run { page.deadLinks = value }

    override var links get() = page.links
        set(value) = run { page.links = value }

    override var estimatedLinkCount get() = metadata.get(Name.TOTAL_OUT_LINKS)?.toIntOrNull() ?: 0
        set(value) = run { metadata[Name.TOTAL_OUT_LINKS] = value.toString() }

    override var anchor get() = page.anchor ?: ""
        set(value) = run { page.anchor = value }

    override var anchorOrder get() = page.anchorOrder
        set(value) = run { page.anchorOrder = value }

    override var referrer get() = if (page.referrer == null) null else page.referrer.toString()
        set(value) = run { page.referrer = value }

    /**
     * *****************************************************************************
     * Page Model
     * ******************************************************************************
     */
    override val pageModelUpdateTime get() = Instant.ofEpochMilli(page.pageModelUpdateTime)

    override val pageModel get() = if (page.pageModel == null) null else box(page.pageModel)

    open fun ensurePageModel(): PageModel {
        if (page.pageModel == null) {
            page.pageModel = GPageModel.newBuilder().build()
        }
        return pageModel!!
    }

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     */
    override fun hashCode() = url.hashCode()

    override fun equals(other: Any?): Boolean {
        return if (this === other) true else other is BasicWebPage && other.url == url
    }

    override fun toString() = url
    
    /**
     * The entire raw document content e.g. raw XHTML
     *
     * @return The raw document content in [ByteBuffer].
     */
    private fun getContentWithLocalCache(): ByteBuffer? {
        if (contentCache != null) {
            return contentCache
        }
        
        return getPersistContent0()
    }
    
    /**
     * Get the persistent page content
     */
    private fun getPersistContent0(): ByteBuffer = page.content
    
    /**
     * Get content as bytes, the underling buffer is duplicated
     *
     * @return a duplication of the underling buffer.
     */
    private fun getContentAsBytes0(): ByteArray {
        val content = getContentWithLocalCache() ?: return ByteUtils.toBytes('\u0000')
        return ByteUtils.toBytes(content)
    }
    
    /**
     * Get the page content as a string, if the underlying page content is null, return an empty string
     */
    private fun getContentAsString0(): String {
        val buffer = getContentWithLocalCache()
        return if (buffer == null || buffer.remaining() == 0) {
            ""
        } else String(buffer.array(), buffer.arrayOffset(), buffer.limit())
    }
    
    /**
     * Get the page content as input stream
     */
    private fun getContentAsInputStream0(): ByteArrayInputStream {
        val contentInOctets = getContentWithLocalCache() ?: return ByteArrayInputStream(ByteUtils.toBytes('\u0000'))
        return ByteArrayInputStream(
            contentInOctets.array(), contentInOctets.arrayOffset() + contentInOctets.position(),
            contentInOctets.remaining()
        )
    }

    private fun getContentAsSaxInputSource0(): InputSource {
        val inputSource = InputSource(getContentAsInputStream0())
        val encoding = encoding
        if (encoding != null) {
            inputSource.encoding = encoding
        }
        return inputSource
    }
}
