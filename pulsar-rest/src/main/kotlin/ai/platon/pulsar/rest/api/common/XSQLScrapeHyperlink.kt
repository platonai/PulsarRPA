package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.PulsarParams.VAR_IS_SCRAPE
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.GoraWebPage
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.skeleton.crawl.event.impl.DefaultCrawlEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.DefaultLoadEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.PageEventHandlersFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import java.util.*

open class XSQLScrapeHyperlink(
    request: ScrapeRequest,
    sql: NormXSQL,
    session: PulsarSession,
    uuid: String = UUID.randomUUID().toString()
) : AbstractScrapeHyperlink(request, sql, session, uuid) {

    class CrawlEventHandlers(
        val hyperlink: XSQLScrapeHyperlink,
        val response: ScrapeResponse,
    ) : DefaultCrawlEventHandlers() {
        init {
            onWillLoad.addLast {
                // println("crawl-onWillLoad")
                response.pageStatusCode = ResourceStatus.SC_PROCESSING
                it
            }
            onLoaded.addLast { url, page ->
                // println("crawl-onLoaded")
                if (!hyperlink.isDone) {
                    hyperlink.complete(page ?: GoraWebPage.NIL)
                }
            }
        }
    }
    
    class LoadEventHandlers(
        val hyperlink: XSQLScrapeHyperlink,
        val response: ScrapeResponse,
    ) : DefaultLoadEventHandlers() {
        init {
            onWillLoad.addLast {
                // println("onWillLoad")
                response.pageStatusCode = ResourceStatus.SC_PROCESSING
                null
            }
            onWillParseHTMLDocument.addLast { page ->
                // println("onWillParseHTMLDocument")
                page.variables[VAR_IS_SCRAPE] = true
                null
            }
            onWillParseHTMLDocument.addLast { page ->
                // println("onWillParseHTMLDocument")
            }
            onHTMLDocumentParsed.addLast { page, document ->
                // println("onHTMLDocumentParsed")
                require(page.hasVar(VAR_IS_SCRAPE))
                hyperlink.extract(page, document)
            }
            onLoaded.addLast { page ->
                // println("onLoaded")
                // should complete in crawlEvent.onLoaded()
                // hyperlink.complete(page)
            }
        }
    }

    private val logger = getLogger(XSQLScrapeHyperlink::class)

    override var args: String? = "-parse ${sql.args}"
    override var eventHandlers = PageEventHandlersFactory.create(
        loadEventHandlers = LoadEventHandlers(this, response),
        crawlEventHandlers = CrawlEventHandlers(this, response),
    )

    open fun extract(page: WebPage, document: FeaturedDocument) {
        try {
            response.pageContentBytes = page.contentLength.toInt()
            response.pageStatusCode = page.protocolStatus.minorCode
            
            // logger.info("Extracting {} | {}", page.protocolStatus, page.url)
            
            if (page.protocolStatus.isSuccess) {
                doExtract(page, document)
            }
        } catch (t: Throwable) {
            // Log the exception and throw it.
            logger.warn("Unexpected exception", t)
            throw t
        }
    }

    protected open fun doExtract(page: WebPage, document: FeaturedDocument) {
        if (!page.protocolStatus.isSuccess || page.contentLength == 0L || page.content == null) {
            logger.info("No content | {}", page.url)
            response.statusCode = ResourceStatus.SC_NO_CONTENT
        }

        val rs = executeQuery(request, response)
        response.resultSet = ResultSetUtils.getEntitiesFromResultSet(rs)
    }
}
