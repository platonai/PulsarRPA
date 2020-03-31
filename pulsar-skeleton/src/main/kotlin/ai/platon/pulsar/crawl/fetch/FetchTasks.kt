package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Iterables
import io.netty.util.concurrent.Future
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

abstract class TaskHandler: (WebPage) -> Unit {
    abstract override operator fun invoke(page: WebPage)
}

abstract class AfterTaskHandler: (FetchResult) -> Unit {
    abstract override operator fun invoke(page: FetchResult)
}

abstract class BatchHandler: (Iterable<WebPage>) -> Unit {
    abstract override operator fun invoke(pages: Iterable<WebPage>)
}

abstract class AfterBatchHandler: (Iterable<FetchResult>) -> Unit {
    abstract override operator fun invoke(pages: Iterable<FetchResult>)
}

class FetchTask(
        val batchId: Int,
        val priority: Int,
        val page: WebPage,
        val volatileConfig: VolatileConfig,
        val id: Int = instanceSequence.incrementAndGet(),
        val batchSize: Int = 1,
        val batchTaskId: Int = 0,
        var batchStat: BatchStat? = null,
        var proxyEntry: ProxyEntry? = null, // the proxy used
        var nRetries: Int = 0, // The number retries inside a privacy context
        private val canceled: AtomicBoolean = AtomicBoolean() // whether this task is canceled
): Comparable<FetchTask> {
    lateinit var response: Response

    val url get() = page.url
    val domain get() = URLUtil.getDomainName(url)
    val isCanceled get() = canceled.get()

    fun reset() {
        batchStat = null
        proxyEntry = null
        nRetries = 0
        canceled.set(false)
    }

    fun cancel() {
        canceled.set(true)
    }

    fun clone(): FetchTask {
        return FetchTask(
                batchId = batchId,
                batchTaskId = batchTaskId,
                batchSize = batchSize,
                priority = priority,
                page = page,
                volatileConfig = volatileConfig
        )
    }

    override fun compareTo(other: FetchTask): Int {
        return url.compareTo(other.url)
    }

    override fun toString(): String {
        return "$batchTaskId/$batchId"
    }

    companion object {
        val NIL = FetchTask(0, 0, WebPage.NIL, VolatileConfig.EMPTY, id = 0)
        private val instanceSequence = AtomicInteger(0)
    }
}

class FetchResult(
        val task: FetchTask,
        var response: Response,
        var exception: Exception? = null
) {
    operator fun component1() = task
    operator fun component2() = response
    operator fun component3() = exception

    val status get() = response.status
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

    val priority = conf.getUint(CapabilityTypes.BROWSER_DRIVER_PRIORITY, 0)
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
    val workingTasks = mutableMapOf<FetchTask, Future<FetchResult>>()
    /**
     * The finished tasks in the sub context
     * */
    val finishedTasks = mutableMapOf<String, FetchResult>()
    /**
     * The privace leaked tasks in the sub context
     * */
    val privacyLeakedTasks = mutableListOf<FetchTask>()
    /**
     * The aborted tasks in the sub context
     * */
    val abortedTasks = mutableListOf<FetchTask>()

    val beforeFetchHandler: TaskHandler? = prevNode?.beforeFetchHandler?:conf.getBean(FETCH_BEFORE_FETCH_HANDLER, TaskHandler::class.java)
    val afterFetchHandler: TaskHandler? = prevNode?.afterFetchHandler?:conf.getBean(FETCH_AFTER_FETCH_HANDLER, TaskHandler::class.java)
    val afterFetchNHandler: BatchHandler? = prevNode?.afterFetchNHandler?:conf.getBean(FETCH_AFTER_FETCH_N_HANDLER, BatchHandler::class.java)
    val beforeFetchAllHandler: BatchHandler? = prevNode?.beforeFetchAllHandler?:conf.getBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, BatchHandler::class.java)
    val afterFetchAllHandler: BatchHandler? = prevNode?.afterFetchAllHandler?:conf.getBean(FETCH_AFTER_FETCH_BATCH_HANDLER, BatchHandler::class.java)

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
            numTasksFailed > numAllowedFailures -> State.TOO_MANY_FAILURE
            idleSeconds > idleTimeout.seconds -> State.IDLE_TIMEOUT
            connectionLost -> State.CONNECTION_LOST
            isLastTaskTimeout() -> State.LAST_TASK_TIMEOUT
            else -> State.RUNNING
        }
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
        lastSuccessProxy = result.task.proxyEntry
        numTasksSuccess++

        universalSuccessTasks[task.url] = result
        universalSuccessPages.add(task.page)
        universalStat.totalSuccessBytes += result.response.length
        universalStat.numTasksSuccess++

        privacyContext.informSuccess()

        afterFetchN(headNode.universalSuccessPages)
    }

    fun onFailure(task: FetchTask, result: FetchResult) {
        finishedTasks[task.url] = result

        lastFailedTask = task
        lastFailedProxy = task.proxyEntry
        ++numTasksFailed
    }

    fun onRetry(task: FetchTask, result: FetchResult) {
        onFailure(task, result)

        if (result.status.isRetry(RetryScope.PRIVACY)) {
            privacyLeakedTasks.add(task)
            privacyContext.informWarning()
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

    fun beforeFetch(page: WebPage) {
        try {
            beforeFetchHandler?.invoke(page)
        } catch (e: Throwable) {}
    }

    fun afterFetch(page: WebPage) {
        try {
            afterFetchHandler?.invoke(page)
        } catch (e: Throwable) {}
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

class FetchTaskStream(
        val pages: Sequence<WebPage>,
        val conf: VolatileConfig,
        val privacyContext: PrivacyContext,
        var prevNode: FetchTaskBatch? = null
): AutoCloseable {

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
