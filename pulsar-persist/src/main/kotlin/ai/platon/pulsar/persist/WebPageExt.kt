package ai.platon.pulsar.persist

import ai.platon.pulsar.common.DateTimes.constructTimeHistory
import ai.platon.pulsar.common.DateTimes.parseInstant
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.impl.WebPageImpl
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.ActiveDOMStat
import ai.platon.pulsar.persist.model.ActiveDOMStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

class WebPageExt(private val page: WebPage) {

    companion object {

        fun newTestWebPage(url: String): WebPage {
            val page = WebPageImpl.newWebPage(url, VolatileConfig(), null)

            page.activeDOMStatus = ActiveDOMStatus(1, 1, "1", "1", "1")
            page.activeDOMStatTrace = mapOf("a" to ActiveDOMStat(), "b" to ActiveDOMStat())
            page.ensurePageModel().emplace(1, "g", mapOf("a" to "b"))

            return page
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

    fun addLinks(hypeLinks: Iterable<CharSequence>) {
        var links = page.links

        // If there are too many links, Drop the front 1/3 links
        if (links.size > AppConstants.MAX_LINK_PER_PAGE) {
            links = links.subList(links.size - AppConstants.MAX_LINK_PER_PAGE / 3, links.size)
        }

        for (link in hypeLinks) {
            val url = WebPageImpl.u8(link.toString())
            // Use a set?
            if (!links.contains(url)) {
                links.add(url)
            }
        }

        page.links = links
    }

    fun updateContent(pageDatum: PageDatum, contentTypeHint: String? = null) {
        var contentType = contentTypeHint

        page.setOriginalContentLength(pageDatum.originalContentLength)
        page.setContent(pageDatum.content)
        // clear content immediately to release resource as soon as possible
        pageDatum.content = null

        if (contentType != null) {
            pageDatum.contentType = contentType
        } else {
            contentType = pageDatum.contentType
        }

        if (contentType != null) {
            page.contentType = contentType
        } else {

        }
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

    fun sniffModifiedTime(): Instant {
        var modifiedTime = page.modifiedTime
        val headerModifiedTime = page.headers.lastModified
        if (page.isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
            modifiedTime = headerModifiedTime
        }
        // A fix
        if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
            modifiedTime = Instant.now()
        }
        return modifiedTime
    }
}
