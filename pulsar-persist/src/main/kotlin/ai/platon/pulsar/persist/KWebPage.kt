package ai.platon.pulsar.persist

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.HtmlIntegrity.Companion.fromString
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.UrlUtils.normalize
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.gora.generated.GParseStatus
import ai.platon.pulsar.persist.gora.generated.GProtocolStatus
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.*
import ai.platon.pulsar.persist.model.ActiveDOMStatTrace
import ai.platon.pulsar.persist.model.ActiveDOMUrls
import ai.platon.pulsar.persist.model.PageModel
import org.apache.avro.util.Utf8
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.math.NumberUtils
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty

class MetadataField<T>(val initializer: (KWebPage) -> T) {
    operator fun getValue(thisRef: KWebPage, property: KProperty<*>): T =
        thisRef.metadata[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: KWebPage, property: KProperty<*>, value: T): T {
        thisRef.metadata.set(property.name, value.toString())
        return value
    }
}

class NullableMetadataField<T> {
    operator fun getValue(thisRef: KWebPage, property: KProperty<*>): T? =
        thisRef.metadata[property.name] as? T

    operator fun setValue(thisRef: KWebPage, property: KProperty<*>, value: T?): T? {
        thisRef.metadata.set(property.name, value?.toString())
        return value
    }
}

fun <T> field(initializer: (KWebPage) -> T): MetadataField<T> {
    return MetadataField(initializer)
}

inline fun <reified T> nullableField(): NullableMetadataField<T> {
    return NullableMetadataField()
}

/**
 * The core data structure across the whole program execution
 *
 * Notice: Use a build-in java string or a Utf8 to serialize strings?
 *
 * see org .apache.gora.hbase.util.HBaseByteInterface #fromBytes
 *
 * In serialization phrase, a byte array created by s.getBytes(UTF8_CHARSET) is serialized, and
 * in deserialization phrase, every string are wrapped to be a Utf8
 *
 * So both build-in string and a Utf8 wrap is OK to serialize, and Utf8 is always returned.
 */
