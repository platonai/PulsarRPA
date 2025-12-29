package ai.platon.pulsar.app.api.controller

import ai.platon.pulsar.agentic.ActionOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.NavigateRequest
import ai.platon.pulsar.test.TestResourceUtil.Companion.PRODUCT_DETAIL_URL
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import kotlin.test.Ignore

@Ignore("Disabled temporarily, Run the tests manually")
@Tag("MustManualRun")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SinglePageApplicationControllerTest : IntegrationTestBase() {

    fun health() {
        val typeRef = object : ParameterizedTypeReference<Map<String, String>>() {}
        val response: ResponseEntity<Map<String, String>> = restTemplate.exchange(
            "$baseUri/api", HttpMethod.GET, null, typeRef
        )
        // If not initialized yet, health may be 500 (unhealthy). Don't hard fail at this stage.
        assertThat(response.body).isNotNull
        assertThat(response.body!!).containsKey("status")
    }

    fun init() {
        val typeRef = object : ParameterizedTypeReference<Map<String, String>>() {}
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(mapOf("api_key" to ""), headers)

        val response: ResponseEntity<Map<String, String>> = restTemplate.exchange(
            "$baseUri/api/init", HttpMethod.POST, request, typeRef
        )
        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        val body = response.body
        assertThat(body).isNotNull
        assertThat(body!!["status"]).isEqualTo("healthy")
    }

    @BeforeEach
    fun `init Single Page Application`() {
        Assumptions.assumeTrue { ChatModelFactory.isModelConfigured(session.configuration) }

        health()
        init()
    }

    @Order(10)
    @Test
    fun `navigate to product page`() {
        val request = NavigateRequest(PRODUCT_DETAIL_URL)
        val response = restTemplate.postForEntity(
            "$baseUri/api/navigate", request, SinglePageApplicationController.BrowserActionResult::class.java
        )
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body
        assertThat(body).isNotNull
        assertThat(body!!.success).isTrue()
        assertThat(body.url).isNotBlank()
        assertThat(body.screenshot_base64).isNotNull
    }

    @Order(20)
    @Test
    fun `act with searching`() {
        val request = ActionOptions(action = "search latest iphone, give me a summary of the search result")

        var lastStatus: Int? = null
        var lastBody: SinglePageApplicationController.BrowserActionResult? = null
        var lastError: Throwable? = null

        val maxAttempts = 3
        val delayMillis = 1500L
        repeat(maxAttempts) { attempt ->
            try {
                val response = restTemplate.postForEntity(
                    "$baseUri/api/act", request, SinglePageApplicationController.BrowserActionResult::class.java
                )
                lastStatus = response.statusCode.value()
                lastBody = response.body
                if (response.statusCode.is2xxSuccessful && response.body != null) {
                    val result = response.body!!
                    printlnPro("Attempt ${attempt + 1}: action='${result.action}' message='${result.message}'")
                    assertThat(result.message).isNotBlank
                    return
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
        val request = HttpEntity(mapOf<String, Any?>(), headers)

        val response = restTemplate.exchange(
            "$baseUri/api/screenshot", HttpMethod.POST, request, SinglePageApplicationController.BrowserActionResult::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body
        assertThat(body).isNotNull
        assertThat(body!!.screenshot_base64).isNotNull
        assertThat(body.screenshot_base64!!).isNotBlank()

        printlnPro("Screenshot captured successfully (base64 length): ${body.screenshot_base64!!.length}")

        // Optional: decode and persist for manual inspection
        val exportPath = AppPaths.getRandomProcTmpTmpPath("screenshot-", ".jpg")
        val bytes = java.util.Base64.getDecoder().decode(body.screenshot_base64)
        java.nio.file.Files.write(exportPath, bytes, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)
        printlnPro("Screenshot saved to: $exportPath")

        assertThat(java.nio.file.Files.exists(exportPath)).isTrue()
        assertThat(java.nio.file.Files.size(exportPath)).isGreaterThan(0)
    }

    @Order(40)
    @Test
    fun `extract with prompt`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf(
            "instruction" to "name, price",
            "iframes" to true,
        )
        val request = HttpEntity(requestBody, headers)

        val response = restTemplate.exchange(
            "$baseUri/api/extract", HttpMethod.POST, request, SinglePageApplicationController.BrowserActionResult::class.java
        )
        printlnPro(response.body)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.message).isNotBlank
    }
}
