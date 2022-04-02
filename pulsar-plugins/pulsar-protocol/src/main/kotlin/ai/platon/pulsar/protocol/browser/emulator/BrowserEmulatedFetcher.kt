package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_WEB_DRIVER_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    private val logger = LoggerFactory.getLogger(BrowserEmulatedFetcher::class.java)!!

    private val closed = AtomicBoolean()
    private val illegalState = AtomicBoolean()
    private val isActive get() = !illegalState.get() && !closed.get() && AppContext.isActive

    fun fetch(url: String) = fetchContent(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    fun fetch(url: String, conf: VolatileConfig) = fetchContent(WebPage.newWebPage(url, conf))

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage) = runBlocking {
        // val pageLoadTimeout = EmulateSettings(page.conf).pageLoadTimeout.toMillis()
//        withTimeout(pageLoadTimeout) { fetchContentDeferred(page) }
        fetchContentDeferred(page)
    }

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
            logger.warn("Unexpected internal page | {}", page.url)
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
                    AppContext.shouldTerminate()
                    logger.info("Illegal context state | {} | {}", driverManager.formatStatus(driver.browserInstanceId), task.url)
                }
                throw e
            }
        }.response
    }

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

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }

    companion object {
        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()
    }
}
