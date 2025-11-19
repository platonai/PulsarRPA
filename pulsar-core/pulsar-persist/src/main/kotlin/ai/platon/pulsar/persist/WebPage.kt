package ai.platon.pulsar.persist

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.persist.model.ActiveDOMMetadata
import ai.platon.pulsar.persist.model.ActiveDOMStat
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import ai.platon.pulsar.persist.model.PageModel
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.function.Function

/**
 * Represents a static web page in the Browser4 system. This interface provides methods to access and manipulate
 * various properties and metadata associated with a web page, such as its content, headers, fetch status,
 * and more. It also includes methods for managing beans, and other page-related data.
 *
 * This interface extends `Comparable<WebPage>` to allow for comparison between web pages.
 */
interface WebPage : Comparable<WebPage> {
    /**
     * The unique, in-process identifier of the web page.
     */
    val id: Long

    /**
     * The url is the permanent internal address, and it's also the storage key (reserved).
     * The url can differ from the original url passed by the user, because the original url might be normalized,
     * and the url also can differ from the final location of the page, because the page can be redirected in the browser.
     */
    val url: String

    /**
     * The key associated with the web page, typically used for indexing or identification purposes.
     * The key is defined as the reserved url, for example, the key for page TestResourceUtil.PRODUCT_DETAIL_URL
     * is "uk.co.amazon.www:https/dp/B0E000001".
     */
    val key: String

    /**
     * The href (hypertext reference) of the web page, the href should not be normalized and kept the original form
     * where it extracted from.
     * For example, the href can be extracted from an HTML page:
     * ```html
     * <a href='https://www.amazon.com/dp/B0E000001?th=1'>Huawei P60 ...</a>
     * ```
     */
    var href: String?

    /**
     * A baseUrl is used to resolve relative URLs.
     *
     * The base URL is determined as follows:
     * 1. By default, the base URL is the location of the document
     *    (as determined by window.location).
     * 2. If the document has an `<base>` element, its href attribute is used.
     * */
    var baseURI: String
    /**
     * Returns the document location as a string.
     *
     * [location] is the last working address,
     * it might redirect from the original url, or it might have additional query parameters.
     * [location] can differ from [url].
     *
     * In javascript, the documentURI property can be used on any document types. The document.URL
     * property can only be used on HTML documents.
     *
     * @see <a href='https://www.w3schools.com/jsref/prop_document_documenturi.asp'>
     *     HTML DOM Document documentURI</a>
     * */
    var location: String
    /**
     * The URL of the page that linked to this page.
     */
    var referrer: String?

    /**
     * The load arguments which can be parsed into a `LoadOptions` object.
     * It's usually used by `session.load()` method series.
     */
    var args: String

    /**
     * The configured URL of the web page, which is always a combination of `url` and `args`.
     */
    val configuredUrl: String

    /**
     * Indicates whether the web page is nil (i.e., not initialized or empty).
     */
    val isNil: Boolean

    /**
     * Indicates whether the web page is not nil (i.e., initialized and contains data).
     */
    val isNotNil: Boolean

    /**
     * Indicates whether the web page is internal (i.e., part of the same domain or system).
     */
    val isInternal: Boolean

    /**
     * Indicates whether the web page is not internal (i.e., external to the domain or system).
     */
    val isNotInternal: Boolean

    /**
     * The page scope configuration, which is expected to be modified frequently.
     */
    var conf: VolatileConfig

    /**
     * Indicates whether the web page is stored in the in-memory cache.
     */
    var isCached: Boolean

    /**
     * Indicates whether the web page is loaded from persistent storage.
     */
    var isLoaded: Boolean

    /**
     * Indicates whether the web page is fetched from the Internet.
     */
    var isFetched: Boolean

    /**
     * Indicates whether the web page is canceled by the user.
     */
    var isCanceled: Boolean

    /**
     * Indicates whether the content of the web page has been updated.
     */
    val isContentUpdated: Boolean

    /**
     * The delay time before the next retry to load the web page.
     */
    var retryDelay: Duration

    /**
     * The maximum number of retries to load the web page.
     */
    var maxRetries: Int

    /**
     * The time zone of the web page.
     */
    var zoneId: ZoneId

    /**
     * The distance of the web page from the root page.
     */
    var distance: Int

    /**
     * The fetch mode of the web page.
     */
    var fetchMode: FetchMode

    /**
     * The last browser used to fetch the web page.
     */
    var lastBrowser: BrowserType

    /**
     * Indicates whether the web page is a resource, which can be fetched using a single request.
     */
    var isResource: Boolean

    /**
     * The integrity of the web page, used to check if the page is valid.
     */
    var htmlIntegrity: HtmlIntegrity

    /**
     * The time when the web page was created.
     */
    var createTime: Instant

    /**
     * The number of times the web page has been fetched.
     */
    var fetchCount: Int

    /**
     * The time the page to be fetched.
     */
    var fetchTime: Instant

    /**
     * The number of times the web page has been retried to fetch.
     */
    var fetchRetries: Int

    /**
     * The previous time when the web page was fetched.
     */
    var prevFetchTime: Instant

    /**
     * The interval between the previous fetch and the current fetch.
     */
    var fetchInterval: Duration

    /**
     * The time when the web page was last modified.
     */
    var modifiedTime: Instant

    /**
     * The previous time when the web page was modified.
     */
    var prevModifiedTime: Instant

