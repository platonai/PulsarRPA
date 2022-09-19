package ai.platon.pulsar.protocol.browser.emulator.impl

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.persist.ext.browseEvent
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulatedFetcher
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulator
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BrowserEmulatedFetcherImpl(
    override val privacyManager: PrivacyManager,
    override val driverPoolManager: WebDriverPoolManager,
    override val browserEmulator: BrowserEmulator,
    private val immutableConfig: ImmutableConfig,
    private val closeCascaded: Boolean = false
): BrowserEmulatedFetcher {
    private val logger = LoggerFactory.getLogger(BrowserEmulatedFetcherImpl::class.java)!!

    private val closed = AtomicBoolean()
    private val illegalState = AtomicBoolean()
    private val isActive get() = !illegalState.get() && !closed.get() && AppContext.isActive

    enum class EventType {
        willFetch,
        fetched
    }

    override fun fetch(url: String) = fetchContent(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    override fun fetch(url: String, conf: VolatileConfig) = fetchContent(WebPage.newWebPage(url, conf))

    /**
     * Fetch page content
     * */
    override fun fetchContent(page: WebPage): Response = runBlocking {
        fetchContentDeferred(page)
    }

    override suspend fun fetchDeferred(url: String) =
        fetchContentDeferred(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    override suspend fun fetchDeferred(url: String, volatileConfig: VolatileConfig) =
        fetchContentDeferred(WebPage.newWebPage(url, volatileConfig))

    /**
     * Fetch page content
     * */
    override suspend fun fetchContentDeferred(page: WebPage): Response {
        if (!isActive) {
            return ForwardingResponse.canceled(page)
        }

        if (page.isInternal) {
            logger.warn("Unexpected internal page | {}", page.url)
            return ForwardingResponse.canceled(page)
        }

        val task = FetchTask.create(page)
        return fetchTaskDeferred(task)
    }

    /**
     * Fetch page content
     * */
    private suspend fun fetchTaskDeferred(task: FetchTask): Response {
        return privacyManager.run(task) { _, driver -> doFetch(task, driver) }.response
    }

    private suspend fun doFetch(task: FetchTask, driver: WebDriver): FetchResult {
        if (!isActive) {
            return FetchResult.canceled(task)
        }

        dispatchEvent(EventType.willFetch, task.page, driver)
//        notify("onWillFetch") { event?.onWillFetch?.invoke(task.page, driver) }

        val result = browserEmulator.fetch(task, driver)

        dispatchEvent(EventType.fetched, task.page, driver)
//        notify("onFetched") { event?.onFetched?.invoke(task.page, driver) }

        return result
    }

    override fun reset() {
        TODO("Not implemented")
    }

    override fun cancel(page: WebPage) {
        TODO("Not implemented")
    }

    override fun cancelAll() {
        TODO("Not implemented")
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (closeCascaded) {
                browserEmulator.close()
                driverPoolManager.close()
                privacyManager.close()
            }
        }
    }

    private suspend fun dispatchEvent(type: EventType, page: WebPage, driver: WebDriver) {
        val event = page.browseEvent ?: return
        when(type) {
            EventType.willFetch -> notify(type.name) { event.onWillFetch(page, driver) }
            EventType.fetched -> notify(type.name) { event.onFetched(page, driver) }
            else -> {}
        }
    }

    private suspend fun notify(name: String, action: suspend () -> Unit) {
        if (!isActive) {
            return
        }

        try {
            action()
        } catch (e: WebDriverCancellationException) {
            logger.info("Web driver is cancelled")
        } catch (e: WebDriverException) {
            logger.warn(e.brief("[Ignored][$name] "))
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }
    }
}
