package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.FetchThreadExecutor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

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

    private val driverManager = privacyContextFactory.driverManager
    private val closed = AtomicBoolean()

    fun fetch(url: String): Response {
        return fetchContent(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))
    }

    fun fetch(url: String, volatileConfig: VolatileConfig): Response {
        return fetchContent(WebPage.newWebPage(url, volatileConfig))
    }

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage): Response {
        if (closed.get()) {
            return ForwardingResponse.canceled(page)
        }

        val volatileConfig = page.volatileConfig ?: immutableConfig.toVolatileConfig()
        val priority = volatileConfig.getUint(BROWSER_DRIVER_PRIORITY, 0)
        val task = FetchTask(0, priority, page, volatileConfig)

        return try {
            privacyContextFactory.activeContext.runInContext(task) { _, driver ->
                browserEmulator.fetch(task, driver)
            }.response
        } catch (e: IllegalStateException) {
            ForwardingResponse.canceled(page)
        }
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>): List<Response> {
        val volatileConfig = immutableConfig.toVolatileConfig()
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
        var cx = FetchTaskBatch(batchId, pages, volatileConfig, privacyContextFactory)

        // allocate drivers before batch fetch context timing
        allocateDriversIfNecessary(cx, cx.conf)

        log.info("Start batch task {} with {} pages in parallel with expected proxy <{}>",
                cx.batchId, cx.batchSize, driverManager.proxyManager.currentProxyEntry)

        cx.beforeFetchAll(cx.pages)

        var retry = 1
        val maxRetry = 2
        do {
            if (privacyContextFactory.activeContext.isPrivacyLeaked) {
                privacyContextFactory.reset()
                cx = cx.createNextNode()
            }

            parallelFetch(cx)
        } while (retry++ <= maxRetry && privacyContextFactory.activeContext.isPrivacyLeaked)

        cx.afterFetchAll(cx.pages)

        logBatchFinished(cx)

        return cx.collectResponses()
    }

    /**
     * TODO: merge with [ai.platon.pulsar.crawl.fetch.FetchThread]
     * */
    private fun parallelFetch(cx: FetchTaskBatch) {
        // Submit all tasks
        cx.pages.forEachIndexed { i, page ->
            val task = FetchTask(
                    batchId = cx.batchId,
                    batchTaskId = 1 + i,
                    priority = cx.priority,
                    page = page,
                    volatileConfig = cx.conf,
                    batchSize = cx.batchSize
            )
            cx.workingTasks[task] = executor.submit { doFetch(task, cx) }
        }

        // Since the urls in the batch are usually in the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        val done = ArrayList<FetchResult>(cx.numWorkingTasks)
        var state: FetchTaskBatch.State
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
        } while (state == FetchTaskBatch.State.OK)

        if (cx.workingTasks.isNotEmpty()) {
            handleBatchAbort(cx, state)
        }
    }

    private fun checkState(cx: FetchTaskBatch): FetchTaskBatch.State {
        return when {
            closed.get() -> FetchTaskBatch.State.CLOSED
            Thread.currentThread().isInterrupted -> FetchTaskBatch.State.INTERRUPTED
            privacyContextFactory.activeContext.isPrivacyLeaked -> FetchTaskBatch.State.PRIVACY_LEAK
            else -> cx.checkState()
        }
    }

    private fun allocateDriversIfNecessary(cx: FetchTaskBatch, volatileConfig: VolatileConfig) {
        // allocate drivers before batch fetch context timing, the allocation might take long time
        val requiredDrivers = cx.batchSize - driverManager.driverPool.numFree
        if (requiredDrivers > 0) {
            log.info("Allocating $requiredDrivers drivers")
            driverManager.allocate(0, requiredDrivers, volatileConfig)
            cx.startTime = Instant.now()
        }
    }

    private fun doFetch(task: FetchTask, cx: FetchTaskBatch): FetchResult {
        return try {
            cx.beforeFetch(task.page)
            privacyContextFactory.activeContext.run(task) { _, driver ->
                browserEmulator.fetch(task, driver)
            }
        } catch (e: ProxyException) {
            log.warn(StringUtil.simplifyException(e))
            FetchResult(task, ForwardingResponse.canceled(task.page))
        } catch (e: WebDriverPoolExhaust) {
            log.warn("Too many web drivers", e)
            FetchResult(task, ForwardingResponse.retry(task.page, RetryScope.CRAWL_SCHEDULE))
        } catch (e: IllegalStateException) {
            log.warn("Application is closed ({})", StringUtil.simplifyException(e))
            FetchResult(task, ForwardingResponse.canceled(task.page))
        } catch (e: Throwable) {
            log.warn("Unexpected throwable", e)
            e.printStackTrace()
            FetchResult(task, ForwardingResponse.failed(task.page, e))
        } finally {
            cx.afterFetch(task.page)
        }
    }

    private fun handleTaskDone(task: FetchTask, future: Future<FetchResult>, cx: FetchTaskBatch): FetchResult {
        val url = task.url
        try {
            val result = waitFor(task, future, cx.threadTimeout)

            val elapsed = Duration.ofSeconds(cx.round)
            val response = result.response
            when (response.httpCode) {
                ProtocolStatusCodes.SUCCESS_OK -> {
                    cx.onSuccess(task, result)
                    logTaskSuccess(cx, url, response, elapsed)
                }
                ProtocolStatusCodes.RETRY -> {
                    cx.onRetry(task, result)
                }
                else -> {
                    cx.onFailure(task, result)
                    logTaskFailed(cx, url, response, elapsed)
                }
            }

            return result
        } finally {
            ++cx.stat.numTaskDone
        }
    }

    private fun handleBatchAbort(cx: FetchTaskBatch, state: FetchTaskBatch.State) {
        logBatchAbort(cx, state)

        // if there are still pending tasks, cancel them
        cx.workingTasks.forEach { browserEmulator.cancel(it.key) }
//        cx.workingTasks.forEach { it.value.cancel(true) }
        cx.workingTasks.forEach { (task, future) ->
            val result = waitFor(task, future, cx.threadTimeout)
            cx.onAbort(task, result)
        }
    }

    private fun waitFor(task: FetchTask, future: Future<FetchResult>, timeout: Duration): FetchResult {
        // used only for failure
        val status: ProtocolStatus
        val headers = MultiMetadata()

        try {
            // Wait if necessary for at most the given time for the computation
            // to complete, and then retrieves its result, if available.
            return future.get(timeout.seconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.CancellationException) {
            log.debug("Fetch thread is canceled | {}", task.url)
            // if the computation was cancelled
            status = ProtocolStatus.STATUS_CANCELED
        } catch (e: java.util.concurrent.TimeoutException) {
            log.warn("Timeout when retrieve task result", e)
            status = ProtocolStatus.failed(ProtocolStatusCodes.THREAD_TIMEOUT)
        } catch (e: java.util.concurrent.ExecutionException) {
            log.warn("Execution error caught when retrieve task result", e)
            status = ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL)
        } catch (e: InterruptedException) {
            log.warn("Interrupted when retrieve task result", e)
            status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
        } catch (e: Exception) {
            log.warn("Unexpected exception", e)
            status = ProtocolStatus.STATUS_EXCEPTION
        }

        return FetchResult(FetchTask.NIL, ForwardingResponse(task.url, "", status, headers, task.page))
    }

    private fun logLongTimeCost(cx: FetchTaskBatch) {
        log.warn("Batch {} takes long time - round {} - {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}",
                cx.batchId, cx.round,
                cx.workingTasks.size, cx.finishedTasks.size, cx.stat.numFailedTasks,
                cx.idleSeconds, cx.idleTimeout)
    }

    private fun logTaskFailed(cx: FetchTaskBatch, url: String, response: Response, elapsed: Duration) {
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

    private fun logTaskSuccess(cx: FetchTaskBatch, url: String, response: Response, elapsed: Duration) {
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

    private fun logBatchAbort(cx: FetchTaskBatch, state: FetchTaskBatch.State) {
        val proxyDisplay = (cx.lastSuccessProxy?:cx.lastFailedProxy)?.display
        log.warn("Batch {} is aborted ({}), finished: {}, pending: {}, failed: {}, total: {}, idle: {}s | {}",
                cx.batchId, state,
                cx.numFinishedTasks, cx.numWorkingTasks, cx.stat.numFailedTasks, cx.batchSize,
                cx.idleSeconds, proxyDisplay?:"(all failed)")
    }

    private fun logBatchFinished(cx: FetchTaskBatch) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(cx.startTime, Instant.now())
            val aveTime = elapsed.dividedBy(1 + cx.batchSize.toLong())
            val speed = StringUtil.readableBytes((1.0 * cx.stat.totalBytes / (1 + elapsed.seconds)).roundToLong())
            val proxyDisplay = (cx.lastSuccessProxy?:cx.lastFailedProxy)?.display
            log.info("Batch {} with {} tasks is finished in {}, ave time {}, ave size: {}, speed: {}/s | {}",
                    cx.batchId, cx.batchSize,
                    DateTimeUtil.readableDuration(elapsed),
                    DateTimeUtil.readableDuration(aveTime),
                    StringUtil.readableBytes(cx.stat.averagePageSize.roundToLong()),
                    speed,
                    proxyDisplay?:"(no proxy)"
            )
        }

        if (log.isTraceEnabled) {
            log.trace("Drivers - $driverManager")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }

    companion object {
        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()
    }
}
