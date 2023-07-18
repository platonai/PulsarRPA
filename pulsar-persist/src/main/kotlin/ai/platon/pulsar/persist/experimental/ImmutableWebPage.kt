/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.AppConstants.DISTANCE_INFINITE
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.config.VolatileConfig.Companion.UNSAFE
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrlOrEmpty
import ai.platon.pulsar.common.urls.UrlUtils.unreverseUrl
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.gora.generated.*
import ai.platon.pulsar.persist.metadata.*
import ai.platon.pulsar.persist.metadata.OpenPageCategory.Companion.parse
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import ai.platon.pulsar.persist.model.Converters.convert
import ai.platon.pulsar.persist.model.PageModel
import ai.platon.pulsar.persist.model.PageModel.Companion.box
import org.apache.commons.lang3.StringUtils
import org.apache.gora.util.ByteUtils
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * The core web page structure
 */
open class ImmutableWebPage {
    companion object {
        val NIL: ImmutableWebPage = ImmutableWebPage("", "", GWebPage(), UNSAFE)
    }

    /**
     * The url is the permanent internal address, and the location is the last working address
     */
    var url = ""
        private set

    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    var reversedUrl = ""
        private set

    /**
     * Underlying persistent object
     */
    private var page: GWebPage
        private set

    /**
     * Web page scope configuration
     */
    open val conf: VolatileConfig
    /**
     * *****************************************************************************
     * Common fields
     * ******************************************************************************
     *
     * @return a [variables] object.
     */
    /**
     * Web page scope variables
     */
    val variables = Variables()

    /**
     * If this page is fetched from internet
     */
    open val isCached = false

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    open val isLoaded = false

    /**
     * If this page is fetched from internet
     */
    open val isFetched = false
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If this page is canceled
     */
    open val isCanceled = false

    /**
     * If this page is fetched and updated
     */
    open val isContentUpdated = false
    /**
     * Get the cached content
     */
    /**
     * Set the cached content, keep the persisted page content unmodified
     */
    /**
     * The cached content
     */
    open val tmpContent: ByteBuffer? = null

    /**
     * The delay time to retry if a retry is needed
     */
    open val retryDelay = Duration.ZERO

    private constructor(
        url: String, page: GWebPage, urlReversed: Boolean, conf: VolatileConfig
    ) {
        this.url = if (urlReversed) unreverseUrl(url) else url
        reversedUrl = if (urlReversed) url else reverseUrlOrEmpty(url)
        this.conf = conf
        this.page = page

        // the url of a page might be normalized, but the baseUrl always keeps be the original
        if (page.baseUrl == null) {
            // setLocation(url)
        }
    }

    private constructor(
        url: String, reversedUrl: String, page: GWebPage, conf: VolatileConfig
    ) {
        this.url = url
        this.reversedUrl = reversedUrl
        this.conf = conf
        this.page = page

        // BaseUrl is the last working address, it might redirect to url, or it might have random parameters
        if (page.baseUrl == null) {
            // setLocation(url)
        }
    }

    open val key: String
        get() = reversedUrl
    /**
     * Get The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * TODO: use a separate field for href
     *
     * @return The hypertext reference
     */
    /**
     * Set The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * @param href The hypertext reference
     */
    open val href: String? get() = metadata[Name.HREF]
    open val isNil get() = this === NIL
    open val isNotNil get() = !isNil
    open val isInternal get() = hasMark(Mark.INTERNAL)
    open val isNotInternal get() = !isInternal

    fun unbox() = page

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

    open val metadata get() = Metadata.box(page.metadata)

    open val marks get() = CrawlMarks.box(page.markers)

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
    open val args: String get() = page.params?.toString() ?: ""

    open val maxRetries: Int get() = metadata.getInt(Name.FETCH_MAX_RETRY, 3)

    open val configuredUrl get() = UrlUtils.mergeUrlArgs(url, args)

    open val fetchedLinkCount get() = metadata.getInt(Name.FETCHED_LINK_COUNT, 0)

    open val zoneId get() = if (page.zoneId == null) DateTimes.zoneId else ZoneId.of(page.zoneId.toString())

    open val batchId get() = page.batchId?.toString() ?: ""

    open val isSeed get() = metadata.contains(Name.IS_SEED)

    open val distance get() = page.distance.takeIf { it > 0 } ?: DISTANCE_INFINITE

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    open val fetchMode get() = FetchMode.fromString(metadata[Name.FETCH_MODE])

    open val lastBrowser get() = BrowserType.fromString(page.browser?.toString())

