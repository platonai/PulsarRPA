package ai.platon.pulsar.skeleton.crawl.impl

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.persist.ext.eventHandlers
import ai.platon.pulsar.skeleton.crawl.Crawler
import ai.platon.pulsar.skeleton.crawl.GlobalEventHandlers
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableUrl
import ai.platon.pulsar.skeleton.session.PulsarSession
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

enum class CrawlEvents {
    filter,
    willLoad,
    load,
    loaded
}

abstract class AbstractCrawler(
    val session: PulsarSession,
    val autoClose: Boolean = true
): Crawler, AbstractEventEmitter<CrawlEvents>() {
    companion object {
        private val instanceSequencer = AtomicInteger()
    }

    override val id = instanceSequencer.incrementAndGet()

    override val name: String get() = this.javaClass.simpleName

    override var retryDelayPolicy: (Int, UrlAware?) -> Duration = { nextRetryNumber, url ->
        // Duration.ofMinutes(1L + 2 * nextRetryNumber)
        Duration.ofSeconds(30 + Random.nextLong(15))
    }

    protected var isPaused = false

    protected val closed = AtomicBoolean()

    open val isActive get() = !closed.get() && AppContext.isActive && session.context.isActive

    init {
        attach()
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
    }

    override fun report() {
        // Nothing to do
    }

    override fun onWillLoad(url: UrlAware) {
        if (url is ListenableUrl) {
            GlobalEventHandlers.pageEventHandlers?.crawlEventHandlers?.onWillLoad?.invoke(url)
            // The more specific handlers has the opportunity to override the result of more general handlers.
            url.eventHandlers.crawlEventHandlers.onWillLoad(url)
        }
    }

    override fun onLoad(url: UrlAware) {
//        if (url is ListenableUrl) {
//            GlobalEventHandlers.pageEventHandlers?.crawlEventHandlers?.onLoad?.invoke(url)
//            // The more specific handlers has the opportunity to override the result of more general handlers.
//            url.eventHandlers.crawlEventHandlers.onLoad(url)
//        }
    }

    override fun onLoaded(url: UrlAware, page: WebPage?) {
        GlobalEventHandlers.pageEventHandlers?.crawlEventHandlers?.onLoaded?.invoke(url, page)

        val event = page?.eventHandlers?.crawlEventHandlers
        if (event != null) {
            // The more specific handlers has the opportunity to override the result of more general handlers.
            event.onLoaded(url, page)
        } else if (url is ListenableUrl) {
            url.eventHandlers.crawlEventHandlers.onLoaded(url, page)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            detach()
            if (autoClose) {
                session.close()
            }
        }
    }

    private fun attach() {
        on(CrawlEvents.willLoad) { url: UrlAware -> this.onWillLoad(url) }
        on(CrawlEvents.load) { url: UrlAware -> this.onLoad(url) }
        on(CrawlEvents.loaded) { url: UrlAware, page: WebPage? -> this.onLoaded(url, page) }
    }

    private fun detach() {
        off(CrawlEvents.willLoad)
        off(CrawlEvents.load)
        off(CrawlEvents.loaded)
    }
}
