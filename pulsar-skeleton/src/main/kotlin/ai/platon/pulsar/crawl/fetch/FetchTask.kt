package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.URLUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.persist.WebPage
import java.net.URL
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

interface IFetchEntry {
    var reservedUrl: String
    var page: WebPage
}

/**
 * This class described the item to be fetched.
 */
class FetchTask(
        jobID: Int,
        priority: Int,
        protocol: String,
        host: String,
        var page: WebPage?,
        u: URL
) : Comparable<FetchTask> {
    val key: Key = Key(jobID, priority, protocol, host, u.toString())
    var pendingStart = Instant.EPOCH

    val priority: Int get() = key.priority
    val protocol: String get() = key.protocol
    val host: String get() = key.host
    val itemId: Int get() = key.itemId
    val url: String get() = key.url

    override fun toString(): String {
        return "[itemId=" + key.itemId + ", priority=" + key.priority + ", url=" + key.url + "]"
    }

    override fun compareTo(other: FetchTask): Int {
        return key.compareTo(other.key)
    }

    data class Key(
            var jobID: Int,
            var priority: Int,
            var protocol: String,
            var host: String,
            var url: String,
            var itemId: Int = nextId()
    ) : Comparable<Key> {
        override fun compareTo(other: Key): Int {
            return itemId - other.itemId
        }
    }

    companion object {
        /**
         * The initial value is the current timestamp in second, to make it
         * an unique id in the current host
         */
        private val instanceSequence = AtomicInteger(0)

        /**
         * Generate an unique numeric id for the fetch item
         * fetch item id is used to easy item searching
         */
        private fun nextId(): Int {
            return instanceSequence.incrementAndGet()
        }

        /**
         * Create an item. Queue id will be created based on `groupMode`
         * argument, either as a protocol + hostname pair, protocol + IP
         * address pair or protocol+domain pair.
         */
        fun create(jobId: Int, priority: Int, url: String, page: WebPage, groupMode: URLUtil.GroupMode): FetchTask? {
            val u = Urls.getURLOrNull(url) ?: return null

            val proto = u.protocol
            val host = URLUtil.getHost(u, groupMode)

            return if (proto == null || host.isEmpty()) {
                null
            } else FetchTask(jobId, priority, proto.toLowerCase(), host.toLowerCase(), page, u)

        }
    }
}

/**
 * Created by vincent on 16-10-15.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
data class FetchStatus(
        var hostName: String,
        var urls: Int = 0,
        var indexUrls: Int = 0,
        var detailUrls: Int = 0,
        var searchUrls: Int = 0,
        var mediaUrls: Int = 0,
        var bbsUrls: Int = 0,
        var blogUrls: Int = 0,
        var tiebaUrls: Int = 0,
        var unknownUrls: Int = 0,
        var urlsTooLong: Int = 0,
        var urlsFromSeed: Int = 0
) : Comparable<FetchStatus> {

    override fun compareTo(other: FetchStatus): Int {
        val reverseHost = Urls.reverseHost(hostName)
        val reverseHost2 = Urls.reverseHost(other.hostName)

        return reverseHost.compareTo(reverseHost2)
    }
}
