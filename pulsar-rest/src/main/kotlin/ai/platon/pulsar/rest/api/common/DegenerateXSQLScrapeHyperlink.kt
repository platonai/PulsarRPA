package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.DegenerateUrl
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Never go to load phase, `LoadComponent.load()` is never used for a degenerate link
 * */
open class DegenerateXSQLScrapeHyperlink(
    request: ScrapeRequest,
    session: PulsarSession,
    globalCacheFactory: GlobalCacheFactory,
    uuid: String = UUID.randomUUID().toString(),
) : XSQLScrapeHyperlink(request, DegenerateXSQL(uuid, sql = request.sql), session, globalCacheFactory, uuid), DegenerateUrl {
    private val logger = LoggerFactory.getLogger(DegenerateXSQLScrapeHyperlink::class.java)
    override var args: String? = "-taskId $uuid ${sql.args}"

    init {
        registerEventHandler()
    }

    override fun complete(page: WebPage) {
        try {
            // TODO: properly retrieve the following value
            if (page.isNil) {
                response.pageContentBytes = 0
                response.pageStatusCode = ResourceStatus.SC_EXPECTATION_FAILED
            } else {
                response.pageContentBytes = 1
                response.pageStatusCode = 200
            }
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        } finally {
            response.isDone = true
        }
    }

    private fun registerEventHandler() {
        event.crawlEvent.onLoaded.addLast { url, page ->
            try {
                executeQuery()
            } catch (t: Throwable) {
                getLogger(this).warn("Unexpected exception", t)
            } finally {
                complete(page ?: MutableWebPage.NIL)
            }
        }
    }
}
