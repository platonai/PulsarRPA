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
package `fun`.platonic.pulsar.persist.experimental

import `fun`.platonic.pulsar.common.config.MutableConfig
import `fun`.platonic.pulsar.common.config.PulsarConstants.*
import `fun`.platonic.pulsar.persist.*
import `fun`.platonic.pulsar.persist.gora.generated.GParseStatus
import `fun`.platonic.pulsar.persist.gora.generated.GProtocolStatus
import `fun`.platonic.pulsar.persist.gora.generated.GWebPage
import `fun`.platonic.pulsar.persist.metadata.*
import org.apache.avro.util.Utf8
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * The core data structure across the whole program execution
 *
 *
 * Notice: Use a build-in java string or a Utf8 to serialize strings?
 *
 * @see org.apache.gora.hbase.util.HBaseByteInterface.fromBytes
 *
 *
 * In serializetion phrase, a byte array created by s.getBytes
 */
/**
 * Experimental, do not use this class
 * We are looking for a better way to represent a WebPage
 * */
class GoraWebPage(override val url: String, val page: GWebPage) : IWebPage {

    constructor(url: String) : this(url, GWebPage.newBuilder().build())

    override var metadata: Metadata
        get() = Metadata.box(page.metadata)
        set(value) {
            page.metadata = value.unbox()
        }
    /********************************************************************************
     * Creation fields
     */

    override var markers: CrawlMarks
        get() = CrawlMarks.box(page.markers)
        set(value) {
            page.markers = value.unbox()
        }

    /**
     * All options are saved here, including crawl options, link options, entity options and so on
     */
    override var options: String?
        get() = if (page.options == null) "" else page.options.toString()
        set(options) {
            page.options = options
        }

    override var zoneId: ZoneId
        get() = if (page.zoneId == null) defaultZoneId else ZoneId.of(page.zoneId.toString())
        set(zoneId) {
            page.zoneId = zoneId.id
        }

    override var batchId: String
        get() = if (page.batchId == null) "" else page.batchId.toString()
        set(value) {
            page.batchId = value
        }

    override var distance: Int
        get() {
            val distance = page.distance
            return if (distance < 0) DISTANCE_INFINITE else distance
        }
        set(newDistance) {
            page.distance = newDistance
        }

    /**
     * Fetch mode is used to determine the protocol before fetch
     */
    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    var fetchMode: FetchMode
        get() = FetchMode.fromString(metadata.get(Name.FETCH_MODE))
        set(mode) = metadata.set(Name.FETCH_MODE, mode.name)

    var lastBrowser: BrowserType
        get() = BrowserType.fromString(metadata.get(Name.BROWSER))
        set(browser) = metadata.set(Name.BROWSER, browser.name)

    override var fetchPriority: Int
        get() = if (page.fetchPriority > 0) page.fetchPriority else FETCH_PRIORITY_DEFAULT
        set(priority) {
            page.fetchPriority = priority
        }

    override var createTime: Instant
        get() = Instant.ofEpochMilli(page.createTime)
        set(createTime) {
            page.createTime = createTime.toEpochMilli()
        }

    /********************************************************************************
     * Fetch fields
     */

    override var fetchCount: Int
        get() = page.fetchCount
        set(count) {
            page.fetchCount = count
        }

    override var crawlStatus: CrawlStatus
        get() = CrawlStatus(page.crawlStatus.toByte())
        set(crawlStatus) {
            page.crawlStatus = crawlStatus.code
        }

    /**
     * BaseUrl comes from Content#getBaseUrl which comes from ProtocolOutput
     * Maybe be different from url if the request redirected.
     */
    override var baseUrl: String?
        get() = if (page.baseUrl == null) "" else page.baseUrl.toString()
        set(value) {
            page.baseUrl = value
        }

    override var fetchTime: Instant
        get() = Instant.ofEpochMilli(page.fetchTime)
        set(time) {
            page.fetchTime = time.toEpochMilli()
        }

    override var prevFetchTime: Instant
        get() = Instant.ofEpochMilli(page.prevFetchTime)
        set(time) {
            page.prevFetchTime = time.toEpochMilli()
        }

    override var fetchInterval: Duration
        get() = Duration.ofSeconds(page.fetchInterval.toLong())
        set(interval) {
            page.fetchInterval = interval.seconds.toInt()
        }

    override var protocolStatus: ProtocolStatus
        get() {
            var protocolStatus: GProtocolStatus? = page.protocolStatus
            if (protocolStatus == null) {
                protocolStatus = GProtocolStatus.newBuilder().build()
            }
            return ProtocolStatus.box(protocolStatus)
        }
        set(protocolStatus) {
            page.protocolStatus = protocolStatus.unbox()
        }

    override var reprUrl: String? = null

