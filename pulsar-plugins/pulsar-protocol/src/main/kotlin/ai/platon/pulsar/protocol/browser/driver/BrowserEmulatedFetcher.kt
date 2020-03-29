package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.FetchTaskBatch
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import io.netty.util.concurrent.EventExecutorGroup
import io.netty.util.concurrent.Future
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class BrowserEmulatedFetcher(
        val privacyContextManager: PrivacyContextManager,
        private val executor: EventExecutorGroup,
        private val browserEmulator: BrowserEmulator,
        private val immutableConfig: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatedFetcher::class.java)!!

    private val driverManager = privacyContextManager.driverManager
    private val proxyManager = driverManager.proxyManager
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
            privacyContextManager.run(task) { _, driver ->
                browserEmulator.fetch(task, driver)
            }.response
        } catch (e: ProxyException) {
            ForwardingResponse.retry(page, RetryScope.CRAWL)
        } catch (e: IllegalContextStateException) {
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
        FetchTaskBatch(batchId, pages, volatileConfig, privacyContextManager.activeContext).use { batch ->
            // allocate drivers before batch fetch context timing
            allocateDriversIfNecessary(batch, batch.conf)

            logBeforeBatchStart(batch)

            batch.universalStat.startTime = Instant.now()
            batch.beforeFetchAll(batch.pages)

            var i = 1
            do {
                var b = batch
                if (privacyContextManager.activeContext.isPrivacyLeaked) {
                    privacyContextManager.reset()
                    b = b.createNextNode(privacyContextManager.activeContext)
                }

                parallelFetch(b)
            } while (i++ <= privacyContextManager.maxRetry && privacyContextManager.activeContext.isPrivacyLeaked)

            batch.afterFetchAll(batch.pages)

            logAfterBatchFinish(batch)

            return batch.collectResponses()
        }
    }

    /**
     * TODO: merge with [ai.platon.pulsar.crawl.fetch.FetchThread]
     * */
    private fun parallelFetch(batch: FetchTaskBatch) {
        // Submit all tasks
        batch.pages.forEachIndexed { i, page ->
            val task = FetchTask(
                    batchId = batch.batchId,
                    batchTaskId = 1 + i,
                    priority = batch.priority,
                    page = page,
                    volatileConfig = batch.conf,
                    batchSize = batch.batchSize
            )

            batch.workingTasks[task] = executor.submit(Callable { doFetch(task, batch) })
            // batch.workingTasks[task] = executor.submit { doFetch(task, batch) }
        }

        // Since the urls in the batch are usually in the same domain
        // if there are too many failure, the rest tasks are very likely run to failure too
        var state: FetchTaskBatch.State
        do {
            ++batch.round

            var numDone = 0
            val it = batch.workingTasks.iterator()
            while (it.hasNext()) {
                val (task, future) = it.next()
                if (future.isDone) {
                    handleTaskDone(task, future, batch)
                    ++numDone
                    it.remove()
                }
            }

            batch.idleSeconds = if (numDone == 0) 1 + batch.idleSeconds else 0

            if (batch.round >= 60 && batch.round % 30 == 0L) {
                logIdleLongTime(batch)
            }

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Browser emulator is interrupted, {} pending tasks will be canceled", batch.workingTasks.size)
            }

            state = checkState(batch)
        } while (state == FetchTaskBatch.State.RUNNING)

        if (batch.workingTasks.isNotEmpty()) {
            abortBatchTasks(batch, state)
        }
    }

    private fun checkState(batch: FetchTaskBatch): FetchTaskBatch.State {
        return when {
            closed.get() -> FetchTaskBatch.State.CLOSED
            Thread.currentThread().isInterrupted -> FetchTaskBatch.State.INTERRUPTED
            privacyContextManager.activeContext.isPrivacyLeaked -> FetchTaskBatch.State.PRIVACY_LEAK
            else -> batch.checkState()
        }
    }

    private fun allocateDriversIfNecessary(batch: FetchTaskBatch, volatileConfig: VolatileConfig) {
        // allocate drivers before batch fetch context timing, the allocation might take long time
        val requiredDrivers = batch.batchSize - driverManager.driverPool.numFree
        if (requiredDrivers > 0) {
            log.info("Allocating $requiredDrivers drivers")
            driverManager.allocate(0, requiredDrivers, volatileConfig)
        }
    }

    private fun doFetch(task: FetchTask, batch: FetchTaskBatch): FetchResult {
        return privacyContextManager.activeContext.run(task) { _, driver ->
            try {
                batch.beforeFetch(task.page)
                browserEmulator.fetch(task, driver)
            } catch (e: WebDriverPoolExhaust) {
                log.warn("Too many web drivers", e)
                FetchResult(task, ForwardingResponse.retry(task.page, RetryScope.CRAWL))
            } catch (e: IllegalContextStateException) {
                log.info("Illegal context state, the task is cancelled | {}", task.url)
                FetchResult(task, ForwardingResponse.canceled(task.page))
            } catch (e: Throwable) {
                log.warn("Unexpected throwable", e)
                FetchResult(task, ForwardingResponse.failed(task.page, e))
            } finally {
                batch.afterFetch(task.page)
            }
        }
    }

    private fun handleTaskDone(task: FetchTask, future: Future<FetchResult>, batch: FetchTaskBatch): FetchResult {
        val url = task.url
        try {
            val result = waitFor(task, future, batch.threadTimeout)

            val elapsed = Duration.ofSeconds(batch.round)
            val response = result.response
            when (response.httpCode) {
                ProtocolStatusCodes.SUCCESS_OK -> {
                    batch.onSuccess(task, result)
                    logAfterTaskSuccess(batch, url, response, elapsed)
                }
                ProtocolStatusCodes.RETRY -> {
                    batch.onRetry(task, result)
                }
                else -> {
                    batch.onFailure(task, result)
                    logAfterTaskFailed(batch, url, response, elapsed)
                }
            }

            return result
        } finally {
            ++batch.numTasksDone
        }
    }

    private fun abortBatchTasks(batch: FetchTaskBatch, state: FetchTaskBatch.State) {
        logAfterBatchAbort(batch, state)

        // If there are still pending tasks, cancel them
        batch.workingTasks.forEach { browserEmulator.cancel(it.key) }

        // Wait until all worker thread complete normally or timeout
        var tick = 0
        val timeout = Duration.ofMinutes(2).seconds
        while (batch.workingTasks.isNotEmpty() && tick++ < timeout) {
            checkAndHandleTasksAbort(batch)

            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // Finally, if there are still working tasks, force abort the worker threads
        if (batch.workingTasks.isNotEmpty()) {
            log.warn("There are still {} working tasks, cancel worker threads", batch.numWorkingTasks)
            batch.workingTasks.forEach {
                it.value.cancel(true)
            }

            checkAndHandleTasksAbort(batch)
        }

        if (batch.workingTasks.isNotEmpty()) {
            log.warn("There are still {} working tasks unexpectedly", batch.numWorkingTasks)
            batch.workingTasks.clear()
        }
    }

    private fun checkAndHandleTasksAbort(batch: FetchTaskBatch) {
        val it = batch.workingTasks.iterator()
        while (it.hasNext()) {
            val (task, future) = it.next()
            if (future.isDone) {
                handleTaskAbort(task, future, batch)
                it.remove()
            }
        }
    }

    private fun handleTaskAbort(task: FetchTask, future: Future<FetchResult>, batch: FetchTaskBatch): FetchResult {
        val result = waitFor(task, future, batch.threadTimeout)
        batch.onAbort(task, result)
        return result
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
            // log.debug("Fetch thread for task #{}/{} is canceled | {}", task.batchTaskId, task.batchId, task.url)
            status = ProtocolStatus.STATUS_CANCELED
        } catch (e: java.util.concurrent.TimeoutException) {
            log.warn("Timeout when retrieve task result", e)
            status = ProtocolStatus.failed(ProtocolStatusCodes.THREAD_TIMEOUT)
        } catch (e: java.util.concurrent.ExecutionException) {
            log.warn("Failed to get the fetch result", e)
            status = ProtocolStatus.retry(RetryScope.CRAWL)
        } catch (e: InterruptedException) {
            log.warn("Interrupted when retrieve task result", e)
            status = ProtocolStatus.retry(RetryScope.CRAWL)
        } catch (e: Exception) {
            log.warn("Unexpected exception", e)
            status = ProtocolStatus.STATUS_EXCEPTION
        }

        return FetchResult(task, ForwardingResponse(task.url, "", status, headers, task.page))
    }

    private fun logBeforeBatchStart(batch: FetchTaskBatch) {
        if (log.isInfoEnabled) {
            val proxy = driverManager.proxyManager.currentProxyEntry
            val proxyMessage = if (proxy == null) "" else " with expected proxy " + proxy.display
            log.info("Start task batch {} with {} pages in parallel{}", batch.batchId, batch.batchSize, proxyMessage)
        }
    }

    private fun logIdleLongTime(batch: FetchTaskBatch) {
        log.warn("Batch {} round {} takes long time, {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}",
                batch.batchId, batch.round,
                batch.workingTasks.size, batch.finishedTasks.size, batch.numTasksFailed,
                batch.idleSeconds, batch.idleTimeout)
    }

    private fun logAfterTaskFailed(batch: FetchTaskBatch, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            log.info("Batch {} round {} task failed, {} in {}, {}, total {} failed | {}",
                    batch.batchId, String.format("%2d", batch.round),
                    Strings.readableBytes(response.length()),
                    DateTimes.readableDuration(elapsed),
                    response.status,
                    batch.numTasksFailed,
                    url
            )
        }
    }

    private fun logAfterTaskSuccess(batch: FetchTaskBatch, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            val httpCode = response.httpCode
            val length = response.length()
            val codeMessage = if (httpCode != 200) " with code $httpCode" else ""
            log.info("Batch {} round {} fetched{}{} in {}{} | {}",
                    batch.batchId, String.format("%2d", batch.round),
                    if (length < 2000) " only " else " ",
                    Strings.readableBytes(length),
                    DateTimes.readableDuration(elapsed),
                    codeMessage, url)
        }
    }

    private fun logAfterBatchAbort(batch: FetchTaskBatch, state: FetchTaskBatch.State) {
        val proxyDisplay = (batch.lastSuccessProxy ?: batch.lastFailedProxy)?.display
        log.warn("Batch {} is aborted ({}), finished: {}, pending: {}, failed: {}, total: {}, idle: {}s | {}",
                batch.batchId, state,
                batch.numFinishedTasks, batch.numWorkingTasks, batch.numTasksFailed, batch.batchSize,
                batch.idleSeconds, proxyDisplay ?: "(all failed)")
    }

    private fun logAfterBatchFinish(batch: FetchTaskBatch) {
        if (log.isInfoEnabled) {
            val primeBatch = batch.headNode
            val stat = primeBatch.universalStat
            // val proxyDisplay = (bc.lastSuccessProxy?:bc.lastFailedProxy)?.display
            val successProxy = primeBatch.lastSuccessProxy
            val failedProxy = primeBatch.lastFailedProxy
            val proxyDisplay = when {
                successProxy != null -> successProxy.display
                failedProxy != null -> "${failedProxy.display}(failed)"
                else -> null
            }
            log.info(String.format("Batch %d is finished with %d/%d tasks in %s(%.2f pages/s) | time: %s/p, size: %s/p, speed: %s/s | %s",
                    primeBatch.batchId,
                    stat.numTasksSuccess, primeBatch.batchSize,
                    DateTimes.readableDuration(stat.elapsedTime), stat.pagesPerSecond,
                    DateTimes.readableDuration(stat.timePerPage),
                    Strings.readableBytes(stat.bytesPerPage.roundToLong()),
                    Strings.readableBytes(stat.bytesPerSecond.roundToLong()),
                    proxyDisplay?:"(no proxy)"
            ))
            log.info("Proxy: $proxyManager")
            log.info("Drivers: $driverManager")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.info("Proxy: $proxyManager")
            log.info("Drivers: $driverManager")
        }
    }

    companion object {
        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()
    }
}
