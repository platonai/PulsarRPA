package ai.platon.pulsar.crawl.fetch.driver

import java.time.Instant

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
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
     * The referer claimed by the page.
     */
    var pageReferrer: String? = null,
    /**
     * The location of the page, it shows in the browser window, can differ from url.
     */
    var location: String = url,
    /**
     * Indicate if the driver be stopped.
     */
    var stopped: Boolean = false,
    /**
     * The last active time.
     */
    var lastActiveTime: Instant = Instant.now(),
    /**
     * The time when the object is created.
     */
    val createTime: Instant = Instant.now(),
): Comparable<NavigateEntry> {
    /**
     * The time when the document is ready.
     */
    var documentReadyTime = Instant.MAX
    /**
     * Track the time of page actions.
     */
    val actionTimes = mutableMapOf<String, Instant>()
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
