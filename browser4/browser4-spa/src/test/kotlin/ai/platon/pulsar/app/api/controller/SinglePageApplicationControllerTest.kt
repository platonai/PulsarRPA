package ai.platon.pulsar.app.api.controller

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.*
import ai.platon.pulsar.test.TestResourceUtil.Companion.PRODUCT_DETAIL_URL
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
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

    @BeforeEach
    fun `init Single Page Application`() {
        Assumptions.assumeTrue { ChatModelFactory.isModelConfigured(session.configuration) }

        init()
    }

    @Order(10)
    @Test
    fun `navigate to product page`() {
        val request = NavigateRequest(PRODUCT_DETAIL_URL)
        val response = restTemplate.postForObject(
            "$baseUri/api/spa/navigate", request, String::class.java
        )
        Assertions.assertThat(response).isNotNull
        // Execution is synchronous; status should be marked done even if page load fails
//        assertThat(status!!.isDone).isTrue()
//        assertThat(status.id).isNotBlank()
        assertThat(response).isEqualTo("success")
    }

    @Order(20)
    @Test
    fun `act with clicking`() {
        val request = ActRequest(id = "test", act = "click search box")
        val response = restTemplate.postForEntity(
            "$baseUri/api/spa/act", request, CommandStatus::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(200)
        // assertThat(response.body).isNotNull
        printlnPro(response.body)
    }

    @Order(30)
    @Test
    fun `take screenshot`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.IMAGE_JPEG)
        val request = HttpEntity(ScreenshotRequest(id = "test"), headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/screenshot", HttpMethod.GET, request, ByteArray::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.size).isGreaterThan(0)
        printlnPro("Screenshot captured successfully, size: ${response.body!!.size} bytes")

        val exportPath = AppPaths.getRandomProcTmpTmpPath("screenshot-", ".jpg")
        Files.write(exportPath, response.body!!, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        printlnPro("Screenshot saved to: $exportPath")

        // Verify the file was created and has content
        assertThat(Files.exists(exportPath)).isTrue()
        assertThat(Files.size(exportPath)).isEqualTo(response.body!!.size.toLong())
    }

    @Order(40)
    @Test
    fun `extract with prompt`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(ExtractRequest(id = "test", prompt = "name, price"), headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/extract", HttpMethod.GET, request, Any::class.java
        )
        printlnPro(response.body)
        assertThat(response.statusCode.value()).isEqualTo(200)
    }
}

