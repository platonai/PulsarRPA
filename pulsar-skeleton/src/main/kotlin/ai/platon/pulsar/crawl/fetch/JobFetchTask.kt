package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.crawl.fetch.data.PoolId
import ai.platon.pulsar.persist.WebPage
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

interface IFetchEntry {
    var reservedUrl: String
    var page: WebPage
}

/**
 * This class described the item to be fetched.
 */
class JobFetchTask(
        jobID: Int,
        val priority: Int,
        val protocol: String,
        val host: String,
        val page: WebPage,
        val url: URL
) : Comparable<JobFetchTask> {
    val key = Key(jobID, priority, protocol, host, url.toString())
    val itemId get() = key.itemId
    val urlString get() = key.url
    val poolId get() = PoolId(priority, protocol, host)
    var pendingStart = Instant.EPOCH!!

    override fun toString(): String {
        return "<itemId=" + key.itemId + ", priority=" + key.priority + ", url=" + key.url + ">"
    }

    override fun compareTo(other: JobFetchTask): Int {
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
        fun create(jobId: Int, priority: Int, url: String, page: WebPage, groupMode: URLUtil.GroupMode): JobFetchTask? {
            val u = Urls.getURLOrNull(url) ?: return null

            val proto = u.protocol
            val host = URLUtil.getHost(u, groupMode)

            return if (proto == null || host == null || host.isEmpty()) {
                // TODO: report the exception
                null
            } else JobFetchTask(jobId, priority, proto.toLowerCase(), host.toLowerCase(), page, u)
        }
    }
}

/**
 * Created by vincent on 16-10-15.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
data class UrlStat(
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
        var urlsFromSeed: Int = 0,
        var pageViews: Int = 0
) : Comparable<UrlStat> {

    override fun compareTo(other: UrlStat): Int {
        val reverseHost = Urls.reverseHost(hostName)
        val reverseHost2 = Urls.reverseHost(other.hostName)

        return reverseHost.compareTo(reverseHost2)
    }
}

data class BatchStat(
        var numTasksSuccess: Long = 0,
        var totalSuccessBytes: Long = 0L
) {
    var startTime = Instant.now()
    val elapsedTime get() = Duration.between(startTime, Instant.now())

    val timePerPage get() = elapsedTime.dividedBy(1 + numTasksSuccess)
    val bytesPerPage get() = 1.0 * totalSuccessBytes / (0.1 + numTasksSuccess)
    val pagesPerSecond get() = numTasksSuccess / (0.1 + elapsedTime.seconds)
    val bytesPerSecond get() = 1.0 * totalSuccessBytes / (0.1 + elapsedTime.seconds)
}
