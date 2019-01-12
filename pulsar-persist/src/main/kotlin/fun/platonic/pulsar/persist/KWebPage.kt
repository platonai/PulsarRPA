package `fun`.platonic.pulsar.persist

import `fun`.platonic.pulsar.common.DateTimeUtil
import `fun`.platonic.pulsar.common.PulsarConstants
import `fun`.platonic.pulsar.common.PulsarConstants.EXAMPLE_URL
import `fun`.platonic.pulsar.common.StringUtil
import `fun`.platonic.pulsar.common.config.MutableConfig
import `fun`.platonic.pulsar.persist.metadata.Mark
import `fun`.platonic.pulsar.persist.metadata.Name
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.apache.gora.util.ByteUtils
import org.apache.hadoop.hbase.util.Bytes
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Experimental, do not use this class
 * We are looking for a better way to represent a WebPage
 * */
class KWebPage(implementation: IWebPage): IWebPage by implementation {

    val variables = Variables()

    /********************************************************************************
     * Common fields
     */
    /**
     * Object scope configuration
     */
    var mutableConfig: MutableConfig? = null

    val isInternal: Boolean
        get() = hasMark(Mark.INTERNAL)

    val isNotInternal: Boolean
        get() = !isInternal

    val isSeed: Boolean
        get() = metadata.contains(Name.IS_SEED)

    val configuredUrl: String
        get() {
            var configuredUrl = url
            if (options != null) {
                configuredUrl += " " + options.toString()
            }
            return configuredUrl
        }

    var query: String
        get() = metadata.get(Name.QUERY)
        set(query) {
            Objects.requireNonNull(query)
            metadata.set(Name.QUERY, query)
        }

    fun hasMark(mark: Mark): Boolean {
        return markers[mark] != null
    }

    fun markSeed() {
        metadata.set(Name.IS_SEED, PulsarConstants.YES_STRING)
    }

    fun unmarkSeed() {
        metadata.remove(Name.IS_SEED)
    }

    fun updateDistance(newDistance: Int) {
        val oldDistance = distance
        if (newDistance < oldDistance) {
            distance = newDistance
        }
    }

    fun sniffFetchPriority(): Int {
        var priority = fetchPriority

        val depth = distance
        if (depth < PulsarConstants.FETCH_PRIORITY_DEPTH_BASE) {
            priority = Math.max(priority, PulsarConstants.FETCH_PRIORITY_DEPTH_BASE - depth)
        }

        return priority
    }

    fun increaseFetchCount() {
        val count = fetchCount
        fetchCount = count + 1
    }

    // Old version of generate time, created by String.valueOf(epochMillis)
    var generateTime: Instant
        get() {
            val generateTime = metadata.get(Name.GENERATE_TIME)
            return if (generateTime == null) {
                Instant.EPOCH
            } else if (NumberUtils.isDigits(generateTime)) {
                Instant.ofEpochMilli(NumberUtils.toLong(generateTime, 0))
            } else {
                Instant.parse(generateTime)
            }
        }
        set(generateTime) = metadata.set(Name.GENERATE_TIME, generateTime.toString())

    /**
     * Get last fetch time
     *
     *
     * If fetchTime is before now, the result is the fetchTime
     * If fetchTime is after now, it means that schedule has modified it for the next fetch, the result is prevFetchTime
     */
    fun getLastFetchTime(now: Instant): Instant {
        var lastFetchTime = fetchTime
        // Minus 1 seconds to protect from inaccuracy
        if (lastFetchTime.isAfter(now.plusSeconds(1))) {
            // updated by schedule
            lastFetchTime = prevFetchTime
        }
        return lastFetchTime
    }

    fun getFetchInterval(destUnit: TimeUnit): Long {
        return destUnit.convert(fetchInterval.seconds, TimeUnit.SECONDS)
    }

    fun getFetchTimeHistory(defaultValue: String): String {
        val s = metadata.get(Name.FETCH_TIME_HISTORY)
        return s ?: defaultValue
    }

    /********************************************************************************
     * Parsing
     */

    fun putFetchTimeHistory(fetchTime: Instant) {
        var fetchTimeHistory = metadata.get(Name.FETCH_TIME_HISTORY)
        fetchTimeHistory = DateTimeUtil.constructTimeHistory(fetchTimeHistory, fetchTime, 10)
        metadata.set(Name.FETCH_TIME_HISTORY, fetchTimeHistory)
    }

    /**
     * Get content encoding
     * Content encoding is detected just before it's parsed
     */
    fun getEncodingOrDefault(defaultEncoding: String): String {
        return if (encoding == null) defaultEncoding else encoding.toString()
    }

    fun hasContent(): Boolean {
        return content != null
    }

    fun setContent(value: String) {
        setContent(value.toByteArray())
    }

    fun setContent(value: ByteArray?) {
        if (value != null) {
            content = ByteBuffer.wrap(value)
        } else {
            content = null
        }
    }

    fun setSignature(value: ByteArray) {
        signature = ByteBuffer.wrap(value)
    }

