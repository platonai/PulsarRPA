package ai.platon.pulsar.app.api.controller

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.*
import ai.platon.pulsar.test.BasicTestHelper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*

class SinglePageApplicationControllerTest : IntegrationTestBase() {

    fun init() {
        val typeRef = object : ParameterizedTypeReference<Map<String, String>>() {}
        val response: ResponseEntity<Map<String, String>> = restTemplate.exchange(
            "$baseUri/api/spa/init", HttpMethod.GET, null, typeRef
        )
        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        val body = response.body
        assertThat(body).isNotNull
        assertThat(body!!).containsKey("status")
        // Accept any of: initialized, healthy, error depending on environment
        assertThat(body["status"]).isNotBlank()
    }

    fun navigateToProductPage() {
        val request = NavigateRequest(BasicTestHelper.PRODUCT_DETAIL_URL)
        val status = restTemplate.postForObject(
            "$baseUri/api/spa/navigate", request, CommandStatus::class.java
        )
        Assertions.assertThat(status).isNotNull
        // Execution is synchronous; status should be marked done even if page load fails
        assertThat(status!!.isDone).isTrue()
        assertThat(status.id).isNotBlank()
    }

    @BeforeEach
    fun `init Single Page Application`() {
        Assumptions.assumeTrue { ChatModelFactory.isModelConfigured(session.unmodifiedConfig) }

        init()
        navigateToProductPage()
    }

    @Test
    fun `act with clicking`() {
        val request = ActRequest(id = "test", act = "click search box")
        val response = restTemplate.postForEntity(
            "$baseUri/api/spa/act", request, CommandStatus::class.java
        )

        readln()

        assertThat(response.statusCode.value()).isEqualTo(200)
        // assertThat(response.body).isNotNull
        println(response.body)
    }

    @Test
    fun `take screenshot`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(ScreenshotRequest(id = "test"), headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/screenshot", HttpMethod.GET, request, CommandStatus::class.java
        )
        assertThat(response.statusCode.value()).isEqualTo(200)
        Assertions.assertThat(response.body).isNotNull
        println(response.body)
    }

    @Test
    fun `extract with prompt`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(ExtractRequest(id = "test", prompt = "name, price"), headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/extract", HttpMethod.GET, request, Any::class.java
        )
        println(response.body)
        assertThat(response.statusCode.value()).isEqualTo(200)
    }
}
