package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.boot.autoconfigure.PulsarContextConfiguration
import ai.platon.pulsar.common.config.ImmutableConfig
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
    lateinit var unmodifiedConfig: ImmutableConfig

    val hostname = "127.0.0.1"

    val baseUri get() = String.format("http://%s:%d", hostname, serverPort)

    @BeforeTest
    fun setup() {
        assertTrue("Session should be BasicPulsarSession, actual ${session.javaClass}") { session is BasicPulsarSession }
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
