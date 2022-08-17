package ai.platon.pulsar.persist.model.experimental

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.metadata.Name
import org.apache.gora.util.ByteUtils
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

class KWebPageExt(
    val page: KWebPage
) {
    fun getContentAsBytes(): ByteArray {
        val content = page.content ?: return ByteUtils.toBytes('\u0000')
        return ByteUtils.toBytes(content)
    }

    /**
     * TODO: Encoding is always UTF-8?
     */
    fun getContentAsString(): String {
        return ByteUtils.toString(getContentAsBytes())
    }

    fun getContentAsInputStream(): ByteArrayInputStream {
        val contentInOctets = page.content ?: return ByteArrayInputStream(ByteUtils.toBytes('\u0000'))
        return ByteArrayInputStream(page.content!!.array(),
            contentInOctets.arrayOffset() + contentInOctets.position(),
            contentInOctets.remaining())
    }

    fun getContentAsSaxInputSource(): InputSource {
        val inputSource = InputSource(getContentAsInputStream())
        val encoding = page.getEncoding()
        if (encoding != null) {
            inputSource.encoding = encoding
        }
        return inputSource
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
            val url = KWebPage.u8(l.url)
            if (!links.contains(url)) {
                links.add(url)
            }
        }
        page.links = links
        page.impreciseLinkCount = links.size
    }

    fun addLinks(hypeLinks: Iterable<CharSequence>) {
        var links = page.links
        // If there are too many links, Drop the front 1/3 links
        if (links.size > AppConstants.MAX_LINK_PER_PAGE) {
            links = links.subList(links.size - AppConstants.MAX_LINK_PER_PAGE / 3, links.size)
        }
        for (link in hypeLinks) {
            val url = KWebPage.u8(link.toString())
            // Use a set?
            if (!links.contains(url)) {
                links.add(url)
            }
        }
        page.links = links
        page.impreciseLinkCount = links.size
    }

    fun increaseImpreciseLinkCount(count: Int) {
        page.impreciseLinkCount += count
    }

    fun sniffTitle(): String {
        return page.contentTitle?.takeUnless { it.isBlank() }
            ?: page.anchor.takeUnless { it.isBlank() }
            ?: page.pageTitle.takeUnless { it.isNullOrBlank() }
            ?: page.location.takeUnless { it.isBlank() }
            ?: page.url
    }

    fun putFetchTimeHistory(fetchTime: Instant) {
        var fetchTimeHistory = page.metadata[Name.FETCH_TIME_HISTORY]
        fetchTimeHistory = DateTimes.constructTimeHistory(fetchTimeHistory, fetchTime, 10)
        page.metadata[Name.FETCH_TIME_HISTORY] = fetchTimeHistory
    }

    fun sniffModifiedTime(): Instant {
        var modifiedTime = page.getModifiedTime()
        val headerModifiedTime = page.getHeaders().lastModified
        if (isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = headerModifiedTime
        }
        if (isValidContentModifyTime(page.contentModifiedTime) && page.contentModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = page.contentModifiedTime
        }
        if (isValidContentModifyTime(page.contentPublishTime) && page.contentPublishTime.isAfter(modifiedTime)) {
            modifiedTime = page.contentPublishTime
        }
        // A fix
        if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) { // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
            modifiedTime = Instant.now()
        }
        return modifiedTime
    }

    fun updateContentPublishTime(newPublishTime: Instant): Boolean {
        if (!isValidContentModifyTime(newPublishTime)) {
            return false
        }
        val lastPublishTime = page.contentPublishTime
        if (newPublishTime.isAfter(lastPublishTime)) {
            page.prevContentPublishTime = lastPublishTime
            page.contentPublishTime = newPublishTime
        }
        return true
    }

    fun getFirstCrawlTime(defaultValue: Instant): Instant {
        var firstCrawlTime: Instant? = null
        val fetchTimeHistory = page.getFetchTimeHistory("")
        if (!fetchTimeHistory.isEmpty()) {
            val times = fetchTimeHistory.split(",").toTypedArray()
            val time = DateTimes.parseInstant(times[0], Instant.EPOCH)
            if (time.isAfter(Instant.EPOCH)) {
                firstCrawlTime = time
            }
        }
        return firstCrawlTime ?: defaultValue
    }

    fun sniffFetchPriority(): Int {
        var priority = page.fetchPriority
        val depth = page.distance
        if (depth < AppConstants.FETCH_PRIORITY_DEPTH_BASE) {
            priority = Math.max(priority, AppConstants.FETCH_PRIORITY_DEPTH_BASE - depth)
        }
        return priority
    }

    fun updateDistance(newDistance: Int) {
        val oldDistance = page.distance
        if (newDistance < oldDistance) {
            page.distance = newDistance
        }
    }

    fun putIndexTimeHistory(indexTime: Instant?) {
        var indexTimeHistory = page.metadata[Name.INDEX_TIME_HISTORY]
        indexTimeHistory = DateTimes.constructTimeHistory(indexTimeHistory, indexTime!!, 10)
        page.metadata[Name.INDEX_TIME_HISTORY] = indexTimeHistory
    }

    fun getFirstIndexTime(defaultValue: Instant): Instant {
        var firstIndexTime: Instant? = null
        val indexTimeHistory = page.getIndexTimeHistory("")
        if (indexTimeHistory.isNotEmpty()) {
            val times = indexTimeHistory.split(",").toTypedArray()
            val time = DateTimes.parseInstant(times[0], Instant.EPOCH)
            if (time.isAfter(Instant.EPOCH)) {
                firstIndexTime = time
            }
        }
        return firstIndexTime ?: defaultValue
    }

    private fun isValidContentModifyTime(publishTime: Instant): Boolean {
        return publishTime.isAfter(AppConstants.MIN_ARTICLE_PUBLISH_TIME)
    }
}
