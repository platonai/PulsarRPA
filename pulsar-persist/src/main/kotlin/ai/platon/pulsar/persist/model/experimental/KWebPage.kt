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
package ai.platon.pulsar.persist.model.experimental

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.DateTimes.constructTimeHistory
import ai.platon.pulsar.common.DateTimes.parseInstant
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.HtmlIntegrity.Companion.fromString
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.url.Urls.normalize
import ai.platon.pulsar.common.url.Urls.reverseUrlOrEmpty
import ai.platon.pulsar.common.url.Urls.unreverseUrl
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.gora.generated.GParseStatus
import ai.platon.pulsar.persist.gora.generated.GProtocolStatus
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.*
import ai.platon.pulsar.persist.model.ActiveDomMultiStatus
import ai.platon.pulsar.persist.model.ActiveDomUrls
import ai.platon.pulsar.persist.model.PageModel
import org.apache.avro.util.Utf8
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.apache.gora.util.ByteUtils
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * The core data structure across the whole program execution
 *
 *
 * Notice: Use a build-in java string or a Utf8 to serialize strings?
 *
 * see org .apache .gora. hbase. util .HBaseByteInterface #fromBytes
 *
 *
 * In serializetion phrase, a byte array created by s.getBytes(UTF8_CHARSET) is serialized, and
 * in deserialization phrase, every string are wrapped to be a Utf8
 *
 *
 * So both build-in string and a Utf8 wrap is OK to serialize, and Utf8 is always returned
 */
class KWebPage : Comparable<KWebPage> {
    /**
     * The process scope WebPage instance sequence
     */
    var id = sequencer.incrementAndGet()
        private set
    /**
     * page.location is the last working address, and page.url is the permanent internal address
     */
    /**
     * The url is the permanent internal address, and the location is the last working address
     */
    var url = ""
        private set
    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    var reversedUrl: String? = null
        private set
    /**
     * Underlying persistent object
     */
    private var page: GWebPage
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

    private constructor(url: String, page: GWebPage, urlReversed: Boolean) {
        this.url = if (urlReversed) unreverseUrl(url) else url
        reversedUrl = if (urlReversed) url else reverseUrlOrEmpty(url)
        this.page = page
    }

    private constructor(url: String, reversedUrl: String, page: GWebPage) {
        this.url = url
        this.reversedUrl = reversedUrl
        this.page = page
    }

    val key: String get() = reversedUrl?:""

    val isNil: Boolean get() = this === NIL

    val isNotNil: Boolean get() = !isNil

    val isInternal: Boolean get() = hasMark(Mark.INTERNAL)

    val isNotInternal: Boolean get() = !isInternal

    fun unbox() = page

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
    var options: String?
        get() = if (page.options == null) "" else page.options.toString()
        set(options) {
            page.options = options
        }

    val configuredUrl get() = page.options?.let { "$url $it" }?:url

    var query: String?
        get() = metadata[Name.QUERY]
        set(query) {
            metadata[Name.QUERY] = query
        }

    var zoneId: ZoneId
        get() = if (page.zoneId == null) AppContext.defaultZoneId else ZoneId.of(page.zoneId.toString())
        set(zoneId) {
            page.zoneId = zoneId.id
        }

    var batchId: String?
        get() = if (page.batchId == null) "" else page.batchId.toString()
        set(value) {
            page.batchId = value
        }

    fun markSeed() {
        metadata[Name.IS_SEED] = AppConstants.YES_STRING
    }

    fun unmarkSeed() {
        metadata.remove(Name.IS_SEED)
    }

    val isSeed: Boolean
        get() = metadata.contains(Name.IS_SEED)

    var distance: Int
        get() {
            val distance = page.distance
            return if (distance < 0) AppConstants.DISTANCE_INFINITE else distance
        }
        set(newDistance) {
            page.distance = newDistance
        }

    fun updateDistance(newDistance: Int) {
        val oldDistance = distance
        if (newDistance < oldDistance) {
            distance = newDistance
        }
    }

    /**
     * Fetch mode is used to determine the protocol before fetch
     */
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

    fun sniffFetchPriority(): Int {
        var priority = fetchPriority
        val depth = distance
        if (depth < AppConstants.FETCH_PRIORITY_DEPTH_BASE) {
            priority = Math.max(priority, AppConstants.FETCH_PRIORITY_DEPTH_BASE - depth)
        }
        return priority
    }

