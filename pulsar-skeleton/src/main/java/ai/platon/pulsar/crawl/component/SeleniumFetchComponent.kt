package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.GlobalExecutor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT
import ai.platon.pulsar.common.config.CapabilityTypes.SELENIUM_WEB_DRIVER_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

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

    fun fetch(url: String, mutableConfig: MutableConfig): Response {
        return fetchContent(WebPage.newWebPage(url, false, mutableConfig))
    }

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage): Response {
        val conf = page.mutableConfig ?: immutableConfig
        val priority = conf.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
        return seleniumEngine.fetchContentInternal(nextBatchId, priority, page, conf)
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>): Collection<Response> {
        return urls.map { this.fetch(it) }
    }

    fun fetchAll(urls: Iterable<String>): Collection<Response> {
        return fetchAll(nextBatchId, urls)
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return urls.map { fetch(it, mutableConfig) }
    }

    fun fetchAll(urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return fetchAll(nextBatchId, urls, mutableConfig)
    }

    fun parallelFetchAll(urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(urls.map { WebPage.newWebPage(it) }, mutableConfig)
    }

    fun parallelFetchAll(batchId: Int, urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it) }, mutableConfig)
    }

    fun parallelFetchAllPages(pages: Iterable<WebPage>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(nextBatchId, pages, mutableConfig)
    }

    private fun parallelFetchAllPages(batchId: Int, pages: Iterable<WebPage>, mutableConfig: MutableConfig): Collection<Response> {
        val startTime = Instant.now()
        val size = Iterables.size(pages)

        log.info("Start batch task {} with {} pages in parallel", batchId, size)

        val priority = mutableConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)!!

        // Create a task submitter
        val submitter = { i: Int, page: WebPage ->
            executor.submit { seleniumEngine.fetchContentInternal(batchId, i, priority, page, mutableConfig) }
        }

        // Submit all tasks
        var i = 0
        val pendingTasks = pages.associateTo(HashMap()) { it.url to submitter(++i, it) }
        val finishedTasks = mutableMapOf<String, Response>()

        // The function must return in a reasonable time
        val threadTimeout = mutableConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT).plusSeconds(10)
        val numTotalTasks = pendingTasks.size
        val interval = Duration.ofSeconds(1)
        val idleTimeout = Duration.ofMinutes(2).seconds
        val numAllowedFailures = max(10, numTotalTasks / 3)

        var numFinishedTasks = 0
        var numFailedTasks = 0
        var idleSeconds = 0
        var bytes = 0

        // since the urls in the batch are usually from the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        i = 0
        while (numFinishedTasks < numTotalTasks && numFailedTasks <= numAllowedFailures && idleSeconds < idleTimeout
                && !isClosed.get() && !Thread.currentThread().isInterrupted) {
            ++i

            if (i >= 60 && i % 30 == 0) {
                // report every 30 round if it takes long time
                log.warn("Batch {} takes long time - round {} - {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}s",
                        batchId, i, pendingTasks.size, finishedTasks.size, numFailedTasks, idleSeconds, idleTimeout)
            }

            if (i >= 60 && pendingTasks.size == 1 && numTotalTasks > 10) {
                // only one very slow task
            }

            // loop and wait for all parallel tasks return
            var numTaskDone = 0
            val removal = mutableListOf<String>()
            pendingTasks.asSequence().filter { it.value.isDone }.forEach { (key, future) ->
                if (isClosed.get() || Thread.currentThread().isInterrupted) {
                    return@forEach
                }

                try {
                    val response = seleniumEngine.getResponse(key, future, threadTimeout)
                    bytes += response.size()
                    val time = Duration.ofSeconds(i.toLong())// response.headers
                    val isFailed = response.code !in arrayOf(ProtocolStatusCodes.SUCCESS_OK, ProtocolStatusCodes.CANCELED)
                    if (isFailed) {
                        ++numFailedTasks
                    }
                    finishedTasks[key] = response

                    if (log.isInfoEnabled) {
                        if (isFailed) {
                            log.info("Batch {} round {} task failed, reason {}, {} bytes in {}, total {} failed | {}",
                                    batchId, String.format("%2d", i),
                                    ProtocolStatus.getMinorName(response.code), String.format("%,d", response.size()), time,
                                    numFailedTasks,
                                    key
                            )
                        } else {
                            log.info("Batch {} round {} fetched{}{} bytes in {} with code {} | {}",
                                    batchId, String.format("%2d", i),
                                    if (bytes < 2000) " only " else " ", String.format("%,7d", response.size()),
                                    time,
                                    response.code, key
                            )
                        }
                    }
                } catch (e: Throwable) {
                    log.error("Unexpected error {}", StringUtil.stringifyException(e))
                } finally {
                    ++numFinishedTasks
                    ++numTaskDone
                    removal.add(key)
                }
            }
            removal.forEach { pendingTasks.remove(it) }

            if (numFinishedTasks > 5) {
                // We may do something eagerly
            }

            idleSeconds = if (numTaskDone == 0) 1 + idleSeconds else 0

            // TODO: Use reactor, coroutine or signal instead of sleep
            try {
                TimeUnit.SECONDS.sleep(interval.seconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Selenium interrupted, {} pending tasks will be canceled", pendingTasks.size)
            }
        }

        if (pendingTasks.isNotEmpty()) {
            log.warn("Batch task is incomplete, finished tasks {}, pending tasks {}, failed tasks: {}, idle: {}s",
                    finishedTasks.size, pendingTasks.size, numFailedTasks, idleSeconds)

            // if there are still pending tasks, cancel them
            pendingTasks.forEach { it.value.cancel(true) }
            pendingTasks.forEach { (url, task) ->
                // Attempts to cancel execution of this task
                try {
                    val response = seleniumEngine.getResponse(url, task, threadTimeout)
                    finishedTasks[url] = response
                } catch (e: Throwable) {
                    log.error("Unexpected error {}", StringUtil.stringifyException(e))
                }
            }
        }

        val elapsed = Duration.between(startTime, Instant.now())
        log.info("Batch {} with {} tasks is finished in {}, ave time {}s, ave bytes: {}, speed: {}bps",
                batchId, size, elapsed,
                String.format("%,.2f", 1.0 * elapsed.seconds / size),
                bytes / size,
                String.format("%,.2f", 1.0 * bytes * 8 / elapsed.seconds)
        )

        return finishedTasks.values
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
    }

    companion object {
        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()
    }
}
