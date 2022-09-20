package ai.platon.pulsar.persist

import ai.platon.pulsar.common.DateTimes.constructTimeHistory
import ai.platon.pulsar.common.DateTimes.parseInstant
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.ActiveDOMStat
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

class WebPageExt(private val page: WebPage) {

    companion object {

        fun newTestWebPage(url: String): WebPage {
            val page = WebPage.newWebPage(url, VolatileConfig(), null)

            page.vividLinks = mapOf("$url?t=a" to "a", "$url?t=b" to "b")
            page.activeDOMStatus = ActiveDOMStatus(1, 1, "1", "1", "1")
            page.activeDOMStatTrace = mapOf("a" to ActiveDOMStat(), "b" to ActiveDOMStat())
            page.ensurePageModel().emplace(1, "g", mapOf("a" to "b"))

            return page
        }

    }

    fun increaseDistance(newDistance: Int) {
        val oldDistance: Int = page.distance
        if (newDistance < oldDistance) {
            page.distance = newDistance
        }
    }

    fun sniffTitle(): String {
        var title = page.contentTitle
        if (title.isEmpty()) {
            title = page.anchor.toString()
        }
        if (title.isEmpty()) {
            title = page.pageTitle
        }
        if (title.isEmpty()) {
            title = page.location
        }
        if (title.isEmpty()) {
            title = page.url
        }
        return title
    }

    fun setTextCascaded(text: String?) {
        page.setContent(text)
        page.setContentText(text)
        page.setPageText(text)
    }

    /**
     * Record all links appeared in a page
     * The links are in FIFO order, for each time we fetch and parse a page,
     * we push newly discovered links to the queue, if the queue is full, we drop out some old ones,
     * usually they do not appears in the page any more.
     *
     * TODO: compress links
     * TODO: HBase seems not modify any nested array
     *
     * @param hypeLinks a [java.lang.Iterable] object.
     */
    fun addHyperlinks(hypeLinks: Iterable<HyperlinkPersistable>) {
        var links = page.links

        // If there are too many links, Drop the front 1/3 links
        if (links.size > AppConstants.MAX_LINK_PER_PAGE) {
            links = links.subList(links.size - AppConstants.MAX_LINK_PER_PAGE / 3, links.size)
        }

        for (l in hypeLinks) {
            val url = WebPage.u8(l.url)
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
            val url = WebPage.u8(link.toString())
            // Use a set?
            if (!links.contains(url)) {
                links.add(url)
            }
        }

        page.links = links
        page.impreciseLinkCount = links.size
    }

    fun updateContentPublishTime(newPublishTime: Instant): Boolean {
        if (!page.isValidContentModifyTime(newPublishTime)) {
            return false
        }

        val lastPublishTime = page.contentPublishTime
        if (newPublishTime.isAfter(lastPublishTime)) {
            page.prevContentPublishTime = lastPublishTime
            page.contentPublishTime = newPublishTime
        }

        return true
    }

    fun updateContentModifiedTime(newModifiedTime: Instant): Boolean {
        if (!page.isValidContentModifyTime(newModifiedTime)) {
            return false
        }
        val lastModifyTime = page.contentModifiedTime
        if (newModifiedTime.isAfter(lastModifyTime)) {
            page.prevContentModifiedTime = lastModifyTime
            page.contentModifiedTime = newModifiedTime
        }
        return true
    }

    fun updateRefContentPublishTime(newRefPublishTime: Instant): Boolean {
        if (!page.isValidContentModifyTime(newRefPublishTime)) {
            return false
        }
        val latestRefPublishTime = page.refContentPublishTime
        if (newRefPublishTime.isAfter(latestRefPublishTime)) {
            page.prevRefContentPublishTime = latestRefPublishTime
            page.refContentPublishTime = newRefPublishTime
            return true
        }
        return false
    }

    fun getFirstIndexTime(defaultValue: Instant): Instant {
        var firstIndexTime: Instant? = null
        val indexTimeHistory = page.getIndexTimeHistory("")
        if (!indexTimeHistory.isEmpty()) {
            val times = indexTimeHistory.split(",").toTypedArray()
            val time = parseInstant(times[0], Instant.EPOCH)
            if (time.isAfter(Instant.EPOCH)) {
                firstIndexTime = time
            }
        }
        return firstIndexTime ?: defaultValue
    }

    /**
     * *****************************************************************************
     * Parsing
     * ******************************************************************************
     */
    fun updateFetchTimeHistory(fetchTime: Instant) {
        var fetchTimeHistory = page.metadata[Name.FETCH_TIME_HISTORY]
        fetchTimeHistory = constructTimeHistory(fetchTimeHistory, fetchTime, 10)
        page.metadata[Name.FETCH_TIME_HISTORY] = fetchTimeHistory
    }

    fun updateFetchTime(prevFetchTime: Instant?, fetchTime: Instant) {
        page.prevFetchTime = prevFetchTime!!
        // the next time supposed to fetch
        page.fetchTime = fetchTime
        updateFetchTimeHistory(fetchTime)
    }

    /**
     * Get the first fetch time
     */
    val firstFetchTime: Instant?
        get() {
            var firstFetchTime: Instant? = null
            val history = page.getFetchTimeHistory("")
            if (!history.isEmpty()) {
                val times = history.split(",").toTypedArray()
                val time = parseInstant(times[0], Instant.EPOCH)
                if (time.isAfter(Instant.EPOCH)) {
                    firstFetchTime = time
                }
            }
            return firstFetchTime
        }

    fun sniffModifiedTime(): Instant {
        var modifiedTime = page.modifiedTime
        val headerModifiedTime = page.headers.lastModified
        val contentModifiedTime = page.contentModifiedTime
        if (page.isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = headerModifiedTime
        }
        if (page.isValidContentModifyTime(contentModifiedTime) && contentModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = contentModifiedTime
        }
        val contentPublishTime = page.contentPublishTime
        if (page.isValidContentModifyTime(contentPublishTime) && contentPublishTime.isAfter(modifiedTime)) {
            modifiedTime = contentPublishTime
        }

        // A fix
        if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
            modifiedTime = Instant.now()
        }
        return modifiedTime
    }
}
