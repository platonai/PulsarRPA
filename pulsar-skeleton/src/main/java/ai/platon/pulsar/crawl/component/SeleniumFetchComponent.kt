package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT
import ai.platon.pulsar.common.config.CapabilityTypes.SELENIUM_WEB_DRIVER_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.net.browser.FetchTaskContext
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
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

internal class BatchFetchContext(
        val batchId: Int,
        val pages: Iterable<WebPage>,
        val volatileConfig: VolatileConfig
) {
    val startTime = Instant.now()
    val priority = volatileConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)!!
    // The function must return in a reasonable time
    val threadTimeout = volatileConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT).plusSeconds(10)
    val interval = Duration.ofSeconds(1)
    val idleTimeout = Duration.ofMinutes(2).seconds
    val numTotalTasks = Iterables.size(pages)
    val numAllowedFailures = max(10, numTotalTasks / 3)

    // All submitted tasks
    val pendingTasks = mutableMapOf<String, Future<Response>>()
    val finishedTasks = mutableMapOf<String, Response>()

    val numFinishedTasks get() = finishedTasks.size
    val numPendingTasks get() = pendingTasks.size

    // Submit all tasks
    var i: Int = 0
    var numTaskDone = 0
    var numFailedTasks = 0
    var idleSeconds = 0
    var totalBytes = 0

    fun beforeFetch(page: WebPage) {
    }

    fun afterFetch(page: WebPage) {
//        val onFetchComplete = volatileConfig.getBean(Function0::class.java)
//        onFetchComplete.reflect()
    }

    fun beforeBatch(pages: Iterable<WebPage>) {
    }

    fun afterBatch(pages: Iterable<WebPage>) {
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
): Parameterized, AutoCloseable {
    val log = LoggerFactory.getLogger(SeleniumFetchComponent::class.java)!!

    private val isClosed = AtomicBoolean(false)

    fun fetch(url: String): Response {
        return fetchContent(WebPage.newWebPage(url, false))
    }

    fun fetch(url: String, volatileConfig: VolatileConfig): Response {
        return fetchContent(WebPage.newWebPage(url, false, volatileConfig))
    }

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage): Response {
        val volatileConfig = page.volatileConfig ?: immutableConfig.toVolatileConfig()
        val priority = volatileConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
        return seleniumEngine.fetchContentInternal(FetchTaskContext(0, nextTaskId, priority, page, volatileConfig))
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>): Collection<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it) }, immutableConfig.toVolatileConfig())
    }

    fun fetchAll(urls: Iterable<String>): Collection<Response> {
        return fetchAll(nextBatchId, urls)
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>, volatileConfig: VolatileConfig): Collection<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it) }, volatileConfig)
    }

    fun fetchAll(urls: Iterable<String>, volatileConfig: VolatileConfig): Collection<Response> {
        return fetchAll(nextBatchId, urls, volatileConfig)
    }

    fun parallelFetchAll(urls: Iterable<String>, volatileConfig: VolatileConfig): Collection<Response> {
        // return parallelFetchAllPages(urls.map { WebPage.newWebPage(it) }, volatileConfig)
        return parallelFetchAllPages(nextBatchId, urls.map { WebPage.newWebPage(it) }, volatileConfig)
    }

    fun parallelFetchAll(batchId: Int, urls: Iterable<String>, volatileConfig: VolatileConfig): Collection<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it) }, volatileConfig)
    }

    fun parallelFetchAllPages(pages: Iterable<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        return parallelFetchAllPages(nextBatchId, pages, volatileConfig)
    }

    private fun parallelFetchAllPages(batchId: Int, pages: Iterable<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        val context = BatchFetchContext(batchId, pages, volatileConfig)

        log.info("Start batch task {} with {} pages in parallel", batchId, context.numTotalTasks)

        context.beforeBatch(pages)

        // Submit all tasks
        var i = 0
        pages.associateTo(context.pendingTasks) { it.url to executor.submit { doFetch(++i, it, context) } }

        // since the urls in the batch are usually from the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        i = 0
        while (context.numTaskDone < context.numTotalTasks
                && context.numFailedTasks <= context.numAllowedFailures
                && context.idleSeconds < context.idleTimeout
                && !isClosed.get()
                && !Thread.currentThread().isInterrupted) {
            ++i

            if (i >= 60 && i % 30 == 0) {
                // report every 30 round if it takes long time
                log.warn("Batch {} takes long time - round {} - {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}s",
                        batchId, i, context.pendingTasks.size, context.finishedTasks.size, context.numFailedTasks, context.idleSeconds, context.idleTimeout)
            }

            if (context.pendingTasks.size == 1 && context.idleSeconds >= 30 && context.numTotalTasks > 10) {
                // only one very slow task, cancel it
            }

            // loop and wait for all parallel tasks return
            val removal = mutableListOf<String>()
            context.pendingTasks.asSequence().filter { it.value.isDone }.forEach { (url, future) ->
                handleTaskDone(i, url, future, context)
                removal.add(url)
            }
            removal.forEach { context.pendingTasks.remove(it) }

            if (context.numFinishedTasks > 5) {
                // We may do something eagerly
            }

            context.idleSeconds = if (context.numTaskDone == 0) 1 + context.idleSeconds else 0

            // TODO: Use reactor, coroutine or signal instead of sleep
            try {
                TimeUnit.SECONDS.sleep(context.interval.seconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Selenium interrupted, {} pending tasks will be canceled", context.pendingTasks.size)
            }
        }

        if (context.pendingTasks.isNotEmpty()) {
            handleIncomplete(context)
        }

        logBatchFinished(context)

        context.afterBatch(pages)

        return context.finishedTasks.values
    }

    private fun doFetch(i: Int, page: WebPage, context: BatchFetchContext): Response {
        context.beforeFetch(page)
        val taskContext = FetchTaskContext(context.batchId, i, context.priority, page, context.volatileConfig)

        return try {
            seleniumEngine.fetchContentInternal(taskContext)
        } finally {
            context.afterFetch(page)
        }
    }

    private fun handleTaskDone(i: Int, url: String, future: Future<Response>, context: BatchFetchContext): String {
        if (isClosed.get() || Thread.currentThread().isInterrupted) {
            return url
        }

        try {
            val response = seleniumEngine.getResponse(url, future, context.threadTimeout)
            context.totalBytes += response.size()
            context.finishedTasks[url] = response

            val elapsed = Duration.ofSeconds(i.toLong()) // response.headers
            val isFailed = response.code !in arrayOf(ProtocolStatusCodes.SUCCESS_OK, ProtocolStatusCodes.CANCELED)
            if (isFailed) {
                ++context.numFailedTasks
                logTaskFailed(context, url, response, elapsed)
            } else {
                logTaskSuccess(context, url, response, elapsed)
            }

            val onFetchComplete = context.volatileConfig.getBean(Function::class.java)
            if (onFetchComplete != null) {
                // do something
            }
        } catch (e: Throwable) {
            log.error("Unexpected error {}", StringUtil.stringifyException(e))
        } finally {
            ++context.numTaskDone
            // removal.add(key)
        }

        return url
    }

    private fun handleIncomplete(context: BatchFetchContext) {
        logBatchIncomplete(context)

        // if there are still pending tasks, cancel them
        context.pendingTasks.forEach { it.value.cancel(true) }
        context.pendingTasks.forEach { (url, task) ->
            // Attempts to cancel execution of this task
            try {
                val response = seleniumEngine.getResponse(url, task, context.threadTimeout)
                context.finishedTasks[url] = response
            } catch (e: Throwable) {
                log.error("Unexpected error {}", StringUtil.stringifyException(e))
            }
        }
    }

    private fun logTaskFailed(context: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (!log.isInfoEnabled) {
            return
        }

        log.info("Batch {} round {} task failed, reason {}, {} bytes in {}, total {} failed | {}",
                context.batchId, String.format("%2d", context.i),
                ProtocolStatus.getMinorName(response.code), String.format("%,d", response.size()), elapsed,
                context.numFailedTasks,
                url
        )
    }

    private fun logTaskSuccess(context: BatchFetchContext, url: String, response: Response, elapsed: Duration) {
        if (!log.isInfoEnabled) {
            return
        }

        log.info("Batch {} round {} fetched{}{}kb in {} with code {} | {}",
                context.batchId, String.format("%2d", context.i),
                if (context.totalBytes < 2000) " only " else " ", String.format("%,7.2f", response.size() / 1024.0),
                elapsed,
                response.code, url)
    }

    private fun logBatchIncomplete(context: BatchFetchContext) {
        log.warn("Batch task is incomplete, finished tasks {}, pending tasks {}, failed tasks: {}, idle: {}s",
                context.numFinishedTasks, context.numPendingTasks, context.numFailedTasks, context.idleSeconds)
    }

    private fun logBatchFinished(context: BatchFetchContext) {
        if (!log.isInfoEnabled) {
            return
        }

        val elapsed = Duration.between(context.startTime, Instant.now())
        log.info("Batch {} with {} tasks is finished in {}, ave time {}s, ave bytes: {}, speed: {}bps",
                context.batchId, context.numTotalTasks, elapsed,
                String.format("%,.3f", 1.0 * elapsed.seconds / context.numTotalTasks),
                context.totalBytes / context.numTotalTasks,
                String.format("%,.3f", 1.0 * context.totalBytes * 8 / elapsed.seconds)
        )
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
