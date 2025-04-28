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
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.apache.commons.collections4.MultiMapUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class ScrapeService(
    val session: PulsarSession,
    val globalCacheFactory: GlobalCacheFactory,
) {
    private val logger = LoggerFactory.getLogger(ScrapeService::class.java)
    /**
     * The response cache, the key is the uuid, the value is the response
     * */
    private val responseCache = ConcurrentSkipListMap<String, ScrapeResponse>()
    /**
     * The response status map, the key is the status code, the value is the response's uuid
     * */
    private val responseStatusIndex = MultiMapUtils.newListValuedHashMap<Int, String>()

    /**
     * Execute a scrape task and wait until the execution is done,
     * for test purpose only, no customer should access this api
     * */
    fun executeQuery(request: ScrapeRequest): ScrapeResponse {
        try {
            val hyperlink = createScrapeHyperlink(request)
            session.submit(hyperlink)
            val response = hyperlink.get(120, TimeUnit.SECONDS)
            val rs = response.resultSet
            return response
        } catch (e: TimeoutException) {
            logger.warn("Error executing query: >>>${request.sql}<<<", e)
            return ScrapeResponse("", ResourceStatus.SC_INTERNAL_SERVER_ERROR, ProtocolStatusCodes.EXCEPTION)
        }
    }

    /**
     * Submit a scraping task
     * */
    fun submitJob(request: ScrapeRequest): String {
        val hyperlink = createScrapeHyperlink(request)
        responseCache[hyperlink.uuid] = hyperlink.response
        hyperlink.response.uuid = hyperlink.uuid
        require(session is BasicPulsarSession)
        session.submit(hyperlink)
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

    /**
     * Get the response count by status code
     * */
    fun count(statusCode: Int): Int {
        return when (statusCode) {
            0 -> responseCache.size
            else -> responseStatusIndex[statusCode]?.size ?: 0
        }
    }

    private fun createScrapeHyperlink(request: ScrapeRequest): ScrapeHyperlink {
        val sql = request.sql
        val link = if (ScrapeAPIUtils.isScrapeUDF(sql)) {
            val xSQL = ScrapeAPIUtils.normalize(sql)
            XSQLScrapeHyperlink(request, xSQL, session)
        } else {
            DegenerateXSQLScrapeHyperlink(request, session)
        }

        link.eventHandlers.crawlEventHandlers.onLoaded.addLast { url, page ->
            responseCache[link.uuid] = link.response
            responseStatusIndex[link.response.statusCode].add(link.uuid)
            null
        }

        return link
    }
}