    /**
     * The protocol status of the web page, used to check if the page was fetched successfully.
     */
    var protocolStatus: ProtocolStatus

    /**
     * The metadata of the web page, which will be persisted to storage.
     */
    val metadata: Metadata

    /**
     * The headers of the web page, which will be persisted to storage.
     */
    val headers: ProtocolHeaders

    /**
     * The content category of the web page.
     */
    var pageCategory: OpenPageCategory

    /**
     * The encoding of the web page, which is UTF-8 by default.
     */
    var encoding: String?

    /**
     * The content type of the web page, which is `text/html` by default.
     */
    var contentType: String

    /**
     * The content of the web page, stored as a `ByteBuffer`.
     */
    val content: ByteBuffer?

    /**
     * The temporary content of the web page, stored as a `ByteBuffer`.
     */
    var tmpContent: ByteBuffer?

    /**
     * The content of the web page, stored as a `ByteBuffer` for persistence.
     */
    val persistContent: ByteBuffer?

    /**
     * The content of the web page, stored as a `ByteArray`.
     */
    val contentAsBytes: ByteArray

    /**
     * The content of the web page, stored as a `String`.
     */
    val contentAsString: String

    /**
     * The content of the web page, stored as a `ByteArrayInputStream`.
     */
    val contentAsInputStream: ByteArrayInputStream

    /**
     * The content of the web page, stored as an `InputSource` for SAX parsing.
     */
    val contentAsSaxInputSource: InputSource?

    /**
     * The length of the content of the web page.
     */
    val contentLength: Long

    /**
     * The original length of the content of the web page.
     */
    var originalContentLength: Long

    /**
     * The length of the persisted content of the web page.
     */
    var persistedContentLength: Long

    /**
     * The length of the last version of the content of the web page.
     */
    val lastContentLength: Long

    /**
     * The average length of all versions of the content of the web page.
     */
    val aveContentLength: Long

    /**
     * The previous signature of the web page, stored as a `ByteBuffer`.
     */
    var prevSignature: ByteBuffer?

    /**
     * The previous signature of the web page, stored as a `String`.
     */
    val prevSignatureAsString: String

    /**
     * The current signature of the web page, stored as a `ByteBuffer`.
     */
    val signature: ByteBuffer?

    /**
     * The current signature of the web page, stored as a `String`.
     */
    val signatureAsString: String

    /**
     * The proxy used to fetch the web page.
     */
    var proxy: String?

    /**
     * The active DOM status of the web page.
     */
    var activeDOMStatus: ActiveDOMStatus?

    /**
     * The trace of active DOM status changes for the web page.
     */
    var activeDOMStatTrace: Map<String, ActiveDOMStat?>

    /**
     * The metadata of the active DOM for the web page.
     */
    var activeDOMMetadata: ActiveDOMMetadata?

    /**
     * The title of the web page.
     */
    var pageTitle: String?

    /**
     * The title of the content of the web page.
     */
    var contentTitle: String?

    /**
     * The text of the web page, typically extracted by combining the texts within all HTML tags.
     */
    var pageText: String?

    /**
     * The text of the content of the web page, typically extracted using some algorithm.
     */
    val contentText: String?

    /**
     * The parse status of the web page.
     */
    var parseStatus: ParseStatus

    /**
     * The links contained within the web page.
     */
    var links: MutableList<CharSequence>

    /**
     * The vivid links contained within the web page, typically change frequently.
     */
    val vividLinks: MutableMap<CharSequence, CharSequence>

    /**
     * The links that point to this web page.
     */
    val inlinks: MutableMap<CharSequence, CharSequence>

    var anchor: CharSequence?

    var pageModelUpdateTime: Instant?

    val pageModel: PageModel?

    /**
     * Retrieves the bean of the specified class type associated with the web page.
     *
     * @param clazz The class type of the bean to retrieve.
     * @return The bean instance.
     */
    fun getBean(clazz: Class<*>): Any

    /**
     * Retrieves the bean of the specified class type associated with the web page, or `null` if it does not exist.
     *
     * @param clazz The class type of the bean to retrieve.
     * @return The bean instance, or `null` if it does not exist.
     */
    fun getBeanOrNull(clazz: Class<*>): Any?

    /**
     * Associates the given bean with the web page.
     *
     * @param bean The bean to associate with the web page.
     */
    fun <T> putBean(bean: T)

    /**
     * Retrieves the data associated with the given name.
     *
     * @param name The name of the data to retrieve.
     * @return The data value, or `null` if it does not exist.
     */
    fun data(name: String): Any?

    /**
     * Sets the data associated with the given name.
     *
     * @param name The name of the data to set.
     * @param value The value to assign to the data.
     */
    fun data(name: String, value: Any?)

    /**
     * Sets the content of the web page using a `String`.
     *
     * @param value The content to set.
     */
    fun setStringContent(value: String?)

    /**
     * Sets the content of the web page using a `ByteArray`.
     *
     * @param value The content to set.
     */
    fun setByteArrayContent(value: ByteArray?)

    /**
     * Sets the content of the web page using a `ByteBuffer`.
     *
     * @param value The content to set.
     */
    fun setByteBufferContent(value: ByteBuffer?)

    /**
     * Clears the persisted content of the web page.
     */
    fun clearPersistContent()

    fun ensurePageModel(): PageModel
}
