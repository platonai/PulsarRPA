package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.BatchStatus
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.net.browser.FetchResult
import ai.platon.pulsar.net.browser.FetchTask
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

abstract class TaskHandler: (WebPage) -> Unit {
    abstract override operator fun invoke(page: WebPage)
}

abstract class BatchHandler: (Iterable<WebPage>) -> Unit {
    abstract override operator fun invoke(pages: Iterable<WebPage>)
}

internal class BatchFetchContext(
        val batchId: Int,
        val pages: Iterable<WebPage>,
        val volatileConfig: VolatileConfig
) {
    val startTime = Instant.now()
    val priority = volatileConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
    // The function must return in a reasonable time
    val threadTimeout = volatileConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT).plusSeconds(10)
    val interval = Duration.ofSeconds(1)
    val idleTimeout = Duration.ofMinutes(2)
    val numTotalTasks = Iterables.size(pages)
    val numAllowedFailures = max(10, numTotalTasks / 3)

    // All submitted tasks
    val pendingTasks = mutableMapOf<String, Future<FetchResult>>()
    val finishedTasks = mutableMapOf<String, FetchResult>()
    val successPages = mutableListOf<WebPage>()

    val numFinishedTasks get() = finishedTasks.size
    val numPendingTasks get() = pendingTasks.size

    // Submit all tasks
    var round: Int = 0

    val status = BatchStatus()
    var idleSeconds = 0

    var beforeFetchHandler = volatileConfig.getBean(FETCH_BEFORE_FETCH_HANDLER, TaskHandler::class.java)
    var afterFetchHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_HANDLER, TaskHandler::class.java)
    var afterFetchNHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_N_HANDLER, BatchHandler::class.java)
    var beforeFetchAllHandler = volatileConfig.getBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, BatchHandler::class.java)
    var afterFetchAllHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_BATCH_HANDLER, BatchHandler::class.java)

    fun beforeFetch(page: WebPage) {
        beforeFetchHandler?.invoke(page)
    }

    fun afterFetch(page: WebPage) {
        afterFetchHandler?.invoke(page)
    }

    fun afterFetchN(pages: Iterable<WebPage>) {
        afterFetchNHandler?.invoke(pages)
    }

    fun beforeFetchAll(pages: Iterable<WebPage>) {
        beforeFetchAllHandler?.invoke(pages)
    }

    fun afterFetchAll(pages: Iterable<WebPage>) {
        afterFetchAllHandler?.invoke(pages)
    }
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
class SeleniumFetchComponent(
        private val executor: GlobalExecutor,
        private val seleniumEngine: SeleniumEngine,
        private val immutableConfig: ImmutableConfig
): AutoCloseable {
    val log = LoggerFactory.getLogger(SeleniumFetchComponent::class.java)!!

    private var fetchMaxRetry = immutableConfig.getInt(HTTP_FETCH_MAX_RETRY, 3)
    private val isClosed = AtomicBoolean()

    fun fetch(url: String): Response {
        val volatileConfig = VolatileConfig(immutableConfig)
        return fetchContent(WebPage.newWebPage(url, volatileConfig))
    }

    fun fetch(url: String, volatileConfig: VolatileConfig): Response {
        return fetchContent(WebPage.newWebPage(url, volatileConfig))
    }

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage): Response {
        val volatileConfig = page.volatileConfig ?: VolatileConfig(immutableConfig)
        val priority = volatileConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
        val task = FetchTask(0, nextTaskId, priority, page, volatileConfig)
        return seleniumEngine.fetchContentInternal(task).response
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>): List<Response> {
        val volatileConfig = VolatileConfig(immutableConfig)
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it, volatileConfig) }, volatileConfig)
    }

    fun fetchAll(urls: Iterable<String>): List<Response> {
        return fetchAll(nextBatchId, urls)
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>, volatileConfig: VolatileConfig): List<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it, volatileConfig) }, volatileConfig)
    }

    fun fetchAll(urls: Iterable<String>, volatileConfig: VolatileConfig): List<Response> {
        return fetchAll(nextBatchId, urls, volatileConfig)
    }

    fun parallelFetchAll(urls: Iterable<String>, volatileConfig: VolatileConfig): List<Response> {
        return parallelFetchAllPages(nextBatchId, urls.map { WebPage.newWebPage(it, volatileConfig) }, volatileConfig)
    }

    fun parallelFetchAll(batchId: Int, urls: Iterable<String>, volatileConfig: VolatileConfig): List<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it, volatileConfig) }, volatileConfig)
    }

    fun parallelFetchAllPages(pages: Iterable<WebPage>, volatileConfig: VolatileConfig): List<Response> {
        pages.forEach { if (it.volatileConfig == null) it.volatileConfig = volatileConfig }
        return parallelFetchAllPages(nextBatchId, pages, volatileConfig)
    }

    /**
     * TODO: use [ai.platon.pulsar.crawl.fetch.FetchThread]
     * */
    private fun parallelFetchAllPages(batchId: Int, pages: Iterable<WebPage>, volatileConfig: VolatileConfig): List<Response> {
        val cx = BatchFetchContext(batchId, pages, volatileConfig)

        log.info("Start batch task {} with {} pages in parallel", batchId, cx.numTotalTasks)

        cx.beforeFetchAll(pages)

        // Submit all tasks
        var taskId = 0
        pages.associateTo(cx.pendingTasks) { it.url to executor.submit { doFetch(++taskId, it, cx) } }
        val isLastTaskTimeout: (cx: BatchFetchContext) -> Boolean = {
            it.pendingTasks.size == 1 && it.idleSeconds >= 30 && it.numTotalTasks > 10
        }

        // Since the urls in the batch are usually from the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        while (cx.status.numTaskDone < cx.numTotalTasks
                && cx.status.numFailedTasks <= cx.numAllowedFailures
                && cx.idleSeconds < cx.idleTimeout.seconds
                && !isLastTaskTimeout(cx)
                && !isClosed.get()
                && !Thread.currentThread().isInterrupted) {
            ++cx.round

            // loop and wait for all parallel tasks return
            val done = mutableListOf<String>()
            cx.pendingTasks.asSequence().filter { it.value.isDone }.forEach { (url, future) ->
                val result = handleTaskDone(url, future, cx)
                done.add(result.task.url)
            }
            done.forEach { cx.pendingTasks.remove(it) }

            cx.idleSeconds = if (done.isEmpty()) 1 + cx.idleSeconds else 0

            if (cx.round >= 60 && cx.round % 30 == 0) {
                logLongTime(cx)
            }

            try {
                TimeUnit.SECONDS.sleep(cx.interval.seconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Selenium interrupted, {} pending tasks will be canceled", cx.pendingTasks.size)
            }
        }

        if (cx.pendingTasks.isNotEmpty()) {
            handleBatchAbort(cx)
        }

        cx.afterFetchAll(pages)

        logBatchFinished(cx)

        return cx.finishedTasks.values.map { it.response }
    }

    private fun doFetch(taskId: Int, page: WebPage, cx: BatchFetchContext): FetchResult {
        cx.beforeFetch(page)
        val task = FetchTask(cx.batchId, taskId, cx.priority, page, cx.volatileConfig)
        try {
            return seleniumEngine.fetchContentInternal(task)
        } finally {
            cx.afterFetch(page)
        }
    }

    private fun handleTaskDone(url: String, future: Future<FetchResult>, cx: BatchFetchContext): FetchResult {
        try {
            val result = getFetchResult(url, future, cx.threadTimeout)
            cx.finishedTasks[url] = result
            ++cx.status.numTaskDone

            val response = result.response

            val elapsed = Duration.ofSeconds(cx.round.toLong())
            when (response.code) {
                ProtocolStatus.DOCUMENT_INCOMPLETE -> {
                    log.warn("May be incomplete content, received {} average {}, retry it",
                            StringUtil.readableByteCount(response.length()), cx.status.averagePageSize)
                    ++cx.status.numIncompletePages
                }
                ProtocolStatusCodes.SUCCESS_OK -> {
                    cx.successPages.add(result.task.page)
                    cx.status.totalBytes += response.length()
                    ++cx.status.numSuccessTasks

                    cx.afterFetchN(cx.successPages)
                    logTaskSuccess(cx, url, response, elapsed)
                }
                ProtocolStatusCodes.CANCELED -> {
                    // canceled, status will not save
                    // logTaskSuccess(cx, url, response, elapsed)
                    log.warn("Task is canceled {} | {}", result.response.status, url)
                }
                else -> {
                    ++cx.status.numFailedTasks
                    logTaskFailed(cx, url, response, elapsed)
                }
            }

            return result
        } finally {
            ++cx.status.numTaskDone
        }
    }

    private fun handleBatchAbort(cx: BatchFetchContext) {
        logBatchAbort(cx)

        // if there are still pending tasks, cancel them
        cx.pendingTasks.forEach { it.value.cancel(true) }
        cx.pendingTasks.forEach { (url, task) ->
            // Attempts to cancel execution of this task
            try {
                val result = getFetchResult(url, task, cx.threadTimeout)
                cx.finishedTasks[url] = result
            } catch (e: Throwable) {
                log.error("Unexpected error {}", StringUtil.stringifyException(e))
            }
        }
    }

    private fun getFetchResult(url: String, future: Future<FetchResult>, timeout: Duration): FetchResult {
        // used only for failure
        val status: ProtocolStatus
        val headers = MultiMetadata()

        try {
            // Waits if necessary for at most the given time for the computation
            // to complete, and then retrieves its result, if available.
            return future.get(timeout.seconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.CancellationException) {
            // if the computation was cancelled
            status = ProtocolStatus.failed(ProtocolStatusCodes.CANCELED)
            headers.put("EXCEPTION", e.toString())
        } catch (e: java.util.concurrent.TimeoutException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.THREAD_TIMEOUT)
            headers.put("EXCEPTION", e.toString())

            log.warn("Fetch resource timeout, {}", e)
        } catch (e: java.util.concurrent.ExecutionException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.RETRY)
            headers.put("EXCEPTION", e.toString())

            log.warn("Unexpected exception, {}", StringUtil.stringifyException(e))
        } catch (e: InterruptedException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.RETRY)
            headers.put("EXCEPTION", e.toString())

            log.warn("Interrupted when fetch resource {}", e)
        } catch (e: Exception) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            headers.put("EXCEPTION", e.toString())

            log.warn("Unexpected exception, {}", StringUtil.stringifyException(e))
        }

        return FetchResult(FetchTask.NIL, ForwardingResponse(url, "", status, headers))
    }

    private fun logLongTime(cx: BatchFetchContext) {
        log.warn("Batch {} takes long time - round {} - {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}",
                cx.batchId, cx.round,
                cx.pendingTasks.size, cx.finishedTasks.size, cx.status.numFailedTasks,
                cx.idleSeconds, cx.idleTimeout)
    }

    private fun logTaskFailed(cx: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            log.info("Batch {} round {} task failed, reason {}, {} in {}, total {} failed | {}",
                    cx.batchId, String.format("%2d", cx.round),
                    ProtocolStatus.getMinorName(response.code),
                    StringUtil.readableByteCount(response.length()), DateTimeUtil.readableDuration(elapsed),
                    cx.status.numFailedTasks,
                    url
            )
        }
    }

    private fun logTaskSuccess(cx: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            log.info("Batch {} round {} fetched{}{} in {} with code {} | {}",
                    cx.batchId, String.format("%2d", cx.round),
                    if (cx.status.totalBytes < 2000) " only " else " ",
                    StringUtil.readableByteCount(response.length()),
                    DateTimeUtil.readableDuration(elapsed),
                    response.code, url)
        }
    }

    private fun logBatchAbort(cx: BatchFetchContext) {
        log.warn("Batch task is incomplete, finished tasks {}, pending tasks {}, failed tasks: {}, idle: {}s",
                cx.numFinishedTasks, cx.numPendingTasks, cx.status.numFailedTasks, cx.idleSeconds)
    }

    private fun logBatchFinished(cx: BatchFetchContext) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(cx.startTime, Instant.now())
            log.info("Batch {} with {} tasks is finished in {}, ave time {}s, ave size: {}, speed: {}bps",
                    cx.batchId, cx.numTotalTasks,
                    DateTimeUtil.readableDuration(elapsed),
                    String.format("%,.3f", 1.0 * elapsed.seconds / cx.numTotalTasks),
                    StringUtil.readableByteCount(cx.status.totalBytes / cx.numTotalTasks),
                    String.format("%,.3f", 1.0 * cx.status.totalBytes * 8 / elapsed.seconds)
            )
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
    }

    companion object {
        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()

        private val taskIdGen = AtomicInteger(10000)
        val nextTaskId get() = taskIdGen.incrementAndGet()
    }
}
