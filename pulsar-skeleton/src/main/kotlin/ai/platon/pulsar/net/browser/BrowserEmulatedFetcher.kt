package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.FetchThreadExecutor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.BatchStat
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
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
import kotlin.math.roundToLong

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
    var startTime = Instant.now()
    val priority = volatileConfig.getUint(BROWSER_DRIVER_PRIORITY, 0)
    val incognito = volatileConfig.getBoolean(BROWSER_INCOGNITO, false)
    // The function must return in a reasonable time
    val threadTimeout = volatileConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT).plusSeconds(5)
    val idleTimeout = threadTimeout.plusSeconds(5)
    var batchSize = Iterables.size(pages)
    val numAllowedFailures = max(10, batchSize / 3)

    // All submitted tasks
    val workingTasks = mutableMapOf<FetchTask, Future<FetchResult>>()
    val finishedTasks = mutableMapOf<FetchTask, FetchResult>()
    val privacyLeakedTasks = mutableListOf<FetchTask>()
    val successPages = mutableListOf<WebPage>()

    val numFinishedTasks get() = finishedTasks.size
    val numWorkingTasks get() = workingTasks.size

    // Submit all tasks
    var round = 0L

    val stat = BatchStat()
    var idleSeconds = 0
//    // external connection is lost, might be caused by proxy
    var connectionLost = false

    var beforeFetchHandler = volatileConfig.getBean(FETCH_BEFORE_FETCH_HANDLER, TaskHandler::class.java)
    var afterFetchHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_HANDLER, TaskHandler::class.java)
    var afterFetchNHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_N_HANDLER, BatchHandler::class.java)
    var beforeFetchAllHandler = volatileConfig.getBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, BatchHandler::class.java)
    var afterFetchAllHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_BATCH_HANDLER, BatchHandler::class.java)

    fun reset() {
        stat.numTaskDone = 0
        stat.numFailedTasks = 0

        round = 0L
        idleSeconds = 0
        startTime = Instant.now()
    }

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
 */