    open val isResource get() = page.resource != null

    open val htmlIntegrity get() = HtmlIntegrity.fromString(page.htmlIntegrity?.toString())

    open val fetchPriority get() = page.fetchPriority ?: AppConstants.FETCH_PRIORITY_DEFAULT

    open val createTime get() = Instant.ofEpochMilli(page.createTime)

    open val generateTime get() = Instant.parse(metadata[Name.GENERATE_TIME] ?: "0")

    open val fetchCount: Int get() = page.fetchCount

    open val crawlStatus get() = CrawlStatus(page.crawlStatus.toByte())

    /**
     * The baseUrl is as the same as Location
     *
     *
     * A baseUrl has the same semantic with Jsoup.parse:
     *
     * @return a [String] object.
     * @link {https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse-java.io.File-java.lang.String-java.lang.String-}
     * @see ImmutableWebPage.getLocation
     */
    open val baseUrl: String
        get() = if (page.baseUrl == null) "" else page.baseUrl.toString()

    /**
     * WebPage.url is the permanent internal address, it might not still available to access the target.
     * And WebPage.location or WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url, it's generally normalized.
     */
    open val location: String get() = baseUrl

    /**
     * The latest fetch time
     *
     * @return The latest fetch time
     */
    open val fetchTime get() = Instant.ofEpochMilli(page.fetchTime)

    /**
     * The previous fetch time, updated at the fetch stage
     *
     * @return The previous fetch time.
     */
    open val prevFetchTime get() = Instant.ofEpochMilli(page.prevFetchTime)

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     */
    open val prevCrawlTime1 get() = Instant.ofEpochMilli(page.prevCrawlTime1)

    /**
     * Get fetch interval
     */
    open val fetchInterval: Duration
        get() = if (page.fetchInterval > 0) {
            Duration.ofSeconds(page.fetchInterval.toLong())
        } else ChronoUnit.CENTURIES.duration

    /**
     * Get protocol status
     */
    open val protocolStatus get() = ProtocolStatus.box(page.protocolStatus ?: GProtocolStatus.newBuilder().build())

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
    open val headers get() = ProtocolHeaders.box(page.headers)

    open val reprUrl get() = if (page.reprUrl == null) "" else page.reprUrl.toString()

    open val fetchRetries get() = page.fetchRetries

    open val modifiedTime get() = Instant.ofEpochMilli(page.modifiedTime)

    open val prevModifiedTime get() = Instant.ofEpochMilli(page.prevModifiedTime)

    open fun getFetchTimeHistory(defaultValue: String) = metadata[Name.FETCH_TIME_HISTORY] ?: defaultValue

    open val pageCategory: PageCategory
        get() = kotlin.runCatching { PageCategory.parse(page.pageCategory.toString()) }.getOrNull()
            ?: PageCategory.UNKNOWN

    val openPageCategory: OpenPageCategory
        get() = kotlin.runCatching { parse(page.pageCategory.toString()) }.getOrNull()
            ?: OpenPageCategory("", "")

    /**
     * Get the encoding of the content.
     * Content encoding is detected just before it's parsed.
     */
    open val encoding get() = page.encoding?.toString()

    /**
     * The entire raw document content e.g. raw XHTML
     *
     * @return The raw document content in [ByteBuffer].
     */
    open fun getContent() = tmpContent ?: page.content

    /**
     * Get the persistent page content
     */
    open fun getPersistContent() = page.content

    /**
     * Get content as bytes, the underling buffer is duplicated
     *
     * @return a duplication of the underling buffer.
     */
    fun getContentAsBytes(): ByteArray {
        val content = getContent() ?: return ByteUtils.toBytes('\u0000')
        return ByteUtils.toBytes(content)
    }

    /**
     * Get the page content as a string, if the underlying page content is null, return an empty string
     */
    fun getContentAsString(): String {
        val buffer = getContent()
        return if (buffer == null || buffer.remaining() == 0) {
            ""
        } else String(buffer.array(), buffer.arrayOffset(), buffer.limit())
    }

    /**
     * Get the page content as input stream
     */
    fun getContentAsInputStream(): ByteArrayInputStream {
        val contentInOctets = getContent() ?: return ByteArrayInputStream(ByteUtils.toBytes('\u0000'))
        return ByteArrayInputStream(
            getContent()!!.array(),
            contentInOctets.arrayOffset() + contentInOctets.position(),
            contentInOctets.remaining()
        )
    }

