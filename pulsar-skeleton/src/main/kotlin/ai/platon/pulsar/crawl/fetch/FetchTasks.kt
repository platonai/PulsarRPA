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

class FetchTaskBatch(
        val batchId: Int,
        val pages: Iterable<WebPage>,
        val conf: VolatileConfig,
        val privacyContext: PrivacyContext,
        var prevNode: FetchTaskBatch? = null
): AutoCloseable {
    enum class State {
        RUNNING,
        ALL_DONE,
        TOO_MANY_FAILURE,
        IDLE_TIMEOUT,
        CONNECTION_LOST,
        LAST_TASK_TIMEOUT,
        PRIVACY_LEAK,
        INTERRUPTED,
        CLOSED
    }

    private val log = LoggerFactory.getLogger(FetchTaskBatch::class.java)!!

    var proxyEntry: ProxyEntry? = null
    val priority = conf.getUint(CapabilityTypes.BROWSER_WEB_DRIVER_PRIORITY, 0)
    // The function must return in a reasonable time
    val threadTimeout = conf.getDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT).plusSeconds(5)
    val idleTimeout = threadTimeout.plusSeconds(5)
    val batchSize = Iterables.size(pages)
    val numAllowedFailures = max(10, batchSize / 3)

    /**
     * The universal success tasks
     * */
    val universalSuccessTasks: MutableMap<String, FetchResult> = prevNode?.universalSuccessTasks?:mutableMapOf()
    /**
     * The universal success pages
     * */
    val universalSuccessPages: MutableList<WebPage> = prevNode?.universalSuccessPages?:mutableListOf()
    /**
     * The universal statistics
     * */
    val universalStat: BatchStat = prevNode?.universalStat?:BatchStat()
    /**
     * The submitted tasks in the sub context
     * */
    val workingTasks = mutableMapOf<FetchTask, Deferred<FetchResult>>()
    /**
     * The finished tasks in the sub context
     * */
    val finishedTasks = mutableMapOf<String, FetchResult>()
    /**
     * The privacy leaked tasks in the sub context
     * */
    val privacyLeakedTasks = mutableListOf<FetchTask>()
    /**
     * The aborted tasks in the sub context
     * */
    val abortedTasks = mutableListOf<FetchTask>()

    val afterFetchNHandler: WebPageBatchHandler? = prevNode?.afterFetchNHandler?:conf.getBean(FETCH_AFTER_FETCH_N_HANDLER, WebPageBatchHandler::class.java)
    val beforeFetchAllHandler: WebPageBatchHandler? = prevNode?.beforeFetchAllHandler?:conf.getBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, WebPageBatchHandler::class.java)
    val afterFetchAllHandler: WebPageBatchHandler? = prevNode?.afterFetchAllHandler?:conf.getBean(FETCH_AFTER_FETCH_BATCH_HANDLER, WebPageBatchHandler::class.java)

    val numFinishedTasks get() = finishedTasks.size
    val numWorkingTasks get() = workingTasks.size

    // Submit all tasks
    var round = 0L
    var idleSeconds = 0
    // external connection is lost, might be caused by proxy
    var connectionLost = false
    var numTasksDone = 0
    var numTasksSuccess = 0
    var numTasksFailed = 0
    var lastSuccessTask: FetchTask? = null
    var lastFailedTask: FetchTask? = null
    var lastSuccessProxy: ProxyEntry? = null
    var lastFailedProxy: ProxyEntry? = null

    var nextNode: FetchTaskBatch? = null

    val headNode: FetchTaskBatch
        get() {
        var r = this
        while (r.prevNode != null) {
            r = r.prevNode!!
        }
        return r
    }

    val tailNode: FetchTaskBatch
        get() {
        var r = this
        while (r.nextNode != null) {
            r = r.nextNode!!
        }
        return r
    }

    val isHead get() = prevNode == null
    val isTail get() = nextNode == null

    fun checkState(): State {
        return when {
            numTasksDone >= batchSize -> State.ALL_DONE
            // Since the urls in the batch are usually in the same domain
            // if there are too many failure, the rest tasks are very likely run to failure too
            numTasksFailed > numAllowedFailures -> State.TOO_MANY_FAILURE
            idleSeconds > idleTimeout.seconds -> State.IDLE_TIMEOUT
            connectionLost -> State.CONNECTION_LOST
            isLastTaskTimeout() -> State.LAST_TASK_TIMEOUT
            else -> State.RUNNING
        }
    }

    fun createTasks(): List<FetchTask> {
        return pages.mapIndexed { i, page -> FetchTask(batchId, priority, page, conf, batchSize, batchTaskId = i) }
    }

    /**
     * Create a child context to re-fetch aborted pages and privacy leaked pages
     * */
    fun createNextNode(newPrivacyContext: PrivacyContext): FetchTaskBatch {
        log.info("Creating sub-batch of #{}, leaked: {} aborted: {} success: {} finished: {} expect: {}",
                batchId, privacyLeakedTasks.size, abortedTasks.size, universalSuccessPages.size, numFinishedTasks, batchSize)

        val capacity = privacyLeakedTasks.size + abortedTasks.size
        val retryPages = ArrayList<WebPage>(capacity)
        privacyLeakedTasks.mapTo(retryPages) { it.page }
        abortedTasks.asSequence().filter { it in privacyLeakedTasks }.mapTo(retryPages) { it.page }

        return FetchTaskBatch(batchId, retryPages, conf, newPrivacyContext, prevNode = this).also { nextNode = it }
    }

    fun onSuccess(task: FetchTask, result: FetchResult) {
        finishedTasks[task.url] = result

        lastSuccessTask = result.task
        lastSuccessProxy = result.response.pageDatum.proxyEntry
        numTasksSuccess++

        universalSuccessTasks[task.url] = result
        universalSuccessPages.add(task.page)
        universalStat.totalSuccessBytes += result.response.length
        universalStat.numTasksSuccess++

        afterFetchN(headNode.universalSuccessPages)
    }

    fun onFailure(task: FetchTask, result: FetchResult) {
        finishedTasks[task.url] = result

        lastFailedTask = task
        lastFailedProxy = result.response.pageDatum.proxyEntry
        ++numTasksFailed
    }

    fun onRetry(task: FetchTask, result: FetchResult) {
        onFailure(task, result)

        if (result.status.isRetry(RetryScope.PRIVACY)) {
            privacyLeakedTasks.add(task)
        }
    }

    fun onAbort(task: FetchTask, result: FetchResult) {
        onFailure(task, result)
        abortedTasks.add(task)
    }

    fun collectResponses(): List<Response> {
        val tail = tailNode
        return headNode.pages.map { getResponse(it, tail) }
    }

    fun afterFetchN(pages: Iterable<WebPage>) {
        try {
            afterFetchNHandler?.invoke(pages)
        } catch (e: Throwable) {}
    }

    fun beforeFetchAll(pages: Iterable<WebPage>) {
        try {
            beforeFetchAllHandler?.invoke(pages)
        } catch (e: Throwable) {}
    }

    fun afterFetchAll(pages: Iterable<WebPage>) {
        try {
            afterFetchAllHandler?.invoke(pages)
        } catch (e: Throwable) {}
    }

    fun clear() {
        if (isHead) {
            var n: FetchTaskBatch? = this
            while (n != null) {
                clearNode(n)
                n = n.nextNode
            }
        }
    }

    override fun close() {
        clear()
    }

    private fun clearNode(node: FetchTaskBatch) {
        node.workingTasks.clear()
        node.abortedTasks.clear()
        node.privacyLeakedTasks.clear()
        node.finishedTasks.clear()

        if (node.isHead) {
            node.universalSuccessPages.clear()
            node.universalSuccessTasks.clear()
        }
    }

    private fun getResponse(page: WebPage, tail: FetchTaskBatch): Response {
        val url = page.url
        val result = universalSuccessTasks[url]?:tail.finishedTasks[url]
        return result?.response?:ForwardingResponse.retry(page, RetryScope.CRAWL)
    }

    private fun isLastTaskTimeout(): Boolean {
        return batchSize > 10 && workingTasks.size == 1 && idleSeconds >= 45
    }
}
