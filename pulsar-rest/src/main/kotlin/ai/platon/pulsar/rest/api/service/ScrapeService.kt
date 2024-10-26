package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.rest.api.common.DegenerateXSQLScrapeHyperlink
import ai.platon.pulsar.rest.api.common.ScrapeAPIUtils
import ai.platon.pulsar.rest.api.common.ScrapeHyperlink
import ai.platon.pulsar.rest.api.common.XSQLScrapeHyperlink
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.TimeUnit

@Service
class ScrapeService(
    val session: PulsarSession,
    val globalCacheFactory: GlobalCacheFactory,
) {
    private val logger = LoggerFactory.getLogger(ScrapeService::class.java)
    private val responseCache = ConcurrentSkipListMap<String, ScrapeResponse>()
    private val urlPool get() = globalCacheFactory.globalCache.urlPool

    /**
     * Execute a scrape task and wait until the execution is done,
     * for test purpose only, no customer should access this api
     * */
    fun executeQuery(request: ScrapeRequest): ScrapeResponse {
        try {
            val hyperlink = createScrapeHyperlink(request)
            urlPool.higher3Cache.reentrantQueue.add(hyperlink)
            return hyperlink.get(2, TimeUnit.MINUTES)
        } catch (e: Throwable) {
            logger.error("Error executing query: >>>${request.sql}<<<", e)
            return ScrapeResponse("", ResourceStatus.SC_INTERNAL_SERVER_ERROR, ProtocolStatusCodes.EXCEPTION)
        }
    }

    /**
     * Submit a scraping task
     * */
    fun submitJob(request: ScrapeRequest): String {
        val hyperlink = createScrapeHyperlink(request)
        responseCache[hyperlink.uuid] = hyperlink.response
        urlPool.normalCache.reentrantQueue.add(hyperlink)
        return hyperlink.uuid
    }

    /**
     * Get the response
     * */
    fun getStatus(request: ScrapeStatusRequest): ScrapeResponse {
        return responseCache.computeIfAbsent(request.uuid) {
            ScrapeResponse(request.uuid, ResourceStatus.SC_NOT_FOUND, ProtocolStatusCodes.NOT_FOUND)
        }
    }

    private fun createScrapeHyperlink(request: ScrapeRequest): ScrapeHyperlink {
        val sql = request.sql
        return if (ScrapeAPIUtils.isScrapeUDF(sql)) {
            val xSQL = ScrapeAPIUtils.normalize(sql)
            XSQLScrapeHyperlink(request, xSQL, session)
        } else {
            DegenerateXSQLScrapeHyperlink(request, session)
        }
    }
}
