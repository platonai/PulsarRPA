package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.PulsarParams.VAR_IS_SCRAPE
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.common.ResultSets
import ai.platon.pulsar.ql.context.AbstractSQLContext
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.common.url.CompletableListenableHyperlink
import ai.platon.pulsar.skeleton.crawl.event.impl.DefaultCrawlEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.DefaultLoadEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.PageEventHandlersFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.h2.jdbc.JdbcSQLException
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

open class XSQLScrapeHyperlink(
    val request: ScrapeRequest,
    val sql: NormXSQL,
    val session: PulsarSession,
    val uuid: String = UUID.randomUUID().toString()
) : CompletableListenableHyperlink<ScrapeResponse>(sql.url) {
    
    class CrawlEventHandlers(
        val hyperlink: XSQLScrapeHyperlink,
        val response: ScrapeResponse,
    ) : DefaultCrawlEventHandlers() {
        init {
            onWillLoad.addLast {
                response.pageStatusCode = ResourceStatus.SC_PROCESSING
                it
            }
            onLoaded.addLast { url, page ->
                hyperlink.complete(page ?: WebPage.NIL)
            }
        }
    }
    
    class LoadEventHandlers(
        val hyperlink: XSQLScrapeHyperlink,
        val response: ScrapeResponse,
    ) : DefaultLoadEventHandlers() {
        init {
            onWillLoad.addLast {
//                println("onWillLoad")
                response.pageStatusCode = ResourceStatus.SC_PROCESSING
                null
            }
            onWillParseHTMLDocument.addLast { page ->
//                println("onWillParseHTMLDocument")
                page.variables[VAR_IS_SCRAPE] = true
                null
            }
            onWillParseHTMLDocument.addLast { page ->
//                println("onWillParseHTMLDocument")
            }
            onHTMLDocumentParsed.addLast { page, document ->
//                println("onHTMLDocumentParsed")
                require(page.hasVar(VAR_IS_SCRAPE))
                hyperlink.extract(page, document)
            }
            onLoaded.addLast { page ->
//                println("onLoaded")
                hyperlink.complete(page)
            }
        }
    }

    private val logger = getLogger(XSQLScrapeHyperlink::class)

    private val sqlContext get() = session.context as AbstractSQLContext
    private val connectionPool get() = sqlContext.connectionPool
    private val randomConnection get() = sqlContext.randomConnection
    private val isCompleted = AtomicBoolean()

    val response = ScrapeResponse()

    override var args: String? = "-parse ${sql.args}"
    override var event: PageEventHandlers = PageEventHandlersFactory.create(
        loadEventHandlers = LoadEventHandlers(this, response),
        crawlEventHandlers = CrawlEventHandlers(this, response),
    )

    open fun executeQuery(): ResultSet = executeQuery(request, response)

    open fun extract(page: WebPage, document: FeaturedDocument) {
        try {
            response.pageContentBytes = page.contentLength.toInt()
            response.pageStatusCode = page.protocolStatus.minorCode
            
            // logger.info("Extracting {} | {}", page.protocolStatus, page.url)
            
            if (page.protocolStatus.isSuccess) {
                val rs = doExtract(page, document)
                response.resultSet = ResultSetUtils.getEntitiesFromResultSet(rs)
            }
        } catch (t: Throwable) {
            // Log the exception and throw it.
            logger.warn("Unexpected exception", t)
            throw t
        }
    }

    open fun complete(page: WebPage) {
        response.uuid = uuid
        response.isDone = true
        response.finishTime = Instant.now()
        
        if (isCompleted.compareAndSet(false, true)) {
            super.complete(response)
        }
        
        // logger.info("Completed | {}", page.url)
    }

    protected open fun doExtract(page: WebPage, document: FeaturedDocument): ResultSet {
        if (!page.protocolStatus.isSuccess || page.contentLength == 0L || page.content == null) {
            logger.info("No content | {}", page.url)
            response.statusCode = ResourceStatus.SC_NO_CONTENT
            return ResultSets.newSimpleResultSet()
        }

        return executeQuery(request, response)
    }

    protected open fun executeQuery(request: ScrapeRequest, response: ScrapeResponse): ResultSet {
        var rs: ResultSet = ResultSets.newSimpleResultSet()

        try {
            response.statusCode = ResourceStatus.SC_OK
            rs = executeQuery(sql.sql)
        } catch (e: JdbcSQLException) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
            logger.warn("Failed to execute sql #${response.uuid}{}", e.brief())
        } catch (e: Throwable) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
            logger.warn("Failed to execute sql #${response.uuid}\n{}", e.brief())
        }

        return rs
    }

    private fun executeQuery(sql: String): ResultSet {
        val connection = connectionPool.poll() ?: randomConnection
        return executeQuery(sql, connection).also { connectionPool.offer(connection) }
    }

    private fun executeQuery(sql: String, conn: Connection): ResultSet {
        var result: ResultSet? = null
        val millis = measureTimeMillis {
            conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)?.use { st ->
                try {
                    st.executeQuery(sql)?.use { rs ->
                        result = ResultSetUtils.copyResultSet(rs)
                    }
                } catch (e: JdbcSQLException) {
                    val message = e.toString()
                    if (message.contains("Syntax error in SQL statement")) {
                        response.statusCode = ResourceStatus.SC_BAD_REQUEST
                        logger.warn("Syntax error in SQL statement #${response.uuid}>>>\n{}\n<<<", e.sql)
                    } else {
                        response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
                        logger.warn("Failed to execute scrape task #${response.uuid}\n{}", e.stringify())
                    }
                }
            }
        }

        return result ?: ResultSets.newResultSet()
    }
}
