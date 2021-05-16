package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_WEB_DRIVER_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.FetchTaskBatch
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.emulator.context.BrowserPrivacyContext
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BrowserEmulatedFetcher(
        private val privacyManager: PrivacyManager,
        private val driverManager: WebDriverPoolManager,
        private val browserEmulator: BrowserEmulator,
        private val immutableConfig: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatedFetcher::class.java)!!

    private val closed = AtomicBoolean()
    private val illegalState = AtomicBoolean()
    private val isActive get() = !illegalState.get() && !closed.get()

    fun fetch(url: String) = fetchContent(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    fun fetch(url: String, conf: VolatileConfig) = fetchContent(WebPage.newWebPage(url, conf))

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage) = runBlocking { fetchContentDeferred(page) }

    suspend fun fetchDeferred(url: String) =
        fetchContentDeferred(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    suspend fun fetchDeferred(url: String, volatileConfig: VolatileConfig) =
        fetchContentDeferred(WebPage.newWebPage(url, volatileConfig))

    /**
     * Fetch page content
     * */
    suspend fun fetchContentDeferred(page: WebPage): Response {
        if (!isActive) {
            return ForwardingResponse.canceled(page)
        }

        if (page.isInternal) {
            log.warn("Unexpected internal page | {}", page.url)
            return ForwardingResponse.canceled(page)
        }

        val task = createFetchTask(page)
        return fetchTaskDeferred(task)
    }

    /**
     * Fetch page content
     * */
    private suspend fun fetchTaskDeferred(task: FetchTask): Response {
        return privacyManager.run(task) { _, driver ->
            try {
                browserEmulator.fetch(task, driver)
            } catch (e: IllegalApplicationContextStateException) {
                if (illegalState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.info("Illegal context state | {} | {}", driverManager.formatStatus(driver.browserInstanceId), task.url)
                }
                throw e
            }
        }.response
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>): List<Response> {
        val conf = immutableConfig.toVolatileConfig()
        return parallelFetchAllPages0(batchId, urls.map { WebPage.newWebPage(it, conf) }, conf)
    }

    fun fetchAll(urls: Iterable<String>) = fetchAll(nextBatchId, urls)

    fun fetchAll(batchId: Int, urls: Iterable<String>, conf: VolatileConfig) =
        parallelFetchAllPages0(batchId, urls.map { WebPage.newWebPage(it, conf) }, conf)

    fun fetchAll(urls: Iterable<String>, conf: VolatileConfig) = fetchAll(nextBatchId, urls, conf)

    /**
     * TODO: do not support parallel fetching, parallel fetching should be supported at higher level
     * */
    fun parallelFetchAll(urls: Iterable<String>, conf: VolatileConfig) =
        parallelFetchAllPages0(nextBatchId, urls.map { WebPage.newWebPage(it, conf) }, conf)

    /**
     * TODO: do not support parallel fetching, parallel fetching should be supported at higher level
     * */
    fun parallelFetchAll(batchId: Int, urls: Iterable<String>, conf: VolatileConfig) =
        parallelFetchAllPages0(batchId, urls.map { WebPage.newWebPage(it, conf) }, conf)

    /**
     * TODO: do not support parallel fetching, parallel fetching should be supported at higher level
     * */
    fun parallelFetchAllPages(pages: Iterable<WebPage>, conf: VolatileConfig) =
        parallelFetchAllPages0(nextBatchId, pages, conf)

    fun reset() {
        TODO("Not implemented")
    }

    fun cancel(page: WebPage) {
        TODO("Not implemented")
    }

    fun cancelAll() {
        TODO("Not implemented")
    }

    private fun createFetchTask(page: WebPage): FetchTask {
        val conf = page.conf
        val priority = conf.getUint(BROWSER_WEB_DRIVER_PRIORITY, 0)
        return FetchTask(0, priority, page, conf)
    }

    private fun parallelFetchAllPages0(batchId: Int, pages: Iterable<WebPage>, volatileConfig: VolatileConfig): List<Response> {
        val privacyContext = privacyManager.computeNextContext()

        FetchTaskBatch(batchId, pages, volatileConfig, privacyContext).use { batch ->
            // allocate drivers before batch fetch context timing
            // allocateDriversIfNecessary(batch, batch.conf)

            logBeforeBatchStart(batch)

            batch.universalStat.startTime = Instant.now()
            batch.beforeFetchAll(batch.pages)

            runBlocking {
                var i = 1
                do {
                    var b = batch
                    val privacyContext0 = b.privacyContext
                    if (privacyContext0.isLeaked) {
                        b = b.createNextNode(privacyManager.computeNextContext())
                    }

                    parallelFetch0(b)
                } while (i++ <= 2 && privacyContext0.isLeaked)
            }

            batch.afterFetchAll(batch.pages)

            logAfterBatchFinish(batch)

            return batch.collectResponses()
        }
    }

    private suspend fun parallelFetch0(batch: FetchTaskBatch) {
        supervisorScope {
            batch.createTasks().associateWithTo(batch.workingTasks) { async { parallelFetch1(it, batch) } }
        }

        var state: FetchTaskBatch.State
        do {
            ++batch.round

            var numCompleted = 0
            val it = batch.workingTasks.iterator()
            it.forEachRemaining { (task, deferred) ->
                if (deferred.isCompleted) {
                    handleTaskDone(task, deferred, batch)
                    ++numCompleted
                    it.remove()
                }
            }

            batch.idleSeconds = if (numCompleted == 0) 1 + batch.idleSeconds else 0

            if (batch.round >= 60 && batch.round % 30 == 0L) {
                logIdleLongTime(batch)
            }

            sleepSeconds(1)

            state = checkState(batch)
        } while (state == FetchTaskBatch.State.RUNNING)

        if (batch.workingTasks.isNotEmpty()) {
            abortBatchTasks(batch, state)
        }
    }

    private fun checkState(batch: FetchTaskBatch): FetchTaskBatch.State {
        return when {
            !isActive -> FetchTaskBatch.State.CLOSED
            Thread.currentThread().isInterrupted -> FetchTaskBatch.State.INTERRUPTED
            batch.privacyContext.isLeaked -> FetchTaskBatch.State.PRIVACY_LEAK
            else -> batch.checkState()
        }
    }

    private suspend fun parallelFetch1(task: FetchTask, batch: FetchTaskBatch): FetchResult {
        val privacyContext = batch.privacyContext as BrowserPrivacyContext
        return privacyContext.run(task) { _, driver ->
            try {
                batch.proxyEntry = task.proxyEntry
                browserEmulator.fetch(task, driver)
            } catch (e: IllegalApplicationContextStateException) {
                if (illegalState.compareAndSet(false, true)) {
                    AppContext.tryTerminate()
                    log.info("Illegal context state, cancel task {}/{} | {}", task.id, task.batchId, task.url)
                }
                FetchResult(task, ForwardingResponse.canceled(task.page))
            } catch (e: TimeoutCancellationException) {
                log.info("Coroutine is timeout and cancelled, cancel task {}/{} | {}", task.id, task.batchId, task.url)
                FetchResult(task, ForwardingResponse.canceled(task.page))
            } catch (e: Throwable) {
                log.warn("Unexpected throwable", e)
                FetchResult(task, ForwardingResponse.failed(task.page, e))
            }
        }
    }

    private fun handleTaskDone(task: FetchTask, deferred: Deferred<FetchResult>, batch: FetchTaskBatch): FetchResult {
        val url = task.url
        try {
            val result = deferred.getCompleted()

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

    private suspend fun abortBatchTasks(batch: FetchTaskBatch, state: FetchTaskBatch.State) {
        logAfterBatchAbort(batch, state)

        // If there are still pending tasks, cancel them
        batch.workingTasks.forEach { browserEmulator.cancel(it.key) }

        // Wait until all worker thread complete normally or timeout
        var tick = 0
        val timeout = Duration.ofMinutes(2).seconds
        while (batch.workingTasks.isNotEmpty() && tick++ < timeout) {
            checkAndHandleTasksAbort(batch)
            sleepSeconds(1)
        }

        // Finally, if there are still working tasks, force abort the worker threads
        if (batch.workingTasks.isNotEmpty()) {
            log.warn("There are still {} working tasks, cancel worker threads", batch.numWorkingTasks)
            batch.workingTasks.forEach { it.value.cancel() }
            checkAndHandleTasksAbort(batch)
        }

        if (batch.workingTasks.isNotEmpty()) {
            log.warn("There are still {} working tasks unexpectely", batch.numWorkingTasks)
            batch.workingTasks.clear()
        }
    }

    private fun checkAndHandleTasksAbort(batch: FetchTaskBatch) {
        val it = batch.workingTasks.iterator()
        it.forEachRemaining { (task, deferred) ->
            if (deferred.isCompleted) {
                handleTaskAbort(task, deferred, batch)
                it.remove()
            }
        }
    }

    private fun handleTaskAbort(task: FetchTask, deferred: Deferred<FetchResult>, batch: FetchTaskBatch): FetchResult {
        val result = deferred.getCompleted()
        batch.onAbort(task, result)
        return result
    }

    private fun logBeforeBatchStart(batch: FetchTaskBatch) {
        if (log.isInfoEnabled) {
            val proxyEntry = batch.proxyEntry
            val proxyMessage = if (proxyEntry == null) "" else " with expected proxy " + proxyEntry.display
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
                    Strings.readableBytes(response.length),
                    elapsed.readable(),
                    response.status,
                    batch.numTasksFailed,
                    url
            )
        }
    }

    private fun logAfterTaskSuccess(batch: FetchTaskBatch, url: String, response: Response, elapsed: Duration) {
        if (log.isInfoEnabled) {
            val httpCode = response.httpCode
            val length = response.length
            val codeMessage = if (httpCode != 200) " with code $httpCode" else ""
            log.info("Batch {} round {} fetched{}{} in {}{} | {}",
                    batch.batchId, String.format("%2d", batch.round),
                    if (length < 2000) " only " else " ",
                    Strings.readableBytes(length),
                    elapsed.readable(),
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
                    stat.elapsedTime.readable(), stat.pagesPerSecond,
                    stat.timePerPage.readable(),
                    Strings.readableBytes(stat.bytesPerPage.roundToLong()),
                    Strings.readableBytes(stat.bytesPerSecond.roundToLong()),
                    proxyDisplay?:"(no proxy)"
            ))
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
