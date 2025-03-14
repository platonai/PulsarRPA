package ai.platon.pulsar.persist.impl

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.DateTimes.doomsday
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.config.VolatileConfig.Companion.UNSAFE
import ai.platon.pulsar.common.urls.UrlUtils.mergeUrlArgs
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrlOrEmpty
import ai.platon.pulsar.common.urls.UrlUtils.unreverseUrl
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.gora.generated.GPageModel
import ai.platon.pulsar.persist.gora.generated.GParseStatus
import ai.platon.pulsar.persist.gora.generated.GProtocolStatus
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.*
import ai.platon.pulsar.persist.model.ActiveDOMStat
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import ai.platon.pulsar.persist.model.Converters.convert
import ai.platon.pulsar.persist.model.PageModel
import ai.platon.pulsar.persist.model.PageModel.Companion.box
import ai.platon.pulsar.persist.model.WebPageFormatter
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
import java.util.function.Function
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * The core web page structure
 */
class GoraBackendWebPage(
    /**
     * The url is the permanent internal address, while the location is the last working address.
     */
    override val url: String,
    /**
     * A webpage scope configuration, any modifications made to it will exclusively impact this particular webpage.
     */
    override var conf: VolatileConfig,
    /**
     * The underlying persistent object.
     */
    private var page: GWebPage
) : WebPage {
    companion object {
        private val SEQUENCER = AtomicInteger(0)

        val NIL = newInternalPage(AppConstants.NIL_PAGE_URL, 0, "nil", "nil")

        fun newWebPage(url: String, conf: VolatileConfig): GoraBackendWebPage {
            return newWebPage(url, conf, null)
        }

        fun newWebPage(url: String, conf: VolatileConfig, href: String?): GoraBackendWebPage {
            return newWebPageInternal(url, conf, href)
        }

        fun newInternalPage(url: String): GoraBackendWebPage {
            return newInternalPage(url, "internal", "internal")
        }

        fun newInternalPage(url: String, title: String): GoraBackendWebPage {
            return newInternalPage(url, title, "internal")
        }

        fun newInternalPage(url: String, title: String, content: String): GoraBackendWebPage {
            return newInternalPage(url, -1, title, content)
        }

        fun newInternalPage(url: String, id: Int, title: String, content: String): GoraBackendWebPage {
            val unsafe = UNSAFE
            val page = newWebPage(url, unsafe)
            if (id >= 0) {
                page.id = id
            }

            page.location = url
            page.modifiedTime = Instant.EPOCH
            page.prevFetchTime = Instant.EPOCH
            page.fetchTime = doomsday
            page.fetchInterval = ChronoUnit.CENTURIES.duration
            page.fetchPriority = AppConstants.FETCH_PRIORITY_MIN
            page.crawlStatus = CrawlStatus.STATUS_UNFETCHED

            page.distance = AppConstants.DISTANCE_INFINITE // or -1?
            page.marks.put(Mark.INTERNAL, AppConstants.YES_STRING)
            page.marks.put(Mark.INACTIVE, AppConstants.YES_STRING)

            page.pageTitle = title
            page.setStringContent(content)

            return page
        }

        /**
         * Initialize a WebPage with the underlying GWebPage instance.
         */
        fun box(url: String, page: GWebPage, conf: VolatileConfig): GoraBackendWebPage {
            return GoraBackendWebPage(url, conf, page)
        }

        private fun newWebPageInternal(url: String, conf: VolatileConfig, href: String?): GoraBackendWebPage {
            val page = GoraBackendWebPage(url, GWebPage.newBuilder().build(), false, conf)

            page.location = url
            page.conf = conf
            page.href = href
            page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
            page.createTime = Instant.now()
            page.modifiedTime = Instant.now()
            page.fetchCount = 0

            return page
        }

        private fun getUrl(urlOrKey: String, urlReversed: Boolean): String {
            return if (urlReversed) unreverseUrl(urlOrKey) else urlOrKey
        }
    }

    /**
     * A process scope page id.
     */
    /**
     * The page id which is unique in process scope.
     */
    override var id: Int = SEQUENCER.incrementAndGet()
        private set

    override val reversedUrl get() = reverseUrlOrEmpty(url)

    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this webpage.
     * It's faster to retrieve the page by the reversed url.
     */
    override val key: String get() = reversedUrl

    /**
     * Web page scope variables
     */
    override val variables: Variables = Variables()

    /**
     * Store arbitrary data associated with the webpage.
     */
    private val data = Variables()

    /**
     * The page datum for update.
     * Page datum is collected during the fetch phrase and is used to update the page in the update phase.
     */
    override var pageDatum: PageDatum? = null

    /**
     * If this page is fetched from Internet
     */
    override var isCached: Boolean = false

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    override var isLoaded: Boolean = false

    /**
     * If this page is fetched from Internet
     */
    override var isFetched: Boolean = false

    /**
     * Check if the page is canceled.
     *
     *
     * If a page is canceled, it should not be updated.
     */
    /**
     * Check if the page is canceled.
     *
     *
     * If a page is canceled, it should not be updated.
     */
    /**
     * If this page is canceled
     */
    override var isCanceled: Boolean = false

    /**
     * If this page is fetched and updated
     */
    @Volatile
    override var isContentUpdated: Boolean = false
        private set

    /**
     * Get the cached content
     */
    /**
     * Set the cached content, keep the persisted page content unmodified
     */
    /**
     * The cached content.
     * TODO: use a loading cache for all cached page contents.
     */
    @Volatile
    override var tmpContent: ByteBuffer? = null

    /**
     * The delay time to retry if a retry is needed
     */
    override var retryDelay: Duration = Duration.ZERO

    /**
     * The field loader to load fields lazily.
     */
    private var lazyFieldLoader: Function<String, GWebPage>? = null

    private val lazyLoadedFields: MutableList<String> = ArrayList()

    private val CONTENT_MONITOR = Any()
    private val PAGE_MODEL_MONITOR = Any()

    //    private final Deque<String> lazyLoadedFields = new ConcurrentLinkedDeque<>();
    private constructor(
        urlOrKey: String, page: GWebPage, urlReversed: Boolean, conf: VolatileConfig
    ) : this(getUrl(urlOrKey, urlReversed), conf, page)

    private constructor(
        url: String, reversedUrl: String, page: GWebPage, conf: VolatileConfig
    ) : this(url, page = page, conf = conf)

    override var href: String?
        /**
         * Get The hypertext reference of this page.
         * It defines the address of the document, which this time is linked from
         *
         *
         * TODO: use a separate field for href
         *
         * @return The hypertext reference
         */
        get() = metadata[Name.HREF]
        /**
         * Set The hypertext reference of this page.
         * It defines the address of the document, which this time is linked from
         *
         * @param href The hypertext reference
         */
        set(href) {
            metadata[Name.HREF] = href
        }

    override val isNil: Boolean
        get() = this === NIL

    override val isNotNil: Boolean
        get() = !isNil

    override val isInternal: Boolean
        get() = hasMark(Mark.INTERNAL)

    override val isNotInternal: Boolean
        get() = !isInternal

    override fun unbox(): GWebPage {
        return page
    }

    override fun unsafeSetGPage(page: GWebPage) {
        this.page = page
    }

    override fun unsafeCloneGPage(page: WebPage) {
        unsafeSetGPage(GWebPage.newBuilder(page.unbox()).build())
    }

    /**
     * Check if the page scope temporary variable with `name` exists
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    override fun hasVar(name: String): Boolean {
        return variables.contains(name)
    }

    /**
     * Returns the page scope temporary variable to which the specified name is mapped,
     * or `null` if the local variable map contains no mapping for the name.
     *
     * @param name the name whose associated value is to be returned
     * @return the value to which the specified name is mapped, or
     * `null` if the local variable map contains no mapping for the key
     */
    override fun getVar(name: String): Any? {
        return variables[name]
    }

    override fun getVar(clazz: Class<*>): Any? {
        return getVar(clazz::class.java.name)
    }

    /**
     * Retrieves and removes the local variable with the given name.
     */
    override fun removeVar(name: String): Any? {
        return variables.remove(name)
    }

    /**
     * Set a page scope temporary variable.
     *
     * @param name  The variable name.
     * @param value The variable value.
     */
    override fun setVar(name: String, value: Any) {
        variables[name] = value
    }

    /**
     * Returns the bean to which the specified class is mapped,
     * or `null` if the local bean map contains no mapping for the class.
     *
     * @param clazz the class of the variable
     */
    override fun getBean(clazz: Class<*>): Any {
        val bean = getBeanOrNull(clazz) ?: throw NoSuchElementException("No bean found for class $clazz in WebPage")
        return bean
    }

    /**
     * Returns the data to which the specified class is mapped,
     * or `null` if the local bean map contains no mapping for the class.
     *
     * @param clazz the class of the variable
     */
    override fun getBeanOrNull(clazz: Class<*>): Any? {
        return variables[clazz.name]
    }

    /**
     * Set a page scope temporary java bean.
     */
    override fun <T> putBean(bean: T) {
        variables.set(bean!!::class.java.name, bean)
    }

    /**
     * Returns the data to which the specified name is mapped,
     * or `null` if the data map contains no mapping for the name.
     *
     * @param name the name whose associated value is to be returned
     * @return the value to which the specified name is mapped, or
     * `null` if the local variable map contains no mapping for the key
     */
    override fun data(name: String): Any? {
        return data[name]
    }

    /**
     * Store arbitrary data associated with the webpage.
     *
     * @param name  A string naming the piece of data to set.
     * @param value The new data value.
     */
    override fun data(name: String, value: Any?) {
        if (value == null) {
            data.remove(name)
        } else {
            data[name] = value
        }
    }

    override val metadata: Metadata
        get() = Metadata.box(page.metadata)

    override val marks: CrawlMarks
        /**
         * CrawlMarks are used for nutch style crawling.
         */
        get() = CrawlMarks.box(page.markers)

    /**
     * Check if a mark is marked.
     *
     *
     * CrawlMarks are used for nutch style crawling.
     */
    override fun hasMark(mark: Mark): Boolean {
        return page.markers[PersistUtils.wrapKey(mark)] != null
    }

    override var args: String
        /**
         * The load arguments is variant task by task, so the local version is the first choice,
         * while the persisted version is used for historical check only
         *
         * Underlying gora field should not use name 'args' which is already used,
         * see GProtocolStatus.args and GParseStatus.args
         */
        get() {
            // Underlying gora field should not use name 'args' which is already used.
            return page.params?.toString() ?: ""
        }
        /**
         * Set the arguments and clear the LoadOptions object.
         */
        set(args) {
            variables.remove(PulsarParams.VAR_LOAD_OPTIONS)
            page.params = args
        }

    /**
     * Set a field loader, the loader takes a parameter as the field name,
     * and returns a GWebPage containing the field.
     */
    override fun setLazyFieldLoader(lazyFieldLoader: Function<String, GWebPage>) {
        this.lazyFieldLoader = lazyFieldLoader
    }

    override var maxRetries: Int
        get() = metadata.getInt(Name.FETCH_MAX_RETRY, 3)
        set(maxRetries) {
            metadata[Name.FETCH_MAX_RETRY] = maxRetries
        }

    override val configuredUrl: String
        get() = mergeUrlArgs(url, args)

    override var fetchedLinkCount: Int
        get() = metadata.getInt(Name.FETCHED_LINK_COUNT, 0)
        set(count) {
            metadata[Name.FETCHED_LINK_COUNT] = count
        }

    override var zoneId: ZoneId
        get() = if (page.zoneId == null) DateTimes.zoneId else ZoneId.of(page.zoneId.toString())
        set(zoneId) {
            page.zoneId = zoneId.id
        }

    override var batchId: String?
        get() = if (page.batchId == null) null else page.batchId.toString()
        set(value) {
            page.batchId = value
        }

    /**
     * Mark this page as a seed where a crawl job starts from.
     */
    override fun markSeed() {
        metadata[Name.IS_SEED] = AppConstants.YES_STRING
    }

    /**
     * Unmark this page to be a seed.
     */
    override fun unmarkSeed() {
        metadata.remove(Name.IS_SEED)
    }

    override val isSeed: Boolean
        /**
         * Check whether this page is a seed.
         */
        get() = metadata.contains(Name.IS_SEED)

    override var distance: Int
        /**
         * Get the distance of the page from the seed in the graph.
         */
        get() {
            val distance = page.distance
            return if (distance < 0) AppConstants.DISTANCE_INFINITE else distance
        }
        /**
         * Set the distance of the page from the seed in the graph.
         */
        set(newDistance) {
            page.distance = newDistance
        }

    override var fetchMode: FetchMode
        /**
         * Get the fetch mode, only BROWSER mode is supported currently.
         * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch.
         */
        get() = FetchMode.fromString(metadata[Name.FETCH_MODE])
        /**
         * Get the fetch mode, only BROWSER mode is supported currently.
         * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
         */
        set(mode) {
            metadata[Name.FETCH_MODE] = mode.name
        }

    override var lastBrowser: BrowserType
        /**
         * Get the browser used to fetch the page last time.
         */
        get() {
            val browser = if (page.browser != null) page.browser.toString() else ""
            return BrowserType.fromString(browser)
        }
        /**
         * Set the browser used to fetch the page.
         */
        set(browser) {
            page.browser = browser.name
        }

    override var isResource: Boolean
        /**
         * Checks whether the page is a single resource which can be fetched by a single request.
         */
        get() = page.resource != null
        /**
         * Indicates the page to be a single resource that can be fetched by a single request.
         */
        set(resource) {
            if (resource) {
                page.resource = 1
            }
        }

    override var htmlIntegrity: HtmlIntegrity
        get() {
            val integrity =
                if (page.htmlIntegrity != null) page.htmlIntegrity.toString() else ""
            return HtmlIntegrity.fromString(integrity)
        }
        set(integrity) {
            page.htmlIntegrity = integrity.name
        }

    override var fetchPriority: Int
        get() = if (page.fetchPriority > 0) page.fetchPriority else AppConstants.FETCH_PRIORITY_DEFAULT
        set(priority) {
            page.fetchPriority = priority
        }

    fun sniffFetchPriority(): Int {
        var priority = fetchPriority

        val depth = distance
        if (depth < AppConstants.FETCH_PRIORITY_DEPTH_BASE) {
            priority = max(priority.toDouble(), (AppConstants.FETCH_PRIORITY_DEPTH_BASE - depth).toDouble()).toInt()
        }

        return priority
    }

    override var createTime: Instant
        get() = Instant.ofEpochMilli(page.createTime)
        set(createTime) {
            page.createTime = createTime.toEpochMilli()
        }

    override var generateTime: Instant
        get() {
            val generateTime = metadata[Name.GENERATE_TIME]
            return if (generateTime == null) {
                Instant.EPOCH
            } else {
                Instant.parse(generateTime)
            }
        }
        set(generateTime) {
            metadata[Name.GENERATE_TIME] = generateTime.toString()
        }

    override var fetchCount: Int
        get() = page.fetchCount
        set(count) {
            page.fetchCount = count
        }

    override fun updateFetchCount() {
        val count = fetchCount
        fetchCount = count + 1
    }

    override var crawlStatus: CrawlStatus
        get() = CrawlStatus(page.crawlStatus.toByte())
        set(crawlStatus) {
            page.crawlStatus = crawlStatus.code
        }

    override fun setCrawlStatus(value: Int) {
        page.crawlStatus = value
    }

    override var baseUrl: String
        get() = if (page.baseUrl == null) "" else page.baseUrl.toString()
        set(value) {
            page.baseUrl = value
        }

    override var location: String
        get() = baseUrl
        set(location) {
            page.baseUrl = location
        }

    override var fetchTime: Instant
        get() = Instant.ofEpochMilli(page.fetchTime)
        set(fetchTime) {
            page.fetchTime = fetchTime.toEpochMilli()
        }

    override var prevFetchTime: Instant
        get() = Instant.ofEpochMilli(page.prevFetchTime)
        set(prevFetchTime) {
            page.prevFetchTime = prevFetchTime.toEpochMilli()
        }

    override var prevCrawlTime1: Instant
        get() = Instant.ofEpochMilli(page.prevCrawlTime1)
        set(prevCrawlTime1) {
            page.prevCrawlTime1 = prevCrawlTime1.toEpochMilli()
        }

    override var fetchInterval: Duration
        get() = Duration.ofSeconds(fetchIntervalSeconds)
        set(fetchInterval) {
            page.fetchInterval = fetchInterval.seconds.toInt()
        }

    private val fetchIntervalSeconds: Long
        get() {
            var seconds = page.fetchInterval.toLong()
            if (seconds < 0) {
                seconds = ChronoUnit.CENTURIES.duration.seconds
            }
            return seconds
        }

    override var protocolStatus: ProtocolStatus
        get() = getProtocolStatus0()
        set(protocolStatus) {
            page.protocolStatus = protocolStatus.unbox()
        }

    private fun getProtocolStatus0(): ProtocolStatus {
        var protocolStatus = page.protocolStatus
        if (protocolStatus == null) {
            protocolStatus = GProtocolStatus.newBuilder().build()
        }
        return ProtocolStatus.box(protocolStatus)
    }

    override val headers: ProtocolHeaders
        get() {
            val headers = page.headers
            return ProtocolHeaders.box(headers)
        }

    override var reprUrl: String
        get() = if (page.reprUrl == null) "" else page.reprUrl.toString()
        set(reprUrl) {
            page.reprUrl = reprUrl
        }

    override var fetchRetries: Int
        get() = page.fetchRetries
        set(fetchRetries) {
            page.fetchRetries = fetchRetries
        }

    override var modifiedTime: Instant
        get() = Instant.ofEpochMilli(page.modifiedTime)
        set(modifiedTime) {
            page.modifiedTime = modifiedTime.toEpochMilli()
        }

    override var prevModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.prevModifiedTime)
        set(prevModifiedTime) {
            page.prevModifiedTime = prevModifiedTime.toEpochMilli()
        }

    override var pageCategory: PageCategory
        get() = getPageCategory0()
        set(pageCategory) {
            page.pageCategory = pageCategory.format()
        }

    override val openPageCategory: OpenPageCategory
        get() {
            try {
                val pageCategory = page.pageCategory
                if (pageCategory != null) {
                    return OpenPageCategory.parse(pageCategory.toString())
                }
            } catch (ignored: Throwable) {
            }

            return OpenPageCategory("", "")
        }

    override fun setPageCategory(pageCategory: OpenPageCategory) {
        page.pageCategory = pageCategory.format()
    }

    override var encoding: String?
        get() = page.encoding?.toString()
        set(value) {
            page.encoding = value
        }

    override var content: ByteBuffer?
        get() = getContent0()
        set(value) {
            setByteBufferContent1(value)
        }

    override val persistContent: ByteBuffer?
        get() = getPersistContent0()

    override val contentAsBytes get() = getContentAsBytes0()

    override val contentAsString get() = getContentAsString0()

    override val contentAsInputStream get() = getContentAsInputStream0()

    override val contentAsSaxInputSource get() = getContentAsSaxInputSource0()

    override fun setStringContent(value: String?) {
        if (value != null) {
            setByteArrayContent(value.toByteArray())
        } else {
            setByteBufferContent1((null as ByteBuffer?))
        }
    }

    override fun setByteArrayContent(value: ByteArray?) {
        setByteArrayContent1(value)
    }

    override fun setByteBufferContent(value: ByteBuffer?) {
        setByteBufferContent1(value)
    }

    /**
     * Clear persist content, so the content will not write to the disk.
     */
    override fun clearPersistContent() {
        synchronized(CONTENT_MONITOR) {
            tmpContent = page.content
            page.content = null
            persistedContentLength = 0
        }
    }

    /**
     * Get the length of content in bytes.
     *
     * @return The length of the content in bytes.
     */
    override val contentLength: Long
        get() = when {
            page.contentLength != null -> page.contentLength
            else -> 0
        }

    /**
     * The length of the original page content in bytes, the content has no pulsar metadata inserted.
     *
     * @return The length of the original page content in bytes, negative means not specified
     */
    override var originalContentLength: Long
        get() = metadata.getLong(Name.ORIGINAL_CONTENT_LENGTH, -1)
        set(value) {
            metadata[Name.ORIGINAL_CONTENT_LENGTH] = "" + value
        }

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
        val aveBytes = if (count > 0 && lastAveBytes == 0L) {
            // old version, average bytes is not calculated
            bytes
        } else {
            (lastAveBytes * count + bytes) / (count + 1)
        }

        page.aveContentLength = aveBytes
    }

    override var persistedContentLength: Long
        get() = when {
            page.persistedContentLength != null -> page.persistedContentLength
            else -> 0
        }
        set(value) {
            page.persistedContentLength = value
        }

    override val lastContentLength
        get() = when {
            page.lastContentLength != null -> page.lastContentLength
            else -> 0
        }

    override val aveContentLength
        get() = when {
            page.aveContentLength != null -> page.aveContentLength
            else -> 0
        }

    override var contentType
        get() = if (page.contentType == null) "" else page.contentType.toString()
        set(value) {
            page.contentType = value.trim().lowercase(Locale.getDefault())
        }

    override var signature: ByteBuffer? = null

    override val signatureAsString: String
        get() = getSignatureAsString0()

    override var prevSignature: ByteBuffer?
        get() = page.prevSignature
        set(value) {
            page.prevSignature = value
        }

    override val prevSignatureAsString: String
        get() = getPrevSignatureAsString0()

    override var proxy: String?
        get() = page.proxy?.toString()
        set(value) {
            page.proxy = value
        }

    override var activeDOMStatus: ActiveDOMStatus?
        get() = getActiveDOMStatus0()
        set(value) {
            setActiveDOMStatus0(value)
        }

    private fun getActiveDOMStatus0(): ActiveDOMStatus? {
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

    private fun setActiveDOMStatus0(s: ActiveDOMStatus?) {
        if (s == null) {
            return
        }

        val s2 = page.activeDOMStatus
        if (s2 != null) {
            s2.n = s.n
            s2.scroll = s.scroll
            s2.st = s.st
            s2.r = s.r
            s2.idl = s.idl
            s2.ec = s.ec
        }
    }

    override var activeDOMStatTrace: Map<String, ActiveDOMStat?>
        get() = getActiveDOMStatTrace0()
        set(value) {
            setActiveDOMStatTrace0(value)
        }

    private fun getActiveDOMStatTrace0(): Map<String, ActiveDOMStat> {
        val s = page.activeDOMStatTrace
        return s.entries.associate { it.key.toString() to convert(it.value) }
    }

    private fun setActiveDOMStatTrace0(trace: Map<String, ActiveDOMStat?>?) {
        if (trace == null) {
            page.activeDOMStatus = null
            return
        }

        page.activeDOMStatTrace = trace.filterValues { it != null }.entries
            .associate { it.key to convert(it.value!!) }
    }

    override var pageTitle
        get() = page.pageTitle?.toString()
        set(value) {
            page.pageTitle = value
        }

    override var contentTitle
        get() = page.contentTitle?.toString()
        set(value) {
            page.contentTitle = value
        }

    override var pageText
        get() = page.pageText?.toString()
        set(value) {
            page.pageText = value
        }

    override var contentText
        get() = page.contentText?.toString()
        set(value) {
            page.contentText = value
        }

    override var parseStatus: ParseStatus
        get() = ParseStatus.box(page.parseStatus ?: GParseStatus.newBuilder().build())
        set(value) {
            page.parseStatus = value.unbox()
        }

    override var vividLinks: MutableMap<CharSequence, CharSequence>
        get() = page.vividLinks.toMutableMap()
        set(value) {
            page.vividLinks = value
        }

    override var links: MutableList<CharSequence>
        get() = page.links.toMutableList()
        set(value) {
            page.links = value
        }

    override var inlinks: MutableMap<CharSequence, CharSequence>
        get() = page.inlinks.toMutableMap()
        set(value) {
            page.inlinks = value
        }

    override var anchor: CharSequence?
        get() = page.anchor?.toString()
        set(value) {
            page.anchor = value
        }

    override var referrer: String?
        get() = page.referrer?.toString()
        set(value) {
            page.referrer = value
        }

    override var pageModelUpdateTime: Instant?
        get() = page.pageModelUpdateTime?.let { Instant.ofEpochMilli(it) }
        set(value) {
            page.pageModelUpdateTime = value?.toEpochMilli()
        }

    override var pageModel: PageModel?
        get() = getPageModel0()
        set(value) {
            page.pageModel = value?.unbox()
        }

    private fun getPageModel0(): PageModel? {
        synchronized(PAGE_MODEL_MONITOR) {
            val fieldName = GWebPage.Field.PAGE_MODEL.getName()
            // load content lazily
            if (page.pageModel == null && lazyFieldLoader != null && !lazyLoadedFields.contains(fieldName)) {
                lazyLoadedFields.add(fieldName)
                val lazyPage = lazyFieldLoader!!.apply(fieldName)
                page.pageModel = lazyPage.pageModel
            }
            return if (page.pageModel == null) null else box(page.pageModel)
        }
    }

    override fun ensurePageModel(): PageModel {
        synchronized(PAGE_MODEL_MONITOR) {
            if (page.pageModel == null) {
                page.pageModel = GPageModel.newBuilder().build()
            }
            return Objects.requireNonNull(pageModel)!!
        }
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun compareTo(o: WebPage): Int {
        return url.compareTo(Objects.requireNonNull(o.url))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is WebPage && other.url == url
    }

    override fun toString(): String {
        return WebPageFormatter(this).format()
    }


    private fun getContent0(): ByteBuffer? {
        if (tmpContent != null) {
            return tmpContent
        }

        return persistContent
    }

    private fun getPersistContent0(): ByteBuffer? {
        synchronized(CONTENT_MONITOR) {
            val fieldName = GWebPage.Field.CONTENT.getName()
            // load content lazily
            if (page.content == null && lazyFieldLoader != null && !lazyLoadedFields.contains(fieldName)) {
                lazyLoadedFields.add(fieldName)
                val lazyPage = lazyFieldLoader!!.apply(fieldName)
                page.content = lazyPage.content
            }
            return page.content
        }
    }

    private fun getContentAsBytes0(): ByteArray {
        val content = content ?: return ByteUtils.toBytes('\u0000')
        return ByteUtils.toBytes(content)
    }

    private fun getContentAsString0(): String {
        val buffer = content
        if (buffer == null || buffer.remaining() == 0) {
            return ""
        }

        return String(buffer.array(), buffer.arrayOffset(), buffer.limit())
    }

    private fun getContentAsInputStream0(): ByteArrayInputStream {
        val contentInOctets = content ?: return ByteArrayInputStream(ByteUtils.toBytes('\u0000'))

        return ByteArrayInputStream(
            content!!.array(),
            contentInOctets.arrayOffset() + contentInOctets.position(),
            contentInOctets.remaining()
        )
    }

    private fun getContentAsSaxInputSource0(): InputSource {
        val inputSource = InputSource(contentAsInputStream)
        val encoding = encoding
        if (encoding != null) {
            inputSource.encoding = encoding
        }
        return inputSource
    }

    /**
     * Set the page content
     */
    private fun setByteArrayContent1(value: ByteArray?) {
        if (value != null) {
            setByteBufferContent1(ByteBuffer.wrap(value))
        } else {
            setByteBufferContent1((null as ByteBuffer?))
        }
    }

    private fun setByteBufferContent1(value: ByteBuffer?) {
        synchronized(CONTENT_MONITOR) {
            if (value != null) {
                page.content = value
                isContentUpdated = true

                var length = value.array().size.toLong()
                // save the length of the persisted content,
                // so we can query the length without loading the big or even huge content field
                persistedContentLength = length

                length = originalContentLength
                if (length <= 0) {
                    // TODO: it's for old version compatible
                    length = value.array().size.toLong()
                }
                computeContentLength(length)
            } else {
                clearPersistContent()
            }
        }
    }

    private fun getSignatureAsString0(): String {
        var sig = signature
        if (sig == null) {
            sig = ByteBuffer.wrap("".toByteArray())
        }
        return Strings.toHexString(sig)
    }

    private fun getPrevSignatureAsString0(): String {
        var sig: ByteBuffer? = prevSignature
        if (sig == null) {
            sig = ByteBuffer.wrap("".toByteArray())
        }
        return Strings.toHexString(sig)
    }

    private fun getPageCategory0(): PageCategory {
        try {
            val pageCategory = page.pageCategory
            if (pageCategory != null) {
                return PageCategory.parse(pageCategory.toString())
            }
        } catch (ignored: Throwable) {
        }

        return PageCategory.UNKNOWN
    }
}
