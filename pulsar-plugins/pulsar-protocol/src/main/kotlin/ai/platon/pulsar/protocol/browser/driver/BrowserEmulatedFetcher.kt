package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_DRIVER_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.common.silent
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.FetchTaskBatch
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.protocol.browser.driver.async.AsyncBrowserEmulator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.openqa.selenium.NoSuchSessionException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class BrowserEmulatedFetcher(
        val privacyContextManager: BrowserPrivacyContextManager,
        private val browserEmulator: BrowserEmulator,
        private val asyncBrowserEmulator: AsyncBrowserEmulator,
        private val immutableConfig: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatedFetcher::class.java)!!

    private val driverManager = privacyContextManager.driverManager
    private val proxyManager = driverManager.proxyManager
    private val closed = AtomicBoolean()
    private val isClosed get() = closed.get()
    private val isAlive get() = !isClosed

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

        if (page.isInternal) {
            log.warn("Unexpected internal page")
            return ForwardingResponse.canceled(page)
        }

        return try {
            privacyContextManager.run(createFetchTask(page)) { task, driver ->
                try {
                    browserEmulator.fetch(task, driver)
                } catch (e: IllegalContextStateException) {
                    log.info("Illegal context state, task is cancelled | {}", task.url)
                    FetchResult(task, ForwardingResponse.canceled(task.page))
                }
            }.response
        } catch (e: ProxyException) {
            ForwardingResponse.retry(page, RetryScope.CRAWL)
        } catch (e: NoSuchSessionException) {
            ForwardingResponse.retry(page, RetryScope.CRAWL)
        } catch (e: Throwable) {
            log.warn("Unexpected throwable", e)
            ForwardingResponse.failed(page, e)
        } finally {
        }
    }

    suspend fun fetchDeferred(url: String): Response {
        return fetchContentDeferred(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))
    }

    suspend fun fetchDeferred(url: String, volatileConfig: VolatileConfig): Response {
        return fetchContentDeferred(WebPage.newWebPage(url, volatileConfig))
    }

    /**
     * Fetch page content
     * */
    suspend fun fetchContentDeferred(page: WebPage): Response {
        require(page.isNotInternal) { "Internal page ${page.url}" }
        if (isClosed) {
            return ForwardingResponse.canceled(page)
        }

        return try {
            privacyContextManager.submit(createFetchTask(page)) { task, driver ->
                try {
                    asyncBrowserEmulator.fetch(task, driver)
                } catch (e: IllegalContextStateException) {
                    log.info("Illegal context state, task is cancelled | {}", task.url)
                    FetchResult(task, ForwardingResponse.canceled(task.page))
                }
            }.response
        } catch (e: ProxyException) {
            ForwardingResponse.retry(page, RetryScope.CRAWL)
        } catch (e: NoSuchSessionException) {
            ForwardingResponse.retry(page, RetryScope.CRAWL)
        } catch (e: Throwable) {
            log.warn("Unexpected throwable", e)
            ForwardingResponse.failed(page, e)
        } finally {
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

    private fun createFetchTask(page: WebPage): FetchTask {
        val volatileConfig = page.volatileConfig ?: immutableConfig.toVolatileConfig()
        val priority = volatileConfig.getUint(BROWSER_DRIVER_PRIORITY, 0)
        return FetchTask(0, priority, page, volatileConfig)
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

                parallelFetch0(b)
            } while (i++ <= privacyContextManager.maxRetry && privacyContextManager.activeContext.isPrivacyLeaked)

            batch.afterFetchAll(batch.pages)

            logAfterBatchFinish(batch)

            return batch.collectResponses()
        }
    }

    private fun parallelFetch0(batch: FetchTaskBatch) {
        GlobalScope.launch {
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

            silent { TimeUnit.SECONDS.sleep(1) }

            state = checkState(batch)
        } while (state == FetchTaskBatch.State.RUNNING)

        if (batch.workingTasks.isNotEmpty()) {
            abortBatchTasks(batch, state)
        }
    }

    private fun checkState(batch: FetchTaskBatch): FetchTaskBatch.State {
        return when {
            isClosed -> FetchTaskBatch.State.CLOSED
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
            driverManager.allocate(requiredDrivers, volatileConfig)
        }
    }

    private suspend fun parallelFetch1(task: FetchTask, batch: FetchTaskBatch): FetchResult {
        return privacyContextManager.activeContext.submit(task) { _, driver ->
            try {
                batch.beforeFetch(task.page)
                asyncBrowserEmulator.fetch(task, driver)
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

    private fun abortBatchTasks(batch: FetchTaskBatch, state: FetchTaskBatch.State) {
        logAfterBatchAbort(batch, state)

        // If there are still pending tasks, cancel them
        batch.workingTasks.forEach { browserEmulator.cancel(it.key) }

        // Wait until all worker thread complete normally or timeout
        var tick = 0
        val timeout = Duration.ofMinutes(2).seconds
        while (batch.workingTasks.isNotEmpty() && tick++ < timeout) {
            checkAndHandleTasksAbort(batch)
            silent { TimeUnit.SECONDS.sleep(1) }
        }

        // Finally, if there are still working tasks, force abort the worker threads
        if (batch.workingTasks.isNotEmpty()) {
            log.warn("There are still {} working tasks, cancel worker threads", batch.numWorkingTasks)
            batch.workingTasks.forEach { it.value.cancel() }
            checkAndHandleTasksAbort(batch)
        }

        if (batch.workingTasks.isNotEmpty()) {
            log.warn("There are still {} working tasks unexpectedly", batch.numWorkingTasks)
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
