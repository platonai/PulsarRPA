package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.data.PoolId
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
            var itemId: Int = instanceSequence.incrementAndGet()
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