    var createTime: Instant
        get() = Instant.ofEpochMilli(page.createTime)
        set(createTime) {
            page.createTime = createTime.toEpochMilli()
        }

    // Old version of generate time, created by String.valueOf(epochMillis)
    var generateTime: Instant
        get() {
            val generateTime = metadata[Name.GENERATE_TIME]
            return if (generateTime == null) {
                Instant.EPOCH
            } else if (NumberUtils.isDigits(generateTime)) { // Old version of generate time, created by String.valueOf(epochMillis)
                Instant.ofEpochMilli(NumberUtils.toLong(generateTime, 0))
            } else {
                Instant.parse(generateTime)
            }
        }
        set(generateTime) {
            metadata[Name.GENERATE_TIME] = generateTime.toString()
        }

    /********************************************************************************
     * Fetch fields
     */
    var fetchCount: Int
        get() = page.fetchCount
        set(count) {
            page.fetchCount = count
        }

    fun increaseFetchCount() {
        val count = fetchCount
        fetchCount = count + 1
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
     * @see KWebPage.getLocation
     *
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

    /**
     * Get last fetch time
     *
     * If fetchTime is before now, the result is the fetchTime
     * If fetchTime is after now, it means that schedule has modified it for the next fetch, the result is prevFetchTime
     */
    fun getLastFetchTime(now: Instant): Instant {
        var lastFetchTime = fetchTime
        if (lastFetchTime.isAfter(now)) { // fetch time is in the further, updated by schedule
            lastFetchTime = prevFetchTime
        }
        return lastFetchTime
    }

    var fetchInterval: Duration
        get() = Duration.ofSeconds(page.fetchInterval.toLong())
        set(value) { page.fetchInterval = value.seconds.toInt() }

    var protocolStatus: ProtocolStatus
        get() {
            val protocolStatus = page.protocolStatus?:GProtocolStatus.newBuilder().build()
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
    fun getHeaders(): ProtocolHeaders {
        return ProtocolHeaders.box(page.headers)
    }

    fun getReprUrl(): String {
        return if (page.reprUrl == null) "" else page.reprUrl.toString()
    }

    fun setReprUrl(value: String) {
        page.reprUrl = value
    }

    /**
     * Get the number of crawl scope retries
     * @see ai.platon.pulsar.persist.RetryScope
     *
     */
    fun getFetchRetries(): Int {
        return page.fetchRetries
    }

    /**
     * Set the number of crawl scope retries
     * @see ai.platon.pulsar.persist.RetryScope
     *
     */
    fun setFetchRetries(value: Int) {
        page.fetchRetries = value
    }

    fun getLastTimeout(): Duration {
        val s = metadata[Name.RESPONSE_TIME]
        return if (s == null) Duration.ZERO else Duration.parse(s)
    }

    fun getModifiedTime(): Instant {
        return Instant.ofEpochMilli(page.modifiedTime)
    }

    fun setModifiedTime(value: Instant) {
        page.modifiedTime = value.toEpochMilli()
    }

    fun getPrevModifiedTime(): Instant {
        return Instant.ofEpochMilli(page.prevModifiedTime)
    }

    fun setPrevModifiedTime(value: Instant) {
        page.prevModifiedTime = value.toEpochMilli()
    }

    fun sniffModifiedTime(): Instant {
        var modifiedTime = getModifiedTime()
        val headerModifiedTime = getHeaders().lastModified
        if (isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = headerModifiedTime
        }
        if (isValidContentModifyTime(contentModifiedTime) && contentModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = contentModifiedTime
        }
        if (isValidContentModifyTime(contentPublishTime) && contentPublishTime.isAfter(modifiedTime)) {
            modifiedTime = contentPublishTime
        }
        // A fix
        if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) { // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
            modifiedTime = Instant.now()
        }
        return modifiedTime
    }

    fun getFetchTimeHistory(defaultValue: String): String {
        val s = metadata[Name.FETCH_TIME_HISTORY]
        return s ?: defaultValue
    }

    /********************************************************************************
     * Parsing
     */
    fun putFetchTimeHistory(fetchTime: Instant) {
        var fetchTimeHistory = metadata[Name.FETCH_TIME_HISTORY]
        fetchTimeHistory = constructTimeHistory(fetchTimeHistory, fetchTime, 10)
        metadata[Name.FETCH_TIME_HISTORY] = fetchTimeHistory
    }

    fun getFirstCrawlTime(defaultValue: Instant): Instant {
        var firstCrawlTime: Instant? = null
        val fetchTimeHistory = getFetchTimeHistory("")
        if (!fetchTimeHistory.isEmpty()) {
            val times = fetchTimeHistory.split(",").toTypedArray()
            val time = parseInstant(times[0], Instant.EPOCH)
            if (time.isAfter(Instant.EPOCH)) {
                firstCrawlTime = time
            }
        }
        return firstCrawlTime ?: defaultValue
    }

    fun getPageCategory(): PageCategory {
        try {
            if (page.pageCategory != null) {
                return PageCategory.parse(page.pageCategory.toString())
            }
        } catch (ignored: Throwable) {
        }
        return PageCategory.UNKNOWN
    }

    /**
     * category : index, detail, review, media, search, etc
     */
    fun setPageCategory(pageCategory: PageCategory) {
        page.pageCategory = pageCategory.name
    }

    fun getEncoding(): String? {
        return if (page.encoding == null) null else page.encoding.toString()
    }

    fun setEncoding(encoding: String) {
        page.encoding = encoding
        metadata[Name.CHAR_ENCODING_FOR_CONVERSION] = encoding
    }

    /**
     * Get content encoding
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

    fun getContentAsBytes(): ByteArray {
        val content = content ?: return ByteUtils.toBytes('\u0000')
        return Bytes.getBytes(content)
    }

    /**
     * TODO: Encoding is always UTF-8?
     */
    fun getContentAsString(): String {
        return Bytes.toString(getContentAsBytes())
    }

    fun getContentAsInputStream(): ByteArrayInputStream {
        val contentInOctets = content ?: return ByteArrayInputStream(ByteUtils.toBytes('\u0000'))
        return ByteArrayInputStream(content!!.array(),
                contentInOctets.arrayOffset() + contentInOctets.position(),
                contentInOctets.remaining())
    }

    fun getContentAsSaxInputSource(): InputSource {
        val inputSource = InputSource(getContentAsInputStream())
        val encoding = getEncoding()
        if (encoding != null) {
            inputSource.encoding = encoding
        }
        return inputSource
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

    fun getActiveDomMultiStatus(): ActiveDomMultiStatus? { // cached
        val name = Name.ACTIVE_DOM_MULTI_STATUS
        val value = variables[name]
        if (value is ActiveDomMultiStatus) {
            return value
        } else {
            val json = metadata[name]
            if (json != null) {
                val status = ActiveDomMultiStatus.fromJson(json)
                variables[name] = status
                return status
            }
        }
        return null
    }

    fun setActiveDomMultiStatus(domStatus: ActiveDomMultiStatus?) {
        if (domStatus != null) {
            variables[Name.ACTIVE_DOM_MULTI_STATUS] = domStatus
            metadata[Name.ACTIVE_DOM_MULTI_STATUS] = domStatus.toJson()
        }
    }

    fun getActiveDomUrls(): ActiveDomUrls? { // cached
        val name = Name.ACTIVE_DOM_URLS
        val value = variables[name]
        if (value is ActiveDomUrls) {
            return value
        } else {
            val json = metadata[name]
            if (json != null) {
                val status = ActiveDomUrls.fromJson(json)
                variables[name] = status
                return status
            }
        }
        return null
    }

    fun setActiveDomUrls(urls: ActiveDomUrls?) {
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

    var prevSignature: ByteBuffer?
        get() = page.prevSignature
        set(value) { page.prevSignature = value }

    fun setSignature(value: ByteArray?) {
        page.signature = ByteBuffer.wrap(value)
    }

    val signatureAsString: String get() = signature?.let { Strings.toHexString(signature) }?:""

    val prevSignatureAsString: String get() = prevSignature?.let { Strings.toHexString(prevSignature) }?:""

    var pageTitle: String?
        get() = page.pageTitle?.toString()
        set(value) { page.pageTitle = value }

    var contentTitle: String?
        get() = page.contentTitle?.toString()
        set(value) { page.contentTitle = value }

    fun sniffTitle(): String {
        return contentTitle?.takeUnless { it.isBlank() }
                ?: anchor.takeUnless { it.isBlank() }
                ?: pageTitle.takeUnless { it.isNullOrBlank() }
                ?: location.takeUnless { it.isBlank() }
                ?: url
    }

    fun getPageText(): String {
        return if (page.pageText == null) "" else page.pageText.toString()
    }

    fun setPageText(value: String?) {
        if (value != null && !value.isEmpty()) page.pageText = value
    }

    fun getContentText(): String {
        return if (page.contentText == null) "" else page.contentText.toString()
    }

    fun setContentText(textContent: String?) {
        if (textContent != null && !textContent.isEmpty()) {
            page.contentText = textContent
            page.contentTextLen = textContent.length
        }
    }

    fun getContentTextLen(): Int {
        return page.contentTextLen
    }

    /**
     * Set all text fields cascaded, including content, content text and page text.
     */
    fun setTextCascaded(text: String) {
        setContent(text)
        setContentText(text)
        setPageText(text)
    }

    var parseStatus: ParseStatus
        get() = ParseStatus.box(page.parseStatus ?: GParseStatus.newBuilder().build())
        set(value) {
            page.parseStatus = value.unbox()
        }

    fun getSimpleLiveLinks(): Collection<String> {
        return CollectionUtils.collect(page.liveLinks.keys) { obj: CharSequence -> obj.toString() }
    }

    /**
     * TODO: Remove redundant url to reduce space
     */
    fun setLiveLinks(liveLinks: Iterable<HyperlinkPersistable>) {
        page.liveLinks.clear()
        val links = page.liveLinks
        liveLinks.forEach(Consumer { l: HyperlinkPersistable -> links[l.url] = l.unbox() })
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

    var deadLinks: List<CharSequence>
        get() = page.deadLinks
        set(value) {
            page.deadLinks = value
        }

    var links: List<CharSequence>
        get() = page.links
        set(value) {
            page.links = value
        }

    /**
     * Record all links appeared in a page
     * The links are in FIFO order, for each time we fetch and parse a page,
     * we push newly discovered links to the queue, if the queue is full, we drop out some old ones,
     * usually they do not appears in the page any more.
     *
     *
     * TODO: compress links
     * TODO: HBase seems not modify any nested array
     */
    fun addHyperlinks(hyperLinks: Iterable<HyperlinkPersistable>) {
        var links = page.links
        // If there are too many links, Drop the front 1/3 links
        if (links.size > AppConstants.MAX_LINK_PER_PAGE) {
            links = links.subList(links.size - AppConstants.MAX_LINK_PER_PAGE / 3, links.size)
        }
        for (l in hyperLinks) {
            val url = u8(l.url)
            if (!links.contains(url)) {
                links.add(url)
            }
        }
        this.links = links
        setImpreciseLinkCount(links.size)
    }

    fun addLinks(hypeLinks: Iterable<CharSequence>) {
        var links = page.links
        // If there are too many links, Drop the front 1/3 links
        if (links.size > AppConstants.MAX_LINK_PER_PAGE) {
            links = links.subList(links.size - AppConstants.MAX_LINK_PER_PAGE / 3, links.size)
        }
        for (link in hypeLinks) {
            val url = u8(link.toString())
            // Use a set?
            if (!links.contains(url)) {
                links.add(url)
            }
        }
        this.links = links
        setImpreciseLinkCount(links.size)
    }

    fun getImpreciseLinkCount(): Int {
        val count = metadata.getOrDefault(Name.TOTAL_OUT_LINKS, "0")
        return NumberUtils.toInt(count, 0)
    }

    fun setImpreciseLinkCount(count: Int) {
        metadata[Name.TOTAL_OUT_LINKS] = count.toString()
    }

    fun increaseImpreciseLinkCount(count: Int) {
        val oldCount = getImpreciseLinkCount()
        setImpreciseLinkCount(oldCount + count)
    }

    fun getInlinks(): Map<CharSequence, CharSequence> {
        return page.inlinks
    }

    var anchor: String
        get() = page.anchor?.toString()?:""
        set(value) { page.anchor = value }

    var inlinkAnchors: Array<String>
        get() = StringUtils.split(metadata.getOrDefault(Name.ANCHORS, ""), "\n")
        set(value) {
            metadata.set(Name.ANCHORS, StringUtils.join(value, "\n"))
        }

    var anchorOrder: Int
        get() = if (page.anchorOrder < 0) AppConstants.MAX_LIVE_LINK_PER_PAGE else page.anchorOrder
        set(value) { page.anchorOrder = value }

    private fun isValidContentModifyTime(publishTime: Instant): Boolean {
        return publishTime.isAfter(AppConstants.MIN_ARTICLE_PUBLISH_TIME) && publishTime.isBefore(AppContext.imprecise2DaysAhead)
    }

    fun updateContentPublishTime(newPublishTime: Instant): Boolean {
        if (!isValidContentModifyTime(newPublishTime)) {
            return false
        }
        val lastPublishTime = contentPublishTime
        if (newPublishTime.isAfter(lastPublishTime)) {
            prevContentPublishTime = lastPublishTime
            contentPublishTime = newPublishTime
        }
        return true
    }

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

    fun updateContentModifiedTime(newModifiedTime: Instant): Boolean {
        if (!isValidContentModifyTime(newModifiedTime)) {
            return false
        }
        val lastModifyTime = contentModifiedTime
        if (newModifiedTime.isAfter(lastModifyTime)) {
            prevContentModifiedTime = lastModifyTime
            contentModifiedTime = newModifiedTime
        }
        return true
    }

    fun updateRefContentPublishTime(newRefPublishTime: Instant): Boolean {
        if (!isValidContentModifyTime(newRefPublishTime)) {
            return false
        }
        val latestRefPublishTime = refContentPublishTime
        // LOG.debug("Ref Content Publish Time: " + latestRefPublishTime + " -> " + newRefPublishTime + ", Url: " + getUrl());
        if (newRefPublishTime.isAfter(latestRefPublishTime)) {
            prevRefContentPublishTime = latestRefPublishTime
            refContentPublishTime = newRefPublishTime
            // LOG.debug("[Updated] " + latestRefPublishTime + " -> " + newRefPublishTime);
            return true
        }
        return false
    }

    var referrer: String?
        get() = page.referrer?.toString()
        set(value) {
            page.referrer = value
        }

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

    fun putIndexTimeHistory(indexTime: Instant?) {
        var indexTimeHistory = metadata[Name.INDEX_TIME_HISTORY]
        indexTimeHistory = constructTimeHistory(indexTimeHistory, indexTime!!, 10)
        metadata[Name.INDEX_TIME_HISTORY] = indexTimeHistory
    }

    fun getFirstIndexTime(defaultValue: Instant): Instant {
        var firstIndexTime: Instant? = null
        val indexTimeHistory = getIndexTimeHistory("")
        if (!indexTimeHistory.isEmpty()) {
            val times = indexTimeHistory.split(",").toTypedArray()
            val time = parseInstant(times[0], Instant.EPOCH)
            if (time.isAfter(Instant.EPOCH)) {
                firstIndexTime = time
            }
        }
        return firstIndexTime ?: defaultValue
    }

    override fun hashCode() = url.hashCode()

    override fun compareTo(other: KWebPage) = url.compareTo(other.url)

    override fun equals(other: Any?) = other is KWebPage && other.url == url

    override fun toString() = url

    companion object {
        val LOG = LoggerFactory.getLogger(KWebPage::class.java)
        var sequencer = AtomicInteger()
        var NIL = newInternalPage(AppConstants.NIL_PAGE_URL, 0, "nil", "nil")

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
            val page = KWebPage(url, GWebPage.newBuilder().build(), false)
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
            page.setModifiedTime(Instant.now())
            page.fetchTime = Instant.parse("3000-01-01T00:00:00Z")
            page.fetchInterval = ChronoUnit.CENTURIES.duration
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
        fun box(url: String, reversedUrl: String, page: GWebPage) = KWebPage(url, reversedUrl, page)

        /**
         * Initialize a WebPage with the underlying GWebPage instance.
         */
        fun box(url: String, page: GWebPage) = KWebPage(url, page, false)

        /**
         * Initialize a WebPage with the underlying GWebPage instance.
         */
        fun box(url: String, page: GWebPage, urlReversed: Boolean) = KWebPage(url, page, urlReversed)

        /********************************************************************************
         * Other
         */
        fun wrapKey(mark: Mark) = u8(mark.value())

        /**
         * What's the difference between String and Utf8?
         */
        fun u8(value: String?) = value?.let { Utf8(it) }
    }
}
