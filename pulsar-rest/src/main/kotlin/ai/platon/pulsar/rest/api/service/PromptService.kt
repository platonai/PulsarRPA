package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.rest.api.common.DegenerateXSQLScrapeHyperlink
import ai.platon.pulsar.rest.api.common.ScrapeAPIUtils
import ai.platon.pulsar.rest.api.common.ScrapeHyperlink
import ai.platon.pulsar.rest.api.common.XSQLScrapeHyperlink
import ai.platon.pulsar.rest.api.entities.PromptRequest
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
class PromptService(
    val session: PulsarSession,
    val globalCacheFactory: GlobalCacheFactory,
) {
    private val logger = LoggerFactory.getLogger(PromptService::class.java)
    /**
     * The response cache, the key is the uuid, the value is the response
     * TODO: use Ehcache instead
     * */
    private val responseCache = ConcurrentSkipListMap<String, ScrapeResponse>()
    /**
     * The response status map, the key is the status code, the value is the response's uuid
     * */
    private val responseStatusIndex = MultiMapUtils.newListValuedHashMap<Int, String>()

    fun chat(request: PromptRequest): String {
        val page = session.load(request.url, "-refresh")
        val document = session.parse(page)
        return if (page.protocolStatus.isSuccess) {
            session.chat(request.prompt, document.text).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }

    fun extract(request: PromptRequest): String {
        val page = session.load(request.url, "-refresh")
        val document = session.parse(page)

        val prompt = """
            Extract the following information from the web page:
            ${request.prompt}
            """.trimIndent() + "\n\n" + document.text
        return if (page.protocolStatus.isSuccess) {
            session.chat(prompt).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }
}
