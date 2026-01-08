package ai.platon.pulsar.integration.rest

import ai.platon.pulsar.agentic.BasicAgenticSession
import ai.platon.pulsar.boot.autoconfigure.PulsarContextConfiguration
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PulsarContextConfiguration::class)
class IntegrationTestBase {

    @LocalServerPort
    val serverPort: Int = 0

    @Autowired
    lateinit var session: PulsarSession

    @Autowired
    lateinit var configuration: ImmutableConfig

    val hostname = "127.0.0.1"

    val baseUri get() = String.format("http://%s:%d", hostname, serverPort)

    // Build a RestTestClient bound to the running server on demand
    private val rest get() = RestTestClient.bindToServer().baseUrl(baseUri).build()

    @BeforeTest
    fun setup() {
        assertTrue("Session should be BasicAgenticSession, actual ${session.javaClass}") { session is BasicAgenticSession }
    }

    /**
     * Test for Controller [ai.platon.pulsar.rest.api.controller.ScrapeController.execute]
     * */
    fun scrape(url: String): ScrapeResponse? {
        val sql = "select dom_base_uri(dom) as url from load_and_select('$url', ':root')"
        return rest.post().uri("/api/x/e")
            .body(sql)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(ScrapeResponse::class.java)
            .returnResult()
            .responseBody
    }

    /**
     * Test for Controller [ai.platon.pulsar.rest.api.controller.ScrapeController.submitJob]
     * */
    fun llmScrape(url: String): ScrapeResponse? {
        val sql = "select llm_extract(dom, 'Title, Price, Description') as llm_extracted_fields from load_and_select('$url', 'body')"
        val uuid = rest.post().uri("/api/x/s")
            .body(sql)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(String::class.java)
            .returnResult()
            .responseBody

        return if (uuid != null) await(uuid, url) else null
    }

    private fun await(uuid: String, url: String): ScrapeResponse {
        var tick = 0
        val timeout = 60

        var response: ScrapeResponse = rest.get().uri("/api/x/status?uuid=$uuid")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody(ScrapeResponse::class.java)
            .returnResult()
            .responseBody!!

        while (!response.isDone && ++tick < timeout) {
            sleepSeconds(1)
            response = rest.get().uri("/api/x/status?uuid=$uuid")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(ScrapeResponse::class.java)
                .returnResult()
                .responseBody!!
        }

        return response
    }

}
