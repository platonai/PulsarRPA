package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.rest.api.common.DegenerateXSQLScrapeHyperlink
import ai.platon.pulsar.rest.api.common.ScrapeAPIUtils
import ai.platon.pulsar.rest.api.common.ScrapeHyperlink
import ai.platon.pulsar.rest.api.common.XSQLScrapeHyperlink
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
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
    val session: PulsarSession
) {
    private val logger = LoggerFactory.getLogger(ScrapeService::class.java)
    private val responseCache = ConcurrentSkipListMap<String, ScrapeResponse>()
    private val responseStatusIndex = MultiMapUtils.newListValuedHashMap<Int, String>()

    companion object {
        const val DEFAULT_STATUS_CODE = 0
        const val MAX_CACHE_SIZE = 1000
    }

    fun executeQuery(request: ScrapeRequest): ScrapeResponse {
        var uuid: String? = null
        try {
            val hyperlink = doSubmit(request)

            uuid = hyperlink.uuid
            val response = hyperlink.get(3, TimeUnit.MINUTES)
            return response
        } catch (e: TimeoutException) {
            logger.info("Invalid scrape request. {}", e.message)
            if (uuid == null) {
                return ScrapeResponse(null, ResourceStatus.SC_REQUEST_TIMEOUT, ProtocolStatusCodes.REQUEST_TIMEOUT)
            }

            val response = responseCache[uuid]
            return response?.copy(statusCode = ResourceStatus.SC_REQUEST_TIMEOUT)
                ?: ScrapeResponse(null, ResourceStatus.SC_REQUEST_TIMEOUT, ProtocolStatusCodes.REQUEST_TIMEOUT)
        } catch (e: IllegalArgumentException) {
            logger.info("Invalid scrape request. {}", e.message)
            return ScrapeResponse(null, ResourceStatus.SC_BAD_REQUEST, ProtocolStatusCodes.EXCEPTION)
        } catch (e: Exception) {
            logger.error("Error executing query: >>>${request.sql}<<<", e)
            return ScrapeResponse(null, ResourceStatus.SC_INTERNAL_SERVER_ERROR, ProtocolStatusCodes.EXCEPTION)
        }
    }

    fun submitJob(request: ScrapeRequest): ScrapeHyperlink {
        try {
            return doSubmit(request)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw ScrapeSubmissionException("Failed to submit job | >>>\n$request\n<<<")
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun doSubmit(request: ScrapeRequest): ScrapeHyperlink {
        val hyperlink = createScrapeHyperlink(request)

        responseCache[hyperlink.uuid] = hyperlink.response
        hyperlink.response.uuid = hyperlink.uuid
        require(session is BasicPulsarSession)
        session.submit(hyperlink)
        logger.info("Job submitted successfully: ${hyperlink.uuid}")

        return hyperlink
    }

    fun getStatus(request: ScrapeStatusRequest): ScrapeResponse {
        return responseCache.computeIfAbsent(request.uuid) {
            ScrapeResponse(request.uuid, ResourceStatus.SC_NOT_FOUND, ProtocolStatusCodes.NOT_FOUND)
        }
    }

    fun count(statusCode: Int): Int {
        return when (statusCode) {
            DEFAULT_STATUS_CODE -> responseCache.size
            else -> responseStatusIndex[statusCode]?.size ?: 0
        }
    }

    /**
     * Create a scrape hyperlink from a scrape request.
     *
     * @param request The scrape request
     * @return The scrape hyperlink
     * @throws IllegalArgumentException If the request is invalid
     * */
    @Throws(IllegalArgumentException::class)
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
