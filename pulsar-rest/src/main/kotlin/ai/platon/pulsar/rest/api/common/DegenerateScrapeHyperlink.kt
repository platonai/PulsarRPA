package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.DegenerateUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.*
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Never go to load phrase, LoadComponent.load is never used for a degenerate link
 * */
open class DegenerateScrapeHyperlink(
    request: ScrapeRequest,
    session: PulsarSession,
    globalCache: GlobalCache,
    uuid: String = UUID.randomUUID().toString(),
) : ScrapeHyperlink(request, DegenerateXSQL(uuid, sql = request.sql), session, globalCache, uuid), DegenerateUrl {
    private val logger = LoggerFactory.getLogger(DegenerateScrapeHyperlink::class.java)
    override var args: String? = "-taskId $uuid ${sql.args}"

//    override val loadEventHandler: LoadEventHandler = EmptyLoadEventHandler()
//    override val jsEventHandler: JsEventHandler = EmptyJsEventHandler()

    init {
        registerEventHandler()
    }

    override fun complete(page: WebPage) {
        try {
            // TODO: how to properly retrieve the following value?
            response.pageContentBytes = 1
            response.pageStatusCode = 200
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        } finally {
            response.isDone = true
        }
    }

    private fun registerEventHandler() {
        crawlEventHandler.onAfterLoadPipeline.addLast { url, page ->
            try {
                executeQuery()
            } catch (t: Throwable) {
                getLogger(this).warn("Unexpected exception", t)
            } finally {
                complete(page ?: WebPage.NIL)
            }
        }
    }
}