class KWebPage(
    /**
     * The url is the permanent internal address, and the location is the last working address.
     * Page.location is the last working address, and page.url is the permanent internal address
     */
    val url: String,
    /**
     * Underlying persistent object
     */
    val page: GWebPage
) : Comparable<KWebPage> {

    constructor(legacyPage: WebPage): this(legacyPage.url, legacyPage.unbox())

    /**
     * The process scope WebPage instance sequence
     */
    var id = sequencer.incrementAndGet()
        private set

    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    val reversedUrl: String get() = UrlUtils.reverseUrl(url)
    /**
     * Web page scope configuration
     */
    var volatileConfig: VolatileConfig? = null

    /********************************************************************************
     * Common fields
     */
    /**
     * Web page scope variables
     * TODO : we may use it a PageDatum to track all context scope variables
     */
    val variables = Variables()

    val key: String get() = reversedUrl?:""

    val isNil: Boolean get() = this === NIL

    val isNotNil: Boolean get() = !isNil

    val isInternal: Boolean get() = hasMark(Mark.INTERNAL)

    val isNotInternal: Boolean get() = !isInternal

    fun unbox() = page

    val legacyWebPage = WebPage.box(url, page, volatileConfig!!)

    fun hasVar(name: String) = variables.contains(name)

    fun getAndRemoveVar(name: String): Boolean {
        val exist = variables.contains(name)
        if (exist) {
            variables.remove(name)
        }
        return exist
    }

    val metadata get() = Metadata.box(page.metadata)

    /********************************************************************************
     * Creation fields
     */
    val marks get() = CrawlMarks.box(page.markers)

    fun hasMark(mark: Mark) = page.markers[wrapKey(mark)] != null

    /**
     * All options are saved here, including crawl options, link options, entity options and so on
     */
    var args: String?
        get() = page.args.toString()
        set(value) {
            page.args = value
        }

    val configuredUrl get() = page.args?.let { "$url $it" }?:url

    var zoneId: ZoneId
        get() = if (page.zoneId == null) DateTimes.zoneId else ZoneId.of(page.zoneId.toString())
        set(zoneId) {
            page.zoneId = zoneId.id
        }

    var batchId: String?
        get() = page.batchId?.toString()
        set(value) {
            page.batchId = value
        }

    val isSeed: Boolean = metadata.contains(Name.IS_SEED)

    var distance: Int
        get() = page.distance.takeIf { it > 0 } ?: AppConstants.DISTANCE_INFINITE
        set(value) {
            page.distance = value
        }

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    var fetchMode: FetchMode
        get() = FetchMode.fromString(metadata[Name.FETCH_MODE])
        set(mode) {
            metadata[Name.FETCH_MODE] = mode.name
        }

    var lastBrowser: BrowserType
        get() = BrowserType.fromString(metadata[Name.BROWSER])
        set(browser) {
            metadata[Name.BROWSER] = browser.name
        }

    var htmlIntegrity: HtmlIntegrity
        get() = fromString(metadata[Name.HTML_INTEGRITY])
        set(integrity) {
            metadata[Name.HTML_INTEGRITY] = integrity.name
        }

    var fetchPriority: Int
        get() = if (page.fetchPriority > 0) page.fetchPriority else AppConstants.FETCH_PRIORITY_DEFAULT
        set(priority) {
            page.fetchPriority = priority
        }

    var createTime: Instant
        get() = Instant.ofEpochMilli(page.createTime)
        set(createTime) {
            page.createTime = createTime.toEpochMilli()
        }

    // Old version of generate time, created by String.valueOf(epochMillis)
    var generateTime: Instant
        get() {
            val time = metadata[Name.GENERATE_TIME]
            return when {
                time == null -> Instant.EPOCH
                NumberUtils.isDigits(time) -> Instant.ofEpochMilli(time.toLong())
                else -> Instant.parse(time)
            }
        }
        set(value) {
            metadata[Name.GENERATE_TIME] = value.toString()
        }

    /********************************************************************************
     * Fetch fields
     */
    var fetchCount: Int
        get() = page.fetchCount
        set(count) {
            page.fetchCount = count
        }

    var crawlStatus: CrawlStatus
        get() = CrawlStatus(page.crawlStatus.toByte())
        set(crawlStatus) {
            page.crawlStatus = crawlStatus.code
        }

    /**
     * Set crawl status
     *
     * @see CrawlStatus
     */
    fun setCrawlStatus(value: Int) {
        page.crawlStatus = value
    }

    /**
     * The baseUrl is as the same as Location
     *
     * A baseUrl has the same semantic with Jsoup.parse:
     * @link {https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse-java.io.File-java.lang.String-java.lang.String-}
     * @see [location]
     */
    var baseUrl: String
        get() = if (page.baseUrl == null) "" else page.baseUrl.toString()
        set(value) {
            page.baseUrl = value
        }

    /**
     * WebPage.url is the permanent internal address, it might not still available to access the target.
     * And WebPage.location or WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url, it's generally normalized.
     */
    val location: String get() = baseUrl

    var fetchTime: Instant
        get() = Instant.ofEpochMilli(page.fetchTime)
        set(value) { page.fetchTime = value.toEpochMilli() }

    var prevFetchTime: Instant
        get() = Instant.ofEpochMilli(page.prevFetchTime)
        set(value) { page.prevFetchTime = value.toEpochMilli() }

    /**
     * The previous crawl time for out pages
     * */
    var prevCrawlTime1: Instant
        get() = Instant.ofEpochMilli(page.prevFetchTime)
        set(value) { page.prevFetchTime = value.toEpochMilli() }

    var fetchInterval: Duration
        get() = Duration.ofSeconds(page.fetchInterval.toLong())
        set(value) { page.fetchInterval = value.seconds.toInt() }

    var protocolStatus: ProtocolStatus
        get() {
            val protocolStatus = page.protocolStatus?: GProtocolStatus.newBuilder().build()
            return ProtocolStatus.box(protocolStatus)
        }
        set(value) { page.protocolStatus = value.unbox() }

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
    val protocalHeaders get() = ProtocolHeaders.box(page.headers)

    var reprUrl: String?
        get() = page.reprUrl?.toString()
        set(value) { page.reprUrl = value }

    /**
     * Get the number of crawl scope retries
     * @see ai.platon.pulsar.persist.RetryScope
     */
    var fetchRetries: Int
        get() = page.fetchRetries
        set(value) { page.fetchRetries = value }

    var modifiedTime: Instant
        get() = Instant.ofEpochMilli(page.modifiedTime)
        set(value) { page.modifiedTime = value.toEpochMilli() }

    var prevModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.prevModifiedTime)
        set(value) {
            page.prevModifiedTime = value.toEpochMilli()
        }

    fun getFetchTimeHistory(defaultValue: String): String {
        val s = metadata[Name.FETCH_TIME_HISTORY]
        return s ?: defaultValue
    }

    /********************************************************************************
     * Parsing
     */
    var pageCategory: PageCategory?
        get() = page.pageCategory?.let { PageCategory.parse(page.pageCategory.toString()) }
        set(value) { page.pageCategory = value?.name }

    fun getEncoding(): String? {
        return if (page.encoding == null) null else page.encoding.toString()
    }

    fun setEncoding(encoding: String) {
        page.encoding = encoding
        metadata[Name.CHAR_ENCODING_FOR_CONVERSION] = encoding
    }

    /**
     * Get content encoding.
     * Content encoding is detected just before it's parsed
     */
    fun getEncodingOrDefault(defaultEncoding: String): String {
        return if (page.encoding == null) defaultEncoding else page.encoding.toString()
    }

    fun getEncodingClues(): String {
        return metadata.getOrDefault(Name.ENCODING_CLUES, "")
    }

    fun setEncodingClues(clues: String) {
        metadata[Name.ENCODING_CLUES] = clues
    }

    var contentType: String?
        get() = page.contentType?.toString()
        set(value) { page.contentType = value }

    fun hasContent(): Boolean {
        return page.content != null
    }

    /**
     * The entire raw document content e.g. raw XHTML
     */
    var content: ByteBuffer?
        get() = page.content
        set(value) {
            page.content = value
            setContentBytes(value?.array()?.size?:0)
        }

    fun setContent(value: String) {
        setContent(value.toByteArray())
    }

    fun setContent(value: ByteArray?) {
        if (value != null) {
            page.content = ByteBuffer.wrap(value)
            setContentBytes(value.size)
        } else {
            page.content = null
            setContentBytes(0)
        }
    }

    fun getContentBytes(): Int {
        return metadata.getInt(Name.CONTENT_BYTES, 0)
    }

    private fun setContentBytes(bytes: Int) {
        if (bytes == 0) {
            return
        }
        metadata[Name.CONTENT_BYTES] = bytes.toString()
        val count = fetchCount
        val lastAveBytes = metadata.getInt(Name.AVE_CONTENT_BYTES, 0)
        val aveBytes: Int
        aveBytes = if (count > 0 && lastAveBytes == 0) { // old version, average bytes is not calculated
            bytes
        } else {
            (lastAveBytes * count + bytes) / (count + 1)
        }
        metadata[Name.AVE_CONTENT_BYTES] = aveBytes.toString()
    }

    fun getAveContentBytes(): Int {
        return metadata.getInt(Name.AVE_CONTENT_BYTES, 0)
    }

    var proxy: String?
        get() = metadata[Name.PROXY]
        set(value) {
            metadata[Name.PROXY] = proxy
        }

    fun getActiveDomMultiStatus(): ActiveDOMStatTrace? { // cached
        val name = Name.ACTIVE_DOM_MULTI_STATUS
        val value = variables[name]
        if (value is ActiveDOMStatTrace) {
            return value
        } else {
            val json = metadata[name]
            if (json != null) {
                val status = ActiveDOMStatTrace.fromJson(json)
                variables[name] = status
                return status
            }
        }
        return null
    }

    fun setActiveDomMultiStatus(domStatus: ActiveDOMStatTrace?) {
        if (domStatus != null) {
            variables[Name.ACTIVE_DOM_MULTI_STATUS] = domStatus
            metadata[Name.ACTIVE_DOM_MULTI_STATUS] = domStatus.toJson()
        }
    }

    fun getActiveDomUrls(): ActiveDOMUrls? { // cached
        val name = Name.ACTIVE_DOM_URLS
        val value = variables[name]
        if (value is ActiveDOMUrls) {
            return value
        } else {
            val json = metadata[name]
            if (json != null) {
                val status = ActiveDOMUrls.fromJson(json)
                variables[name] = status
                return status
            }
        }
        return null
    }

    fun setActiveDomUrls(urls: ActiveDOMUrls?) {
        if (urls != null) {
            variables[Name.ACTIVE_DOM_URLS] = urls
            metadata[Name.ACTIVE_DOM_URLS] = urls.toJson()
        }
    }

    /**
     * An implementation of a WebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the WebPage's fingerprint representing its state for any point in time.
     */
    var signature: ByteBuffer?
        get() = page.signature
        set(value) { page.signature = value }

    fun setSignature(value: ByteArray?) {
        page.signature = ByteBuffer.wrap(value)
    }

    val signatureAsString get() = signature?.let { Strings.toHexString(signature) }?:""

    var prevSignature: ByteBuffer?
        get() = page.prevSignature
        set(value) { page.prevSignature = value }

    val prevSignatureAsString get() = prevSignature?.let { Strings.toHexString(prevSignature) }?:""

    var pageTitle: String?
        get() = page.pageTitle?.toString()
        set(value) { page.pageTitle = value }

    var contentTitle: String?
        get() = page.contentTitle?.toString()
        set(value) { page.contentTitle = value }

    var pageText: String
        get() = page.pageText?.toString() ?: ""
        set(value) { page.pageText = value }

    var contentText: String
        get() = page.contentText?.toString() ?: ""
        set(value) {
            page.contentText = value
            page.contentTextLen = value.length
        }

    val contentTextLen: Int
        get() = page.contentTextLen

    /**
     * Set all text fields cascaded, including content, content text and page text.
     */
    fun setTextCascaded(text: String) {
        setContent(text)
        contentText = text
        pageText = text
    }

    var parseStatus: ParseStatus
        get() = ParseStatus.box(page.parseStatus ?: GParseStatus.newBuilder().build())
        set(value) {
            page.parseStatus = value.unbox()
        }

    fun getSimpleLiveLinks() = page.liveLinks.keys.map { it.toString() }

    /**
     * TODO: Remove redundant url to reduce space
     */
    fun setLiveLinks(liveLinks: Iterable<HyperlinkPersistable>) {
        page.liveLinks.clear()
        val links = page.liveLinks
        liveLinks.forEach { l -> links[l.url] = l.unbox() }
    }

    fun addLiveLink(hyperlink: HyperlinkPersistable) {
        page.liveLinks[hyperlink.url] = hyperlink.unbox()
    }

    fun getSimpleVividLinks(): Collection<String> {
        return CollectionUtils.collect(page.vividLinks.keys) { obj: CharSequence -> obj.toString() }
    }

    var liveLinks: Map<CharSequence, GHypeLink>
        get() = page.liveLinks
        set(value) {
            page.liveLinks = value
        }

    var vividLinks: Map<CharSequence, CharSequence>
        get() = page.vividLinks
        set(value) {
            page.vividLinks = value
        }

    var deadLinks: MutableList<CharSequence>
        get() = page.deadLinks
        set(value) { page.deadLinks = value }

    var links: MutableList<CharSequence>
        get() = page.links
        set(value) { page.links = value }

    var impreciseLinkCount: Int
        get() = metadata.get(Name.TOTAL_OUT_LINKS)?.toIntOrNull() ?: 0
        set(value) { metadata[Name.TOTAL_OUT_LINKS] = value.toString() }

    var lnlinks: Map<CharSequence, CharSequence>
        get() = page.inlinks
        set(value) { page.inlinks = value }

    var anchor: String
        get() = page.anchor?.toString()?:""
        set(value) { page.anchor = value }

    var inlinkAnchors: List<String>
        get() = metadata.get(Name.ANCHORS)?.split("\n") ?: listOf()
        set(value) {
            metadata.set(Name.ANCHORS, value.joinToString("\n"))
        }

    var anchorOrder: Int
        get() = if (page.anchorOrder < 0) AppConstants.MAX_LIVE_LINK_PER_PAGE else page.anchorOrder
        set(value) { page.anchorOrder = value }

    var contentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.contentPublishTime)
        set(value) { page.contentPublishTime = value.toEpochMilli() }

    var prevContentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.prevContentPublishTime)
        set(value) { page.prevContentPublishTime = value.toEpochMilli() }

    var refContentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.refContentPublishTime)
        set(value) { page.refContentPublishTime = value.toEpochMilli() }

    var contentModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.contentModifiedTime)
        set(value) { page.contentModifiedTime = value.toEpochMilli() }

    var prevContentModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.prevContentModifiedTime)
        set(value) { page.prevContentModifiedTime = value.toEpochMilli() }

    var prevRefContentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.prevRefContentPublishTime)
        set(value) { page.prevRefContentPublishTime = value.toEpochMilli() }

    var referrer: String?
        get() = page.referrer?.toString()
        set(value) { page.referrer = value }

    /********************************************************************************
     * Page Model
     */
    val pageModel get() = PageModel.box(page.pageModel)

    /********************************************************************************
     * Scoring
     */
    var score: Float
        get() = page.score?:0.0f
        set(value) {
            page.score = value
        }

    var contentScore: Float
        get() = page.contentScore?:0.0f
        set(value) {
            page.contentScore = value
        }

    var sortScore: String?
        get() = page.sortScore?.toString()?:""
        set(value) {
            page.sortScore = value
        }

    var cash: Float
        get() = metadata.getFloat(Name.CASH_KEY, 0.0f)
        set(value) {
            metadata[Name.CASH_KEY] = value.toString()
        }

    val pageCounters: PageCounters get() = PageCounters.box(page.pageCounters)

    /********************************************************************************
     * Index
     */
    fun getIndexTimeHistory(defaultValue: String) = metadata[Name.INDEX_TIME_HISTORY] ?: defaultValue

    override fun hashCode() = url.hashCode()

    override fun compareTo(other: KWebPage) = url.compareTo(other.url)

    override fun equals(other: Any?) = other is KWebPage && other.url == url

    override fun toString() = url

    companion object {
        private val sequencer = AtomicInteger()
        val NIL = newInternalPage(AppConstants.NIL_PAGE_URL, 0, "NIL", "")

        fun newWebPage(originalUrl: String) = newWebPage(originalUrl, false)

        fun newWebPage(originalUrl: String, volatileConfig: VolatileConfig) =
                newWebPageInternal(originalUrl, volatileConfig)

        fun newWebPage(originalUrl: String, shortenKey: Boolean): KWebPage {
            val url = if (shortenKey) normalize(originalUrl, shortenKey) else originalUrl
            return newWebPageInternal(url, null)
        }

        fun newWebPage(originalUrl: String, shortenKey: Boolean, volatileConfig: VolatileConfig): KWebPage {
            val url = if (shortenKey) normalize(originalUrl, shortenKey) else originalUrl
            return newWebPageInternal(url, volatileConfig)
        }

        private fun newWebPageInternal(url: String, volatileConfig: VolatileConfig?): KWebPage {
            val page = KWebPage(url, GWebPage.newBuilder().build())
            page.baseUrl = url
            page.volatileConfig = volatileConfig
            page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
            page.createTime = Instant.now()
            page.score = 0f
            page.fetchCount = 0
            return page
        }

        @JvmOverloads
        fun newInternalPage(url: String, title: String = "internal", content: String = "internal"): KWebPage {
            return newInternalPage(url, -1, title, content)
        }

        fun newInternalPage(url: String, id: Int, title: String, content: String): KWebPage {
            val page = newWebPage(url, false)
            if (id >= 0) {
                page.id = id
            }
            page.baseUrl = url
            page.modifiedTime = Instant.now()
            page.fetchTime = Instant.parse("3000-01-01T00:00:00Z")
            page.fetchInterval = ChronoUnit.DECADES.duration
            page.fetchPriority = AppConstants.FETCH_PRIORITY_MIN
            page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
            page.distance = AppConstants.DISTANCE_INFINITE // or -1?
            page.marks.put(Mark.INTERNAL, AppConstants.YES_STRING)
            page.marks.put(Mark.INACTIVE, AppConstants.YES_STRING)
            page.pageTitle = title
            page.setContent(content)
            return page
        }

        /**
         * Initialize a WebPage with the underlying GWebPage instance.
         */
        fun box(url: String, page: GWebPage) = KWebPage(url, page)

        /********************************************************************************
         * Other
         */
        fun wrapKey(mark: Mark) = u8(mark.value())

        /**
         * What's the difference between String and Utf8?
         */
        fun u8(value: String) = Utf8(value)
    }
}