    override var fetchRetries: Int
        get() = page.fetchRetries
        set(value) {
            page.fetchRetries = value
        }

    override var modifiedTime: Instant
        get() = Instant.ofEpochMilli(page.modifiedTime)
        set(value) {
            page.modifiedTime = value.toEpochMilli()
        }

    override var prevModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.prevModifiedTime)
        set(value) {
            page.prevModifiedTime = value.toEpochMilli()
        }

    /**
     * category : index, detail, media, search
     */
    override var pageCategory: PageCategory
        get() {
            try {
                if (page.pageCategory != null) {
                    return PageCategory.valueOf(page.pageCategory.toString())
                }
            } catch (ignored: Throwable) {
            }

            return PageCategory.UNKNOWN
        }
        set(pageCategory) {
            page.pageCategory = pageCategory.name
        }

    override var encoding: String?
        get() = if (page.encoding == null) null else page.encoding.toString()
        set(encoding) {
            page.encoding = encoding
            metadata.set(Name.CHAR_ENCODING_FOR_CONVERSION, encoding)
        }

    /**
     * The entire raw document content e.g. raw XHTML
     */
    override var content: ByteBuffer? = null

    override var contentType: String?
        get() = if (page.contentType == null) "" else page.contentType.toString()
        set(value) {
            page.contentType = value?.trim { it <= ' ' }?.toLowerCase()
        }

    override var prevSignature: ByteBuffer?
        get() = page.prevSignature
        set(value) {
            page.prevSignature = value
        }

    /**
     * An implementation of a GoraWebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the GoraWebPage's fingerprint representing its state for any point in time.
     */
    override var signature: ByteBuffer? = null

    override var pageTitle: String?
        get() = if (page.pageTitle == null) "" else page.pageTitle.toString()
        set(pageTitle) {
            page.pageTitle = pageTitle
        }

    override var contentTitle: String?
        get() = if (page.contentTitle == null) "" else page.contentTitle.toString()
        set(contentTitle) {
            if (contentTitle != null) {
                page.contentTitle = contentTitle
            }
        }

    override var pageText: String?
        get() = if (page.pageText == null) "" else page.pageText.toString()
        set(value) {
            if (value != null && !value.isEmpty()) page.pageText = value
        }

    override var contentText: String?
        get() = if (page.contentText == null) "" else page.contentText.toString()
        set(textContent) {
            if (textContent != null && !textContent.isEmpty()) {
                page.contentText = textContent
                page.contentTextLen = textContent.length
            }
        }

    override var contentTextLen: Int
        get() = page.contentTextLen
        set(value) {
            page.contentTextLen = value
        }

    /**
     * {GoraWebPage#setParseStatus} must be called later if the status is empty
     */
    override var parseStatus: ParseStatus
        get() {
            val parseStatus = page.parseStatus
            return ParseStatus.box(parseStatus ?: GParseStatus.newBuilder().build())
        }
        set(parseStatus) {
            page.parseStatus = parseStatus.unbox()
        }

    /**
     * Embedded hyperlinks which direct outside of the current domain.
     */
    override var liveLinks: Map<String, HypeLink>?
        get() = page.liveLinks.entries.map { Pair(it.key.toString(), HypeLink.box(it.value)) }.toMap()
        set(value) {
            if (value != null) {
                page.liveLinks = value.entries.map { Pair(it.key, it.value.unbox()) }.toMap()
            } else {
                page.liveLinks = null
            }
        }

    override var vividLinks: Map<String, String>?
        get() = page.vividLinks.map { Pair(it.key.toString(), it.value.toString()) }.toMap()
        set(value) {
            if (value != null) {
                page.vividLinks = value.entries.map { Pair(it.key, it.value) }.toMap()
            } else {
                page.vividLinks = null
            }
        }

    override var deadLinks: List<String>?
        get() = page.deadLinks.map { it.toString() }
        set(deadLinks) {
            page.deadLinks = deadLinks
        }

    override var links: List<String>?
        get() = page.links.map { it.toString() }
        set(links) {
            page.links = links
        }

    override var inlinks: Map<String, String>?
        get() = page.inlinks.map { Pair(it.key.toString(), it.value.toString()) }.toMap()
        set(value) {
            if (value != null) {
                page.inlinks = value.entries.map { Pair(it.key, it.value) }.toMap()
            } else {
                page.inlinks = null
            }
        }

    override var headers: Map<String, String>?
        get() = page.headers.map { Pair(it.key.toString(), it.value.toString()) }.toMap()
        set(value) {
            if (value != null) {
                page.headers = value.entries.map { Pair(it.key, it.value) }.toMap()
            } else {
                page.headers = null
            }
        }

    /**
     * Anchor can be used to sniff article title
     */
    override var anchor: String?
        get() = page.anchor?.toString()
        set(anchor) {
            page.anchor = anchor
        }

    override var anchorOrder: Int
        get() {
            val order = page.anchorOrder
            return if (order < 0) MAX_LIVE_LINK_PER_PAGE else order
        }
        set(order) {
            page.anchorOrder = order
        }

    override var contentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.contentPublishTime)
        set(publishTime) {
            page.contentPublishTime = publishTime.toEpochMilli()
        }

    override var prevContentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.prevContentPublishTime)
        set(publishTime) {
            page.prevContentPublishTime = publishTime.toEpochMilli()
        }

    override var refContentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.refContentPublishTime)
        set(publishTime) {
            page.refContentPublishTime = publishTime.toEpochMilli()
        }

    override var contentModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.contentModifiedTime)
        set(modifiedTime) {
            page.contentModifiedTime = modifiedTime.toEpochMilli()
        }

    override var prevContentModifiedTime: Instant
        get() = Instant.ofEpochMilli(page.prevContentModifiedTime)
        set(modifiedTime) {
            page.prevContentModifiedTime = modifiedTime.toEpochMilli()
        }

    override var prevRefContentPublishTime: Instant
        get() = Instant.ofEpochMilli(page.prevRefContentPublishTime)
        set(publishTime) {
            page.prevRefContentPublishTime = publishTime.toEpochMilli()
        }

    override var referrer: String?
        get() = if (page.referrer == null) "" else page.referrer.toString()
        set(referrer) {
            if (referrer != null && referrer.length > SHORTEST_VALID_URL_LENGTH) {
                page.referrer = referrer
            }
        }

    /********************************************************************************
     * Page Model
     */

    override var pageModel: PageModel
        get() = PageModel.box(page.pageModel)
        set(value) {
            page.pageModel = value.unbox()
        }

    /********************************************************************************
     * Scoring
     */

    override var score: Float
        get() = page.score
        set(value) {
            page.score = value
        }

    override var contentScore: Float
        get() = if (page.contentScore == null) 0.0f else page.contentScore
        set(score) {
            page.contentScore = score
        }

    override var sortScore: String?
        get() = if (page.sortScore == null) "" else page.sortScore.toString()
        set(score) {
            page.sortScore = score
        }

    override var pageCounters: PageCounters
        get() = PageCounters.box(page.pageCounters)
        set(value) {
            page.pageCounters = value.unbox()
        }

    fun unbox(): GWebPage? {
        return page
    }

    override fun equals(other: Any?): Boolean {
        return other is GoraWebPage && other.url == url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

//    override fun toString(): String {
//        return WebPageFormatter(this).format()
//    }

    companion object {

        val LOG = LoggerFactory.getLogger(GoraWebPage::class.java)

        var NIL = newInternalPage(NIL_PAGE_URL, "nil", "nil")

        fun newWebPage(url: String): GoraWebPage {
            Objects.requireNonNull(url)
            return newWebPageInternal(url, null)
        }

        fun newWebPage(url: String, mutableConfig: MutableConfig): GoraWebPage {
            Objects.requireNonNull(url)
            Objects.requireNonNull(mutableConfig)
            return newWebPageInternal(url, mutableConfig)
        }

        private fun newWebPageInternal(url: String, mutableConfig: MutableConfig?): GoraWebPage {
            Objects.requireNonNull(url)

            val page = GoraWebPage(url, GWebPage.newBuilder().build())

            page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
            page.createTime = impreciseNow
            page.score = 0F
            page.fetchCount = 0

            return page
        }

        @JvmOverloads
        fun newInternalPage(url: String, title: String = "internal", content: String = "internal"): GoraWebPage {
            Objects.requireNonNull(url)
            Objects.requireNonNull(title)
            Objects.requireNonNull(content)

            val page = newWebPage(url)

            page.modifiedTime = impreciseNow
            page.fetchTime = Instant.parse("3000-01-01T00:00:00Z")
            page.fetchInterval = ChronoUnit.CENTURIES.duration
            page.fetchPriority = FETCH_PRIORITY_MIN
            page.crawlStatus = CrawlStatus.STATUS_UNFETCHED

            page.distance = DISTANCE_INFINITE // or -1?
            page.markers.put(Mark.INTERNAL, YES_STRING)
            page.markers.put(Mark.INACTIVE, YES_STRING)

            page.pageTitle = title
            // page.setContent(content)

            return page
        }

        /********************************************************************************
         * Other
         */

        fun wrapKey(mark: Mark): Utf8? {
            return u8(mark.value())
        }

        /**
         * What's the difference between String and Utf8?
         */
        fun u8(value: String?): Utf8? {
            return if (value == null) {
                // TODO: return new Utf8.EMPTY?
                null
            } else Utf8(value)
        }
    }
}
