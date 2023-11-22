package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.common.DateTimes
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val lock = ReentrantLock()

    /**
     * Main request is only used for HTML documents for now.
     *
     * For HTML webpages, the main request is the request for the first HTML document.
     * TODO: redirection requests are not main requests.
     * */
    var mainRequestId = ""
    var mainRequestHeaders: Map<String, Any> = mapOf()
    var mainRequestCookies: List<Map<String, String>> = listOf()
    /**
     * The response status of the main request
     * */
    var mainResponseStatus: Int = -1
    var mainResponseStatusText: String = ""
    var mainResponseHeaders: Map<String, Any> = mapOf()

    val documentTransferred get() = mainResponseStatus > 0

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

    fun synchronized(action: () -> Unit) {
        lock.withLock(action)
    }

    fun updateMainRequest(requestId: String, headers: Map<String, Any>) {
        mainRequestId = requestId
        mainRequestHeaders = headers

        // amazon.com uses "referer" instead of "referrer" in the request header,
        // not clear if other sites uses the other one
        val referrer = pageReferrer
        if (referrer != null) {
            val mutableHeaders = headers.toMutableMap()
            mutableHeaders["referer"] = referrer
            mutableHeaders["referrer"] = referrer
            mainRequestHeaders = mutableHeaders
        }
    }
    
    fun updateMainResponse(status: Int, statusText: String, headers: Map<String, Any>) {
        mainResponseStatus = status
        mainResponseStatusText = statusText
        mainResponseHeaders = headers
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
