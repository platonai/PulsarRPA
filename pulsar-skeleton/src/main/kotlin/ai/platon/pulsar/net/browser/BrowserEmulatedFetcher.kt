package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.FetchThreadExecutor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
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
    var idleSeconds = 0
//    // external connection is lost, might be caused by proxy
    var connectionLost = false
    /**
     * Batch statistics
     * */
    val stat = BatchStat()
    var lastSuccessTask: FetchTask? = null
    var lastFailedTask: FetchTask? = null
    var lastSuccessProxy: ProxyEntry? = null
    var lastFailedProxy: ProxyEntry? = null

    var beforeFetchHandler = volatileConfig.getBean(FETCH_BEFORE_FETCH_HANDLER, TaskHandler::class.java)
    var afterFetchHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_HANDLER, TaskHandler::class.java)
    var afterFetchNHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_N_HANDLER, BatchHandler::class.java)
    var beforeFetchAllHandler = volatileConfig.getBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, BatchHandler::class.java)
    var afterFetchAllHandler = volatileConfig.getBean(FETCH_AFTER_FETCH_BATCH_HANDLER, BatchHandler::class.java)

    val root: BatchFetchContext get() {
        var r = this
        while (r.parent != null) {
            r = r.parent!!
        }
        return r
    }
    var parent: BatchFetchContext? = null
    var child: BatchFetchContext? = null

    fun lastTaskTimeout(): Boolean {
        return batchSize > 10 && workingTasks.size == 1 && idleSeconds >= 30
    }

    fun createChild(): BatchFetchContext {
        val retryPages = privacyLeakedTasks.map { it.page }
        val cx = BatchFetchContext(batchId, retryPages, volatileConfig)
        cx.parent = this
        child = cx
        return cx
    }

    fun collectResponses(): List<Response> {
        val responses = mutableListOf<Response>()

        var cx: BatchFetchContext? = root
        do {
            cx?.finishedTasks?.mapTo(responses) { it.value.response }
            cx = cx?.child
        } while (cx != null)

        return responses
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
        val privacyContextFactory: PrivacyContextFactory,
        private val executor: FetchThreadExecutor,
        private val browserEmulator: BrowserEmulator,
        private val immutableConfig: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatedFetcher::class.java)!!

    private val driverPool = privacyContextFactory.driverPool
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
        val task = FetchTask(0, priority, page, volatileConfig)

        return try {
            privacyContextFactory.activeContext.runInContext(task) { _, driver ->
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
        var cx = BatchFetchContext(batchId, pages, volatileConfig)

        // allocate drivers before batch fetch context timing
        allocateDriversIfNecessary(cx, cx.volatileConfig)

        log.info("Start batch task {} with {} pages in parallel", cx.batchId, cx.batchSize)

        cx.beforeFetchAll(cx.pages)

        var retry = 1
        do {
            if (privacyContextFactory.activeContext.isPrivacyLeaked) {
                privacyContextFactory.reset()
                cx = cx.createChild()
            }

            parallelFetch(cx)
        } while (retry++ <= 2 && privacyContextFactory.activeContext.isPrivacyLeaked)

        cx.afterFetchAll(cx.pages)

        logBatchFinished(cx)

        return cx.collectResponses()
    }

    /**
     * TODO: merge with [ai.platon.pulsar.crawl.fetch.FetchThread]
     * */
    private fun parallelFetch(cx: BatchFetchContext) {
        // Submit all tasks
        cx.pages.forEachIndexed { i, page ->
            val task = FetchTask(
                    batchId = cx.batchId,
                    batchTaskId = 1 + i,
                    priority = cx.priority,
                    page = page,
                    volatileConfig = cx.volatileConfig,
                    batchSize = cx.batchSize
            )
            cx.workingTasks[task] = executor.submit { doFetch(task, cx) }
        }

        // Since the urls in the batch are usually in the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        val done = ArrayList<FetchResult>(cx.numWorkingTasks)
        var state: String
        do {
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

            state = checkState(cx)
        } while (state == "continue")

        if (cx.workingTasks.isNotEmpty()) {
            handleBatchAbort(cx, state)
        }
    }

    private fun checkState(cx: BatchFetchContext): String {
        return when {
            cx.stat.numTaskDone >= cx.batchSize -> "all done"
            cx.stat.numFailedTasks > cx.numAllowedFailures -> "too many failure"
            cx.idleSeconds > cx.idleTimeout.seconds -> "idle timeout"
            cx.connectionLost -> "connection lost"
            cx.lastTaskTimeout() -> "last task timeout"
            privacyContextFactory.activeContext.isPrivacyLeaked -> "privacy leak"
            Thread.currentThread().isInterrupted -> "interrupted"
            closed.get() -> "program closed"
            else -> "continue"
        }
    }

    private fun allocateDriversIfNecessary(cx: BatchFetchContext, volatileConfig: VolatileConfig) {
        // allocate drivers before batch fetch context timing, the allocation might take long time
        val requiredDrivers = cx.batchSize - driverPool.nFree
        if (requiredDrivers > 0) {
            log.info("Allocating $requiredDrivers drivers")
            driverPool.allocate(0, requiredDrivers, volatileConfig)
            cx.startTime = Instant.now()
        }
    }

    private fun doFetch(task: FetchTask, cx: BatchFetchContext): FetchResult {
        return try {
            cx.beforeFetch(task.page)
            privacyContextFactory.activeContext.run(task) { _, driver ->
                browserEmulator.fetch(task, driver)
            }
        } catch (e: ProxyException) {
            log.warn(StringUtil.simplifyException(e))
            FetchResult(task, ForwardingResponse(task.url, ProtocolStatus.STATUS_CANCELED))
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
                    privacyContextFactory.activeContext.informSuccess("handleTaskDone | ")

                    cx.lastSuccessTask = result.task
                    cx.lastSuccessProxy = result.task.proxyEntry
                    cx.successPages.add(result.task.page)
                    cx.stat.totalBytes += response.length()
                    cx.stat.numSuccessTasks++

                    cx.afterFetchN(cx.successPages)

                    logTaskSuccess(cx, url, response, elapsed)
                }
                ProtocolStatusCodes.RETRY -> {
                    cx.lastFailedTask = result.task
                    cx.lastFailedProxy = result.task.proxyEntry

                    if (response.status.isRetry(RetryScope.PRIVACY_CONTEXT)) {
                        cx.privacyLeakedTasks.add(task)
                        privacyContextFactory.activeContext.informWarning("handleTaskDone | ")
                    }
                }
                else -> {
                    cx.lastFailedTask = result.task
                    cx.lastFailedProxy = result.task.proxyEntry

                    ++cx.stat.numFailedTasks

                    logTaskFailed(cx, url, response, elapsed)
                }
            }

            return result
        } finally {
            ++cx.stat.numTaskDone
        }
    }

    private fun handleBatchAbort(cx: BatchFetchContext, state: String) {
        logBatchAbort(cx, state)

        // if there are still pending tasks, cancel them
        cx.workingTasks.forEach { (task, future) ->
            browserEmulator.cancel(task)
            future.cancel(true)
        }
        cx.workingTasks.forEach { (task, future) ->
            cx.finishedTasks[task] = getFetchResult(task, future, cx.threadTimeout)
        }
    }

    private fun getFetchResult(task: FetchTask, future: Future<FetchResult>, timeout: Duration): FetchResult {
        // used only for failure
        val status: ProtocolStatus
        val headers = MultiMetadata()

        try {
            // Wait if necessary for at most the given time for the computation
            // to complete, and then retrieves its result, if available.
            return future.get(timeout.seconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.CancellationException) {
            // if the computation was cancelled
            status = ProtocolStatus.STATUS_CANCELED
        } catch (e: java.util.concurrent.TimeoutException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.THREAD_TIMEOUT)
            log.warn("Fetch resource timeout, {}", e)
        } catch (e: java.util.concurrent.ExecutionException) {
            status = ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL)
            log.warn("Unexpected exception", e)
        } catch (e: InterruptedException) {
            status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
            log.warn("Interrupted when retrieve task result", e)
        } catch (e: Exception) {
            status = ProtocolStatus.STATUS_EXCEPTION
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
                    StringUtil.readableBytes(response.length()), DateTimeUtil.readableDuration(elapsed),
                    response.status,
                    cx.stat.numFailedTasks,
                    url
            )
        }
    }

    private fun logTaskSuccess(cx: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            val httpCode = response.httpCode
            val codeMessage = if (httpCode != 200) " with code $httpCode" else ""
            log.info("Batch {} round {} fetched{}{} in {}{} | {}",
                    cx.batchId, String.format("%2d", cx.round),
                    if (cx.stat.totalBytes < 2000) " only " else " ",
                    StringUtil.readableBytes(response.length()),
                    DateTimeUtil.readableDuration(elapsed),
                    codeMessage, url)
        }
    }

    private fun logBatchAbort(cx: BatchFetchContext, state: String) {
        val proxyDisplay = cx.lastSuccessTask?.proxyEntry?.display
        log.warn("Batch {} is abort ({}), finished: {}, pending: {}, failed: {}, total: {}, idle: {}s | {}",
                cx.batchId, state,
                cx.numFinishedTasks, cx.numWorkingTasks, cx.stat.numFailedTasks, cx.batchSize,
                cx.idleSeconds, proxyDisplay?:"(all failed)")
    }

    private fun logBatchFinished(cx: BatchFetchContext) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(cx.startTime, Instant.now())
            val aveTime = elapsed.dividedBy(1 + cx.batchSize.toLong())
            val speed = StringUtil.readableBytes((1.0 * cx.stat.totalBytes / (1 + elapsed.seconds)).roundToLong())
            val proxyDisplay = cx.lastSuccessTask?.proxyEntry?.display
            log.info("Batch {} with {} tasks is finished in {}, ave time {}, ave size: {}, speed: {} | {}",
                    cx.batchId, cx.batchSize,
                    DateTimeUtil.readableDuration(elapsed),
                    DateTimeUtil.readableDuration(aveTime),
                    StringUtil.readableBytes(cx.stat.averagePageSize.roundToLong()),
                    speed,
                    proxyDisplay?:"(no proxy)"
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
