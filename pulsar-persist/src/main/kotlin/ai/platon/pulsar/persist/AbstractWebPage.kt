package ai.platon.pulsar.persist

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.UrlUtils.mergeUrlArgs
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrlOrEmpty
import ai.platon.pulsar.common.urls.UrlUtils.unreverseUrl
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.WebPageFormatter
import org.apache.gora.util.ByteUtils
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.concurrent.Volatile

/**
 * The core web page structure
 */
abstract class AbstractWebPage (
    /**
     * The url is the permanent internal address, while the location is the last working address.
     */
    override val url: String,
    /**
     * A webpage scope configuration, any modifications made to it will exclusively impact this particular webpage.
     */
    override var conf: VolatileConfig
) : WebPage {
    companion object {
        // The ID_SEQUENCER is an AtomicInteger initialized to 10 to avoid conflicts with the default ID of 0.
        private val ID_SEQUENCER = AtomicInteger(10)

        /**
         * Returns the URL based on whether it should be reversed or not.
         *
         * @param urlOrKey The original URL or key.
         * @param urlReversed A flag indicating if the URL should be reversed.
         * @return The processed URL.
         */
        protected fun getUrl(urlOrKey: String, urlReversed: Boolean): String {
            return if (urlReversed) unreverseUrl(urlOrKey) else urlOrKey
        }
    }

    /**
     * A process scope page id.
     */
    /**
     * The page id which is unique in process scope.
     */
    override var id: Int = ID_SEQUENCER.incrementAndGet()
        protected set

    val reversedUrl get() = reverseUrlOrEmpty(url)

    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this webpage.
     * It's faster to retrieve the page by the reversed url.
     */
    override val key: String get() = reversedUrl

    /**
     * Web page scope variables
     */
    val variables: Variables = Variables()

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
        protected set

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

    override val isNotNil: Boolean
        get() = !isNil

    override val isInternal: Boolean
        get() = UrlUtils.isInternal(url)

    override val isNotInternal: Boolean
        get() = !isInternal

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

    override var maxRetries: Int
        get() = metadata.getInt(Name.FETCH_MAX_RETRY, 3)
        set(maxRetries) {
            metadata[Name.FETCH_MAX_RETRY] = maxRetries
        }

    override val configuredUrl: String
        get() = mergeUrlArgs(url, args)

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

    override val contentAsBytes get() = getContentAsBytes0()

    override val contentAsString get() = getContentAsString0()

    override val contentAsInputStream get() = getContentAsInputStream0()

    override val contentAsSaxInputSource get() = getContentAsSaxInputSource0()

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
}
