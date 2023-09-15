package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.common.DateTimes
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
data class NavigateEntry(
    /**
     * The url to navigate to.
     * If page.href exists, the url is the href, otherwise, the url is page.url.
     * The href has the higher priority to locate a resource.
     * */
    val url: String,
    /**
     * The page id, 0 means there is no WebPage.
     * */
    val pageId: Int = 0,
    /**
     * The page url which can be used to retrieve the WebPage from database.
     * An empty string means there is no WebPage.
     * */
    val pageUrl: String = "",
    /**
     * The referrer claimed by the page.
     */
    var pageReferrer: String? = null,
    /**
     * The location of the page, it shows in the browser window, can differ from url.
     */
    var location: String = url,
    /**
     * Indicate if the navigation is stopped.
     */
    var stopped: Boolean = false,
    /**
     * Indicate if the tab for this navigation is closed.
     */
    var closed: Boolean = false,
    /**
     * The last active time.
     */
    var lastActiveTime: Instant = Instant.now(),
    /**
     * The time when the object is created.
     */
    val createTime: Instant = Instant.now(),
): Comparable<NavigateEntry> {
    var mainRequestId = ""
    var mainRequestHeaders: Map<String, Any> = mapOf()
    var mainRequestCookies: List<Map<String, String>> = listOf()
    var mainResponseStatus: Int = -1
    var mainResponseStatusText: String = ""
    var mainResponseHeaders: Map<String, Any> = mapOf()

    /**
     * The time when the document is ready.
     */
    var documentReadyTime = DateTimes.doomsday
    /**
     * Track the time of page actions.
     */
    val actionTimes = mutableMapOf<String, Instant>()
    
    val networkRequestCount = AtomicInteger()
    
    val networkResponseCount = AtomicInteger()
    /**
     * Refresh the entry with the given action.
     * */
    fun refresh(action: String) {
        val now = Instant.now()
        lastActiveTime = now
        if (action.isNotBlank()) {
            actionTimes[action] = now
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return when (other) {
            null -> false
            is String -> other == url
            is NavigateEntry -> other.url == url
            else -> false
        }
    }

    override fun hashCode() = url.hashCode()

    override fun compareTo(other: NavigateEntry) = url.compareTo(other.url)
}
