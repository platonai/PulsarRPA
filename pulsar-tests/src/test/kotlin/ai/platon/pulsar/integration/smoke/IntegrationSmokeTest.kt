package ai.platon.pulsar.integration.rest.smoke

import ai.platon.pulsar.common.printlnPro
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationSmokeTest {
    @LocalServerPort
    val port = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    val jsonUrl get() = "http://localhost:$port/json"

    @Test
    fun `when access json API then ok`() {
        val response = restTemplate.getForObject(jsonUrl, String::class.java)
        printlnPro(response)
    }
}
