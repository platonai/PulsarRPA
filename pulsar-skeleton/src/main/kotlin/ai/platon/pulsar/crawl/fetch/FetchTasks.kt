package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.urls.Urls
import ai.platon.pulsar.crawl.WebPageBatchHandler
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Iterables
import kotlinx.coroutines.Deferred
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

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

class FetchTask(
        val batchId: Int,
        val priority: Int,
        val page: WebPage,
        val volatileConfig: VolatileConfig,
        val batchSize: Int = 1,
        val batchTaskId: Int = 0,
        var batchStat: BatchStat? = null,
        // The task id
        val id: Int = instanceSequencer.incrementAndGet(),
        var nRetries: Int = 0 // The total number retries in a crawl
): Comparable<FetchTask> {
    enum class State { NOT_READY, READY, WORKING, CANCELED, DONE }
    val state = AtomicReference(State.NOT_READY)

    var proxyEntry: ProxyEntry? = null

    val url get() = page.url
    val href get() = page.href
    val domain get() = URLUtil.getDomainName(url)
    val isCanceled get() = state.get() == State.CANCELED
    val isWorking get() = state.get() == State.WORKING

    // A task is ready when it about to enter a privacy context
    fun markReady() = state.set(State.READY)
    // A task is working when it enters the the web driver
    fun startWork() = state.set(State.WORKING)
    fun cancel() = state.set(State.CANCELED)
    // A task is done if it exit from a privacy context
    fun done() = state.set(State.DONE)

    fun reset() {
        batchStat = null
        proxyEntry = null
        state.set(State.NOT_READY)
    }

    fun clone(): FetchTask {
        return FetchTask(
                batchId = batchId,
                batchTaskId = batchTaskId,
                batchSize = batchSize,
                priority = priority,
                page = page,
                volatileConfig = volatileConfig,
                nRetries = nRetries
        )
    }

    override fun compareTo(other: FetchTask): Int = id.compareTo(other.id)

    override fun equals(other: Any?): Boolean = other is FetchTask && id == other.id

    override fun hashCode(): Int = id

    override fun toString(): String = "$id"

    companion object {
        val NIL = FetchTask(0, 0, WebPage.NIL, VolatileConfig.EMPTY, id = 0)
        val instanceSequencer = AtomicInteger()
    }
}

class FetchResult(
        val task: FetchTask,
        var response: Response,
        var exception: Throwable? = null
) {
    operator fun component1() = task
    operator fun component2() = response
    operator fun component3() = exception

    val status get() = response.status
    val isSuccess get() = status.isSuccess
    val isPrivacyRetry get() = status.isRetry(RetryScope.PRIVACY)
    val isCrawlRetry get() = status.isRetry(RetryScope.CRAWL)
    val isSmall get() = status.args[ProtocolStatus.ARG_RETRY_REASON] == HtmlIntegrity.TOO_SMALL.toString()

    fun canceled() {
        response = ForwardingResponse.canceled(task.page)
    }

    fun retry(retryScope: RetryScope) {
        response = ForwardingResponse.retry(task.page, retryScope)
    }

    fun failed(t: Throwable?) {
        response = ForwardingResponse.failed(task.page, t)
        exception = t
    }

    companion object {
        fun unchanged(task: FetchTask) = FetchResult(task, ForwardingResponse.unchanged(task.page))
        fun unfetched(task: FetchTask) = FetchResult(task, ForwardingResponse.unfetched(task.page))
        fun canceled(task: FetchTask) = FetchResult(task, ForwardingResponse.canceled(task.page))
        fun retry(task: FetchTask, retryScope: RetryScope) = FetchResult(task, ForwardingResponse.retry(task.page, retryScope))

        fun privacyRetry(task: FetchTask) = retry(task, RetryScope.PRIVACY)
        fun privacyRetry(task: FetchTask, reason: Exception) = FetchResult(task, ForwardingResponse.privacyRetry(task.page, reason))

        fun crawlRetry(task: FetchTask) = FetchResult(task, ForwardingResponse.crawlRetry(task.page))
        fun crawlRetry(task: FetchTask, reason: Exception) = FetchResult(task, ForwardingResponse.crawlRetry(task.page, reason))

        fun failed(task: FetchTask, e: Throwable?) = FetchResult(task, ForwardingResponse.failed(task.page, e))
    }
}