    /**
     * Get the page content as sax input source
     */
    val contentAsSaxInputSource: InputSource
        get() {
            val inputSource = InputSource(getContentAsInputStream())
            val encoding = encoding
            if (encoding != null) {
                inputSource.encoding = encoding
            }
            return inputSource
        }

    /**
     * Get the length of content in bytes.
     *
     * TODO: check consistency with HttpHeaders.CONTENT_LENGTH
     *
     * @return The length of the content in bytes.
     */
    open val contentLength get() = page.contentLength ?: 0

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

    open val persistedContentLength get() = page.persistedContentLength ?: 0

    open val lastContentLength get() = page.lastContentLength ?: 0

    open val aveContentLength get() = page.aveContentLength ?: 0

    open val contentType get() = page.contentType?.toString() ?: ""

    open val prevSignature get() = page.prevSignature

    open val prevSignatureAsString get() = Strings.toHexString(prevSignature ?: ByteBuffer.wrap("".toByteArray()))

    /**
     * The last proxy used to fetch the page
     */
    open val proxy get() = page.proxy?.toString()

    open val activeDOMStatus: ActiveDOMStatus?
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

    open val activeDOMStatTrace get() = page.activeDOMStatTrace.entries.associate { it.key.toString() to convert(it.value) }

    /**
     * An implementation of a WebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the WebPage's fingerprint representing its state for any point in time.
     */
    open val signature get() = page.signature

    open val signatureAsString get() = Strings.toHexString(signature ?: ByteBuffer.wrap("".toByteArray()))

    open val pageTitle get() = page.pageTitle?.toString() ?: ""

    open val contentTitle get() = page.contentTitle?.toString() ?: ""

    open val pageText get() = page.pageText?.toString() ?: ""

    open val contentText get() = page.contentText?.toString() ?: ""

    open val contentTextLen get() = page.contentTextLen

    open val parseStatus get() = ParseStatus.box(page.parseStatus ?: GParseStatus.newBuilder().build())

    open val liveLinks get() = page.liveLinks

    open val simpleLiveLinks get() = page.liveLinks.keys.map { it.toString() }

    open val vividLinks get() = page.vividLinks

    open val simpleVividLinks get() = page.vividLinks.keys.map { it.toString() }

    open val deadLinks get() = page.deadLinks

    open val links get() = page.links

    open val impreciseLinkCount get() = metadata.get(Name.TOTAL_OUT_LINKS)?.toIntOrNull() ?: 0

    open val inlinks get() = page.inlinks

    open val anchor get() = page.anchor ?: ""

    open val inlinkAnchors get() = StringUtils.split(metadata.getOrDefault(Name.ANCHOR_COUNT, ""), "\n")

    open val anchorOrder get() = page.anchorOrder

    open val contentPublishTime get() = Instant.ofEpochMilli(page.contentPublishTime)

    open val prevContentPublishTime get() = Instant.ofEpochMilli(page.prevContentPublishTime)

    open val refContentPublishTime get() = Instant.ofEpochMilli(page.refContentPublishTime)

    open val contentModifiedTime get() = Instant.ofEpochMilli(page.contentModifiedTime)

    open val prevContentModifiedTime get() = Instant.ofEpochMilli(page.prevContentModifiedTime)

    open val prevRefContentPublishTime get() = Instant.ofEpochMilli(page.prevRefContentPublishTime)

    open val referrer get() = if (page.referrer == null) null else page.referrer.toString()

    /**
     * *****************************************************************************
     * Page Model
     * ******************************************************************************
     */
    open val pageModelUpdateTime get() = Instant.ofEpochMilli(page.pageModelUpdateTime)

    open val pageModel get() = if (page.pageModel == null) null else box(page.pageModel)

    open fun ensurePageModel(): PageModel {
        if (page.pageModel == null) {
            page.pageModel = GPageModel.newBuilder().build()
        }
        return pageModel!!
    }

    /**
     * *****************************************************************************
     * Scoring
     * ******************************************************************************
     */
    open val score get() = page.score

    open val contentScore get() = page.contentScore ?: 0.0

    open val sortScore get() = page.sortScore?.toString() ?: ""

    open val cash get() = metadata.getFloat(Name.CASH_KEY, 0.0f)

    open val pageCounters get() = PageCounters.box(page.pageCounters)

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     */
    override fun hashCode() = url.hashCode()

    override fun equals(other: Any?): Boolean {
        return if (this === other) true else other is ImmutableWebPage && other.url == url
    }

    override fun toString() = url
}
