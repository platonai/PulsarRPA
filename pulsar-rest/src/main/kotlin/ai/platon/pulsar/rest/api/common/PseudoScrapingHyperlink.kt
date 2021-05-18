package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.urls.PseudoUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.AbstractCrawlEventHandler
import ai.platon.pulsar.crawl.CrawlEventHandler
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import org.slf4j.LoggerFactory
import java.util.*

class PseudoSinkAwareCrawlEventHandler(hyperlink: PseudoSinkAwareHyperlink) : AbstractCrawlEventHandler() {
    override var onAfterLoad: (UrlAware, WebPage) -> Unit = { url, page ->
        try {
            hyperlink.executeQuery()
        } catch (t: Throwable) {
            hyperlink.commit(page)
        }
    }
}

/**
 * Never go to load phrase, LoadComponent.load is never used for a pseudo link
 * */
open class PseudoSinkAwareHyperlink(
    request: ScrapeRequest,
    session: PulsarSession,
    globalCache: GlobalCache,
    uuid: String = UUID.randomUUID().toString(),
) : ScrapingHyperlink(request, PseudoXSQL(uuid, sql = request.sql), session, globalCache, uuid), PseudoUrl {
    private val logger = LoggerFactory.getLogger(PseudoSinkAwareHyperlink::class.java)
    override var args: String? = "-taskId $uuid ${sql.args}"
    override var crawlEventHandler: CrawlEventHandler? = PseudoSinkAwareCrawlEventHandler(this)

    override fun commit(page: WebPage) {
        if (isCancelled.get()) {
            isDone.countDown()
            return
        }

        try {
            // TODO: how to properly retrieve the following value?
            response.pageContentBytes = 1
            response.pageStatusCode = 200
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        } finally {
            isDone.countDown()
        }
    }
}