    /**
     * Set all text fields cascaded, including content, content text and page text.
     */
    fun setTextCascaded(text: String) {
        setContent(text)
        contentText = text
        pageText = text
    }

//    fun addLiveLink(hypeLink: HypeLink) {
//        page.liveLinks[hypeLink.url] = hypeLink.unbox()
//    }

//    fun increaseImpreciseLinkCount(count: Int) {
//        val oldCount = impreciseLinkCount
//        impreciseLinkCount = oldCount + count
//    }

    fun setInlinkAnchors(anchors: Collection<String>) {
        metadata.set(Name.ANCHORS, StringUtils.join(anchors, "\n"))
    }

    private fun isValidContentModifyTime(publishTime: Instant): Boolean {
        return publishTime.isAfter(PulsarConstants.MIN_ARTICLE_PUBLISH_TIME) && publishTime.isBefore(imprecise2DaysAhead)
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

    /********************************************************************************
     * Index
     */

    fun getIndexTimeHistory(defaultValue: String): String {
        val s = metadata.get(Name.INDEX_TIME_HISTORY)
        return s ?: defaultValue
    }

    fun putIndexTimeHistory(indexTime: Instant) {
        var indexTimeHistory = metadata.get(Name.INDEX_TIME_HISTORY)
        indexTimeHistory = DateTimeUtil.constructTimeHistory(indexTimeHistory, indexTime, 10)
        metadata.set(Name.INDEX_TIME_HISTORY, indexTimeHistory)
    }

    fun getFirstIndexTime(defaultValue: Instant): Instant {
        var firstIndexTime: Instant? = null

        val indexTimeHistory = getIndexTimeHistory("")
        if (!indexTimeHistory.isEmpty()) {
            val times = indexTimeHistory.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val time = DateTimeUtil.parseInstant(times[0], Instant.EPOCH)
            if (time.isAfter(Instant.EPOCH)) {
                firstIndexTime = time
            }
        }

        return if (firstIndexTime == null) defaultValue else firstIndexTime
    }

    /**
     * namespace : metadata, seed, www
     * reserved
     */
    /**
     * reserved
     */
    var namespace: String?
        get() = metadata.get("namespace")
        set(ns) = metadata.set("namespace", ns)


    var encodingClues: String
        get() = metadata.getOrDefault(Name.ENCODING_CLUES, "")
        set(clues) = metadata.set(Name.ENCODING_CLUES, clues)

    val contentAsBytes: ByteArray
        get() {
            val content = content ?: return ByteUtils.toBytes('\u0000')
            return Bytes.getBytes(content)
        }

    /**
     * TODO: Encoding is always UTF-8?
     */
    val contentAsString: String
        get() = Bytes.toString(contentAsBytes)

    val contentAsInputStream: ByteArrayInputStream
        get() {
            val contentInOctets = content ?: ByteBuffer.wrap(ByteUtils.toBytes('\u0000'))
            return ByteArrayInputStream(contentInOctets.array(),
                    contentInOctets.arrayOffset() + contentInOctets.position(),
                    contentInOctets.remaining())
        }

    val contentAsSaxInputSource: InputSource
        get() {
            val inputSource = InputSource(contentAsInputStream)
            val encoding = encoding
            if (encoding != null) {
                inputSource.encoding = encoding
            }
            return inputSource
        }

    var contentBytes: Int
        get() = metadata.getInt(Name.CONTENT_BYTES, 0)
        set(bytes) = metadata.set(Name.CONTENT_BYTES, bytes.toString())


    val prevSignatureAsString: String
        get() {
            var sig: ByteBuffer? = prevSignature
            if (sig == null) {
                sig = ByteBuffer.wrap("".toByteArray())
            }
            return StringUtil.toHexString(sig)
        }

    val signatureAsString: String
        get() {
            var sig = signature
            if (sig == null) {
                sig = ByteBuffer.wrap("".toByteArray())
            }
            return StringUtil.toHexString(sig)
        }

    val simpleLiveLinks: Collection<String>
        get() = if (liveLinks != null) {
            liveLinks!!.keys
        } else {
            arrayListOf<String>()
        }


    val simpleVividLinks: Collection<String>
        get() = if (vividLinks != null) {
            vividLinks!!.keys
        } else {
            arrayListOf<String>()
        }

    var impreciseLinkCount: Int
        get() {
            val count = metadata.getOrDefault(Name.TOTAL_OUT_LINKS, "0")
            return NumberUtils.toInt(count, 0)
        }
        set(count) = metadata.set(Name.TOTAL_OUT_LINKS, count.toString())

    val inlinkAnchors: Array<String>?
        get() = StringUtils.split(metadata.getOrDefault(Name.ANCHORS, ""), "\n")

    var cash: Float
        get() = metadata.getFloat(Name.CASH_KEY, 0.0f)
        set(cash) = metadata.set(Name.CASH_KEY, cash.toString())


}

fun main(args: Array<String>) {
    val page = KWebPage(GoraWebPage(EXAMPLE_URL))
    page.content
}
