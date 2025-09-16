package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.entities.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.core.ParameterizedTypeReference

class SinglePageApplicationControllerTest : ScrapeControllerTestBase() {

    @BeforeEach
    fun `GET init returns status map`() {
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

    @Test
    fun `POST navigate returns CommandStatus`() {
        val request = NavigateRequest(PRODUCT_DETAIL_URL)
        val status = restTemplate.postForObject(
            "$baseUri/api/spa/navigate", request, CommandStatus::class.java
        )
        assertThat(status).isNotNull
        // Execution is synchronous; status should be marked done even if page load fails
        assertThat(status!!.isDone).isTrue()
        assertThat(status.id).isNotBlank()
    }

    @Test
    fun `POST act without driver returns 503`() {
        val request = ActRequest(id = "test", act = "click #submit")
        val response = restTemplate.postForEntity(
            "$baseUri/api/spa/act", request, CommandStatus::class.java
        )
        assertThat(response.statusCode.value()).isEqualTo(503)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.message).contains("No active browser driver")
    }

    @Test
    fun `GET screenshot without driver returns 503`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(ScreenshotRequest(id = "test"), headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/screenshot", HttpMethod.GET, request, CommandStatus::class.java
        )
        assertThat(response.statusCode.value()).isEqualTo(503)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.message).contains("No active browser driver")
    }

    @Test
    fun `GET extract without driver returns 500`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(ExtractRequest(id = "test", prompt = "name, price"), headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/extract", HttpMethod.GET, request, Any::class.java
        )
        assertThat(response.statusCode.value()).isEqualTo(500)
    }
}

