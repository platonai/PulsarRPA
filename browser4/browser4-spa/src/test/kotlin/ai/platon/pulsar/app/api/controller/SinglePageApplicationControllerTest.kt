package ai.platon.pulsar.app.api.controller

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.NavigateRequest
import ai.platon.pulsar.rest.api.entities.ScreenshotRequest
import ai.platon.pulsar.agentic.ActResult
import ai.platon.pulsar.agentic.ActionOptions
import ai.platon.pulsar.agentic.ExtractOptions
import ai.platon.pulsar.agentic.ExtractResult
import ai.platon.pulsar.agentic.ExtractionSchema
import ai.platon.pulsar.test.TestResourceUtil.Companion.PRODUCT_DETAIL_URL
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.Ignore
import org.springframework.web.context.request.async.AsyncRequestTimeoutException

@Ignore("Disabled temporarily, Run the tests manually")
@Tag("MustManualRun")
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
    fun `act with searching`() {
        val request = ActionOptions(action = "search latest iphone, give me a summary of the search result")

        var lastStatus: Int? = null
        var lastBody: ActResult? = null
        var lastError: Throwable? = null

        val maxAttempts = 3
        val delayMillis = 1500L
        repeat(maxAttempts) { attempt ->
            try {
                val response = restTemplate.postForEntity(
                    "$baseUri/api/spa/act", request, ActResult::class.java
                )
                lastStatus = response.statusCode.value()
                lastBody = response.body
                if (response.statusCode.is2xxSuccessful && response.body != null) {
                    val result = response.body!!
                    printlnPro("Attempt ${attempt + 1}: action='${result.action}' message='${result.message}'")
                    printlnPro(result.result?.modelResponse)
                    printlnPro(result)
                    assertThat(result.action).isNotBlank
                    assertThat(result.message).isNotBlank
                    return // success path
                } else {
                    printlnPro("Attempt ${attempt + 1}: Non-success status=${response.statusCode} body=${response.body}")
                }
            } catch (ex: AsyncRequestTimeoutException) {
                lastError = ex
                printlnPro("Attempt ${attempt + 1}: AsyncRequestTimeoutException: ${ex.message}")
            } catch (ex: Exception) {
                lastError = ex
                printlnPro("Attempt ${attempt + 1}: Exception: ${ex::class.simpleName} - ${ex.message}")
            }
            if (attempt < maxAttempts - 1) {
                Thread.sleep(delayMillis)
            }
        }

        Assertions.fail<Any>("Failed to perform SPA act after $maxAttempts attempts. lastStatus=$lastStatus lastBody=$lastBody lastError=${lastError?.message}")
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
        val schema = mapOf("name" to "string - product name", "price" to "double - product price")
        val options = ExtractOptions(instruction = "name, price", ExtractionSchema.fromMap(schema))
        val request = HttpEntity(options, headers)
        val response = restTemplate.exchange(
            "$baseUri/api/spa/extract", HttpMethod.GET, request, ExtractResult::class.java
        )
        printlnPro(response.body)
        assertThat(response.statusCode.value()).isEqualTo(200)
    }
}
