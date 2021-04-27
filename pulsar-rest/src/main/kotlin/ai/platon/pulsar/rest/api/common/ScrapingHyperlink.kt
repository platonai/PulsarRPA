package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.PulsarParams.VAR_IS_SCRAPE
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.crawl.DefaultLoadEventHandler
import ai.platon.pulsar.crawl.LoadEventPipelineHandler
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.AbstractSQLContext
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.h2.jdbc.JdbcSQLException
import java.sql.Connection
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class ScrapingLoadEventHandler(
    val hyperlink: ScrapingHyperlink,
    val response: ScrapeResponse,
) : DefaultLoadEventHandler() {
    init {
        onBeforeLoadPipeline.addLast {
            response.pageStatusCode = ResourceStatus.SC_PROCESSING
        }
        onBeforeParsePipeline.addLast { page ->
            require(page.loadEventHandler === this)
            require(page.isCachedContentEnabled)
            page.variables[VAR_IS_SCRAPE] = true
        }
        onBeforeHtmlParsePipeline.addLast { page ->
        }
        onAfterHtmlParsePipeline.addLast { page, document ->
            require(page.loadEventHandler === this)
            require(page.hasVar(VAR_IS_SCRAPE))
            hyperlink.extract(page, document)
        }
        onAfterParsePipeline.addLast { page ->
            require(page.loadEventHandler === this)
            hyperlink.commit(page)
        }
    }
}

open class ScrapingHyperlink(
    val request: ScrapeRequest,
    val scrapeSQL: ScrapingSQL,
    val session: PulsarSession,
    val globalCache: GlobalCache,
    val uuid: String = UUID.randomUUID().toString()
) : ListenableHyperlink(scrapeSQL.url), Future<ScrapeResponse> {

    private val logger = getLogger(ScrapingHyperlink::class)

    private val sqlContext get() = session.context as AbstractSQLContext
    private val connectionPool get() = sqlContext.connectionPool
    private val randomConnection get() = sqlContext.randomConnection

    val response = ScrapeResponse(uuid)
    protected val isCancelled = AtomicBoolean()
    protected val isDone = CountDownLatch(1)

    override var args: String? = "${scrapeSQL.args} -parse"
    override var loadEventHandler: LoadEventPipelineHandler = ScrapingLoadEventHandler(this, response)

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        isCancelled.set(true)
        isDone.countDown()
        return true
    }

    override fun isCancelled(): Boolean {
        return isCancelled.get()
    }

    override fun isDone(): Boolean {
        return isDone.count == 0L
    }

    override fun get(): ScrapeResponse {
        isDone.await()
        return response
    }

    override fun get(timeout: Long, unit: TimeUnit): ScrapeResponse {
        isDone.await(timeout, unit)
        return response
    }

    open fun executeQuery(): ResultSet = executeQuery(request, response)

    open fun extract(page: WebPage, document: FeaturedDocument) {
        try {
            extract0(page, document)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    open fun commit(page: WebPage) {
        if (isCancelled.get()) {
            isDone.countDown()
            return
        }

        try {
            response.pageContentBytes = page.contentLength.toInt()
            response.pageStatusCode = page.protocolStatus.minorCode
        } finally {
            isDone.countDown()
        }
    }

    protected open fun extract0(page: WebPage, document: FeaturedDocument): ResultSet {
        if (!page.protocolStatus.isSuccess || page.contentLength == 0L || page.content == null) {
            response.statusCode = ResourceStatus.SC_NO_CONTENT
            return ResultSets.newSimpleResultSet()
        }

        globalCache.putPDCache(page, document)
        val rs = executeQuery(request, response)
        globalCache.removePDCache(page.url)
        return rs
    }

    protected open fun executeQuery(request: ScrapeRequest, response: ScrapeResponse): ResultSet {
        var rs: ResultSet = ResultSets.newSimpleResultSet()

        try {
            val sql = APISQLUtils.sanitize(request.sql)

            response.statusCode = ResourceStatus.SC_PROCESSING

            rs = executeQuery(sql)

            val resultSet = mutableListOf<Map<String, Any?>>()
            ResultSetUtils.getEntitiesFromResultSetTo(rs, resultSet)
            response.resultSet = resultSet

            response.statusCode = ResourceStatus.SC_OK
        } catch (e: JdbcSQLException) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
            logger.warn("Failed to execute sql #${response.uuid}{}", Strings.simplifyException(e))
        } catch (e: Throwable) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
            logger.warn("Failed to execute sql #${response.uuid}\n{}", Strings.stringifyException(e))
        }

        return rs
    }

    private fun executeQuery(sql: String): ResultSet {
//        return session.executeQuery(sql)
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
                        logger.warn("Failed to execute scrape task #${response.uuid}\n{}",
                            Strings.stringifyException(e))
                    }
                }
            }
        }

        return result ?: ResultSets.newResultSet()
    }
}
