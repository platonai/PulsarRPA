package ai.platon.pulsar.integration.rest

import ai.platon.pulsar.agentic.BasicAgenticSession
import ai.platon.pulsar.boot.autoconfigure.PulsarContextConfiguration
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Import
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PulsarContextConfiguration::class)
class IntegrationTestBase {

    @LocalServerPort
    val serverPort: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var session: PulsarSession

    @Autowired
    lateinit var configuration: ImmutableConfig

    val hostname = "127.0.0.1"

    val baseUri get() = String.format("http://%s:%d", hostname, serverPort)

    @BeforeTest
    fun setup() {
        assertTrue("Session should be BasicAgenticSession, actual ${session.javaClass}") { session is BasicAgenticSession }
    }

    /**
     * Test for Controller [ai.platon.pulsar.rest.api.controller.ScrapeController.execute]
     * */
    fun scrape(url: String): ScrapeResponse? {
        val sql = "select dom_base_uri(dom) as url from load_and_select('$url', ':root')"
        return restTemplate.postForObject("$baseUri/api/x/e", sql, ScrapeResponse::class.java)
    }

    /**
     * Test for Controller [ai.platon.pulsar.rest.api.controller.ScrapeController.submitJob]
     * */
    fun llmScrape(url: String): ScrapeResponse? {
        val sql = "select llm_extract(dom, 'Title, Price, Description') as llm_extracted_fields from load_and_select('$url', 'body')"
        val uuid = restTemplate.postForObject("$baseUri/api/x/s", sql, String::class.java)

        return await(uuid, url)
    }

    private fun await(uuid: String, url: String): ScrapeResponse {
        var tick = 0
        val timeout = 60

        var response: ScrapeResponse = restTemplate.getForObject("$baseUri/api/x/status?uuid=$uuid", ScrapeResponse::class.java)
        while (!response.isDone && ++tick < timeout) {
            sleepSeconds(1)
            response = restTemplate.getForObject("$baseUri/api/x/status?uuid=$uuid", ScrapeResponse::class.java)
        }

        return response
    }

    // 自定义 RestTemplateBuilder 以设置超时时间
    private fun restTemplateBuilder(): RestTemplateBuilder {
        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(10))  // 连接超时时间（毫秒）
            .setResponseTimeout(Timeout.ofMinutes(2))
            .build()

        val httpClient: HttpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build()

        val factory: ClientHttpRequestFactory = HttpComponentsClientHttpRequestFactory(httpClient)

        return RestTemplateBuilder().requestFactory { _: ClientHttpRequestFactorySettings? -> factory }
    }

    // 使用自定义的 RestTemplateBuilder 创建 TestRestTemplate
    @Autowired
    fun setRestTemplate(restTemplateBuilder: RestTemplateBuilder) {
        this.restTemplate = TestRestTemplate(restTemplateBuilder)
    }
}