class BrowserEmulatedFetcher(
        val privacyContext: BrowserPrivacyContext,
        private val executor: FetchThreadExecutor,
        private val browserEmulator: BrowserEmulator,
        private val immutableConfig: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatedFetcher::class.java)!!

    private val driverPool = privacyContext.driverPool
    private val closed = AtomicBoolean()

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
        if (closed.get()) {
            return ForwardingResponse(page.url, ProtocolStatus.STATUS_CANCELED)
        }

        val volatileConfig = page.volatileConfig ?: immutableConfig.toVolatileConfig()
        val priority = volatileConfig.getUint(BROWSER_DRIVER_PRIORITY, 0)
        val incognito = volatileConfig.getBoolean(BROWSER_INCOGNITO, false)
        val task = FetchTask(0, priority, page, volatileConfig, incognito = incognito)

        return try {
            privacyContext.runInContext(task) { _, driver ->
                browserEmulator.fetch(task, driver)
            }.response
        } catch (e: IllegalStateException) {
            ForwardingResponse(page.url, ProtocolStatus.STATUS_CANCELED)
        }
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

    private fun parallelFetchAllPages(batchId: Int, pages: Iterable<WebPage>, volatileConfig: VolatileConfig): List<Response> {
        val cx = BatchFetchContext(batchId, pages, volatileConfig)

        // allocate drivers before batch fetch context timing
        allocateDriversIfNecessary(cx, cx.volatileConfig)

        log.info("Start batch task {} with {} pages in parallel", cx.batchId, cx.batchSize)

        cx.beforeFetchAll(pages)

        var retry = 1
        do {
            if (privacyContext.isPrivacyLeaked) {
                privacyContext.reset()
                cx.reset()
            }

            parallelFetch(cx)
        } while (retry++ <= 2 && privacyContext.isPrivacyLeaked)

        cx.afterFetchAll(cx.pages)

        logBatchFinished(cx)

        return cx.finishedTasks.values.map { it.response }
    }

    /**
     * TODO: merge with [ai.platon.pulsar.crawl.fetch.FetchThread]
     * */
    private fun parallelFetch(cx: BatchFetchContext) {
        if (cx.privacyLeakedTasks.isNotEmpty()) {
            cx.batchSize = cx.privacyLeakedTasks.size
            cx.privacyLeakedTasks.forEachIndexed { batchTaskId, task ->
                task.reset()
                cx.workingTasks[task] = executor.submit { doFetch(task, cx) }
            }
            cx.privacyLeakedTasks.clear()
        } else {
            // Submit all tasks
            cx.pages.forEachIndexed { batchTaskId, page ->
                val task = FetchTask(
                        batchId = cx.batchId,
                        batchTaskId = batchTaskId,
                        priority = cx.priority,
                        page = page,
                        volatileConfig = cx.volatileConfig,
                        batchSize = cx.batchSize,
                        incognito = cx.incognito
                )
                cx.workingTasks[task] = executor.submit { doFetch(task, cx) }
            }
        }

        val isLastTaskTimeout: (cx: BatchFetchContext) -> Boolean = {
            it.batchSize > 10 && it.workingTasks.size == 1 && it.idleSeconds >= 30
        }

        // Since the urls in the batch are usually in the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        val done = ArrayList<FetchResult>(cx.numWorkingTasks)
        while (cx.stat.numTaskDone < cx.batchSize
                && cx.stat.numFailedTasks <= cx.numAllowedFailures
                && cx.idleSeconds < cx.idleTimeout.seconds
                && !cx.connectionLost
                && !privacyContext.isPrivacyLeaked
                && !isLastTaskTimeout(cx)
                && !closed.get()
                && !Thread.currentThread().isInterrupted) {
            ++cx.round

            // loop and wait for all parallel tasks return
            done.clear()
            cx.workingTasks.asSequence().filter { it.value.isDone }.mapTo(done) { handleTaskDone(it.key, it.value, cx) }
            done.forEach { cx.workingTasks.remove(it.task) }
            cx.idleSeconds = if (done.isEmpty()) 1 + cx.idleSeconds else 0

            if (cx.round >= 60 && cx.round % 30 == 0L) {
                logLongTimeCost(cx)
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Browser emulator is interrupted, {} pending tasks will be canceled", cx.workingTasks.size)
            }
        }

        if (cx.workingTasks.isNotEmpty()) {
            handleBatchAbort(cx)
        }
    }

    private fun allocateDriversIfNecessary(cx: BatchFetchContext, volatileConfig: VolatileConfig) {
        // allocate drivers before batch fetch context timing, the allocation might take long time
        val requiredDrivers = cx.batchSize - driverPool.freeSize
        if (requiredDrivers > 0) {
            log.info("Allocating $requiredDrivers drivers")
            driverPool.allocate(0, requiredDrivers, volatileConfig)
            cx.startTime = Instant.now()
        }
    }

    private fun doFetch(task: FetchTask, cx: BatchFetchContext): FetchResult {
        return try {
            cx.beforeFetch(task.page)
            privacyContext.run(task) { _, driver ->
                browserEmulator.fetch(task, driver)
            }
        } catch (e: WebDriverPoolExhaust) {
            log.warn("Too many web drivers", e)
            FetchResult(task, ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)))
        } catch (e: IllegalStateException) {
            log.warn("Program is already closed", e)
            FetchResult(task, ForwardingResponse(task.url, ProtocolStatus.STATUS_CANCELED))
        } catch (e: Throwable) {
            log.warn("Unexpected exception", e)
            FetchResult(task, ForwardingResponse(task.url, ProtocolStatus.STATUS_FAILED))
        } finally {
            cx.afterFetch(task.page)
        }
    }

    private fun handleTaskDone(task: FetchTask, future: Future<FetchResult>, cx: BatchFetchContext): FetchResult {
        val url = task.url
        try {
            val result = getFetchResult(task, future, cx.threadTimeout)
            cx.finishedTasks[task] = result

            val elapsed = Duration.ofSeconds(cx.round)
            val response = result.response
            when (response.httpCode) {
                ProtocolStatusCodes.SUCCESS_OK -> {
                    privacyContext.informSuccess()

                    cx.successPages.add(result.task.page)
                    cx.stat.totalBytes += response.length()
                    ++cx.stat.numSuccessTasks

                    cx.afterFetchN(cx.successPages)
                    logTaskSuccess(cx, url, response, elapsed)
                }
                ProtocolStatusCodes.RETRY -> {
                    if (response.status.isRetry(RetryScope.PRIVACY_CONTEXT)) {
                        cx.privacyLeakedTasks.add(task)
                        privacyContext.informWarning()
                    }
                }
                ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT -> {
                    log.info("Browser connection timed out | {} | {}", response.status, url)
                    cx.connectionLost = true
                }
                else -> {
                    ++cx.stat.numFailedTasks
                    logTaskFailed(cx, url, response, elapsed)
                }
            }

            return result
        } finally {
            ++cx.stat.numTaskDone
        }
    }

    private fun handleBatchAbort(cx: BatchFetchContext) {
        logBatchAbort(cx)

        // if there are still pending tasks, cancel them
        cx.workingTasks.forEach { (task, future) ->
            browserEmulator.cancel(task)
            future.cancel(true)
        }
        cx.workingTasks.forEach { (task, future) ->
            // Attempts to cancel execution of this task
            try {
                val result = getFetchResult(task, future, cx.threadTimeout)
                cx.finishedTasks[task] = result
            } catch (e: Throwable) {
                log.error("Unexpected error {}", StringUtil.stringifyException(e))
            }
        }
    }

    private fun getFetchResult(task: FetchTask, future: Future<FetchResult>, timeout: Duration): FetchResult {
        // used only for failure
        val status: ProtocolStatus
        val headers = MultiMetadata()

        try {
            // Waits if necessary for at most the given time for the computation
            // to complete, and then retrieves its result, if available.
            return future.get(timeout.seconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.CancellationException) {
            // if the computation was cancelled
            status = ProtocolStatus.STATUS_CANCELED
            headers.put("EXCEPTION", e.toString())
        } catch (e: java.util.concurrent.TimeoutException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.THREAD_TIMEOUT)
            headers.put("EXCEPTION", e.toString())
            log.warn("Fetch resource timeout, {}", e)
        } catch (e: java.util.concurrent.ExecutionException) {
            status = ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL)
            headers.put("EXCEPTION", e.toString())
            log.warn("Unexpected exception", e)
        } catch (e: InterruptedException) {
            status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
            headers.put("EXCEPTION", e.toString())
            log.warn("Interrupted when fetch resource {}", e)
        } catch (e: Exception) {
            status = ProtocolStatus.STATUS_EXCEPTION
            headers.put("EXCEPTION", e.toString())
            log.warn("Unexpected exception", e)
        }

        return FetchResult(FetchTask.NIL, ForwardingResponse(task.url, "", status, headers))
    }

    private fun logLongTimeCost(cx: BatchFetchContext) {
        log.warn("Batch {} takes long time - round {} - {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}",
                cx.batchId, cx.round,
                cx.workingTasks.size, cx.finishedTasks.size, cx.stat.numFailedTasks,
                cx.idleSeconds, cx.idleTimeout)
    }

    private fun logTaskFailed(cx: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            log.info("Batch {} round {} task failed, {} in {}, {}, total {} failed | {}",
                    cx.batchId, String.format("%2d", cx.round),
                    StringUtil.readableByteCount(response.length()), DateTimeUtil.readableDuration(elapsed),
                    response.status,
                    cx.stat.numFailedTasks,
                    url
            )
        }
    }

    private fun logTaskSuccess(cx: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            val code = response.httpCode
            val codeMessage = if (code != 200) " with code $code" else ""
            log.info("Batch {} round {} fetched{}{} in {}{} | {}",
                    cx.batchId, String.format("%2d", cx.round),
                    if (cx.stat.totalBytes < 2000) " only " else " ",
                    StringUtil.readableByteCount(response.length()),
                    DateTimeUtil.readableDuration(elapsed),
                    codeMessage, url)
        }
    }

    private fun logBatchAbort(cx: BatchFetchContext) {
        log.warn("Batch {} is abort, finished: {}, pending: {}, failed: {}, total: {}, idle: {}s",
                cx.batchId,
                cx.numFinishedTasks, cx.numWorkingTasks, cx.stat.numFailedTasks, cx.batchSize,
                cx.idleSeconds)
    }

    private fun logBatchFinished(cx: BatchFetchContext) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(cx.startTime, Instant.now())
            val aveTime = elapsed.dividedBy(cx.batchSize.toLong())
            val speed = StringUtil.readableByteCount((1.0 * cx.stat.totalBytes / elapsed.seconds).roundToLong())
            log.info("Batch {} with {} tasks is finished in {}, ave time {}, ave size: {}, speed: {}",
                    cx.batchId, cx.batchSize,
                    DateTimeUtil.readableDuration(elapsed),
                    DateTimeUtil.readableDuration(aveTime),
                    StringUtil.readableByteCount(cx.stat.averagePageSize.roundToLong()),
                    speed
            )
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
    }

    companion object {
        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()
    }
}
