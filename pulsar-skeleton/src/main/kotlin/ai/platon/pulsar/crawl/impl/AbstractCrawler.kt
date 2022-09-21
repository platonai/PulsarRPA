package ai.platon.pulsar.crawl.impl

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.common.persist.ext.event
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.Crawler
import ai.platon.pulsar.crawl.common.url.ListenableUrl
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.PulsarSession
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class CrawlEvents {
    filter,
    willLoad,
    load,
    loaded
}

abstract class AbstractCrawler(
    val session: PulsarSession = PulsarContexts.createSession(),
    val autoClose: Boolean = true
): Crawler, AbstractEventEmitter<CrawlEvents>() {
    companion object {
        private val instanceSequencer = AtomicInteger()
    }

    override val id = instanceSequencer.incrementAndGet()

    override val name: String get() = this.javaClass.simpleName

    override var retryDelayPolicy: (Int, UrlAware?) -> Duration = { nextRetryNumber, url ->
        Duration.ofMinutes(1L + 2 * nextRetryNumber)
    }

    val closed = AtomicBoolean()

    open val isActive get() = !closed.get() && AppContext.isActive

    init {
        attach()
    }

    override fun onWillLoad(url: UrlAware) {
        if (url is ListenableUrl) {
            url.event.crawlEvent.onWillLoad(url)
        }
    }

    override fun onLoad(url: UrlAware) {
        if (url is ListenableUrl) {
            url.event.crawlEvent.onLoad(url)
        }
    }

    override fun onLoaded(url: UrlAware, page: WebPage?) {
        if (url is ListenableUrl) {
            url.event.crawlEvent.onLoaded(url, page)
        }

        page?.event?.crawlEvent?.onLoaded?.invoke(url, page)
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
