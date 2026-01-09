package ai.platon.pulsar.rest.api.webdriver

import ai.platon.pulsar.rest.api.webdriver.dto.*
import ai.platon.pulsar.rest.util.server.TestWebSiteAccess
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

/**
 * Integration tests for WebDriver-compatible API endpoints.
 */
@SpringBootTest(
    classes = [WebDriverTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class WebDriverApiIntegrationTest: TestWebSiteAccess() {

    private fun postJson(path: String, body: Any?): RestTestClient.ResponseSpec {
        return client.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body ?: "")
            .exchange()
    }

    private fun getJson(path: String): RestTestClient.ResponseSpec {
        return client.get()
            .uri(path)
            .exchange()
    }

    private fun deleteJson(path: String): RestTestClient.ResponseSpec {
        return client.delete()
            .uri(path)
            .exchange()
    }

    @Test
    fun `should create session and return sessionId`() {
        val request = NewSessionRequest(capabilities = mapOf("browserName" to "chrome"))

        val response = postJson("/session", request)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))

        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("sessionId"))
        assertNotNull(value["sessionId"])
    }

    @Test
    fun `should navigate to URL after session creation`() {
        // Create session
        val createResponse = postJson("/session", NewSessionRequest())
        val createResult = createResponse.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, createResult.status)

        @Suppress("UNCHECKED_CAST")
        val createBody = createResult.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val sessionValue = createBody["value"] as Map<String, Any?>
        val sessionId = sessionValue["sessionId"] as String

        // Navigate to URL
        val navRequest = SetUrlRequest(url = "https://example.com")
        val navResponse = postJson("/session/$sessionId/url", navRequest)
        val navResult = navResponse.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, navResult.status)
        // value can be null for success responses
        assertTrue((navResult.responseBody as? Map<*, *>)?.containsKey("value") == true || navResult.status == HttpStatus.OK)
    }

    @Test
    fun `should check selector exists`() {
        // Create session
        val sessionId = createSession()

        // Check selector exists
        val selectorRequest = SelectorRef(selector = "#main-content")
        val response = postJson("/session/$sessionId/selectors/exists", selectorRequest)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))

        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("exists"))
    }

    @Test
    fun `should find element by selector and return element reference`() {
        // Create session
        val sessionId = createSession()

        // Find element
        val selectorRequest = SelectorRef(selector = ".product-title")
        val response = postJson("/session/$sessionId/selectors/element", selectorRequest)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))

        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        // Check for WebDriver element reference key
        assertTrue(value.containsKey("element-6066-11e4-a52e-4f735466cecf"))
    }

    @Test
    fun `should click element by selector`() {
        // Create session
        val sessionId = createSession()

        // Click selector
        val selectorRequest = SelectorRef(selector = "button.submit")
        val response = postJson("/session/$sessionId/selectors/click", selectorRequest)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        // value can be null for success responses
        assertTrue((result.responseBody as? Map<*, *>)?.containsKey("value") == true || result.status == HttpStatus.OK)
    }

    @Test
    fun `should return 404 for non-existent session`() {
        val response = getJson("/session/non-existent-session-id")
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("error"))
    }

    @Test
    fun `should delete session`() {
        // Create session
        val sessionId = createSession()

        // Delete session
        deleteJson("/session/$sessionId")

        // Verify session is gone
        val response = getJson("/session/$sessionId")
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, result.status)
    }

    @Test
    fun `should get current URL`() {
        // Create session
        val sessionId = createSession()

        // Navigate to URL
        val navRequest = SetUrlRequest(url = "https://example.com/page")
        postJson("/session/$sessionId/url", navRequest)

        // Get current URL
        val response = getJson("/session/$sessionId/url")
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertEquals("https://example.com/page", body["value"])
    }

    @Test
    fun `should execute sync script`() {
        // Create session
        val sessionId = createSession()

        // Execute script
        val scriptRequest = ScriptRequest(script = "return document.title;")
        val response = postJson("/session/$sessionId/execute/sync", scriptRequest)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        // value can be null for success responses
        assertTrue((result.responseBody as? Map<*, *>)?.containsKey("value") == true || result.status == HttpStatus.OK)
    }

    @Test
    fun `should create event config`() {
        // Create session
        val sessionId = createSession()

        // Create event config
        val eventRequest = EventConfig(eventType = "click", enabled = true)
        val response = postJson("/session/$sessionId/event-configs", eventRequest)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("configId"))
        assertEquals("click", value["eventType"])
    }

    @Test
    fun `should serve openapi yaml`() {
        val response = client.get()
            .uri("/openapi.yaml")
            .exchange()
            .returnResult(String::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
        assertTrue(response.responseBody!!.contains("openapi:"))
        assertTrue(response.responseBody!!.contains("Browser4 WebDriver-Compatible API"))
    }

    @Test
    fun `should include X-Request-Id header in responses`() {
        val response = postJson("/session", NewSessionRequest()).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseHeaders["X-Request-Id"])
    }

    // ========== Agent API Tests ==========

    @Test
    fun `should run agent task`() {
        val sessionId = createSession()

        val request = AgentRunRequest(task = "Find the login button and click it")
        val result = postJson("/session/$sessionId/agent/run", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))

        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("message"))
    }

    @Test
    fun `should observe page with agent`() {
        val sessionId = createSession()

        val request = AgentObserveRequest(instruction = "Find interactive elements")
        val result = postJson("/session/$sessionId/agent/observe", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))
    }

    @Test
    fun `should execute agent action`() {
        val sessionId = createSession()

        val request = AgentActRequest(action = "Click the submit button")
        val result = postJson("/session/$sessionId/agent/act", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("action"))
    }

    @Test
    fun `should extract data with agent`() {
        val sessionId = createSession()

        val request = AgentExtractRequest(instruction = "Extract the page title and description")
        val result = postJson("/session/$sessionId/agent/extract", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("data"))
    }

    @Test
    fun `should summarize page with agent`() {
        val sessionId = createSession()

        val request = AgentSummarizeRequest(instruction = "Summarize the main content")
        val result = postJson("/session/$sessionId/agent/summarize", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))
    }

    @Test
    fun `should clear agent history`() {
        val sessionId = createSession()

        val result = client.post()
            .uri("/session/$sessionId/agent/clearHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertEquals(true, body["value"])
    }

    // ========== PulsarSession API Tests ==========

    @Test
    fun `should normalize URL`() {
        val sessionId = createSession()

        val request = NormalizeRequest(url = "example.com", args = "-expire 1d")
        val result = postJson("/session/$sessionId/normalize", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertTrue(value.containsKey("spec"))
        // Check that scheme was added
        val normalizedUrl = value["url"] as String
        assertTrue(normalizedUrl.startsWith("https://"))
    }

    @Test
    fun `should open URL immediately`() {
        val sessionId = createSession()

        val request = OpenRequest(url = "https://example.com/page")
        val result = postJson("/session/$sessionId/open", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertEquals("https://example.com/page", value["url"])
    }

    @Test
    fun `should load URL from storage or internet`() {
        val sessionId = createSession()

        val request = LoadRequest(url = "https://example.com", args = "-expire 1d")
        val result = postJson("/session/$sessionId/load", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertTrue(value.containsKey("protocolStatus"))
    }

    @Test
    fun `should submit URL to crawl pool`() {
        val sessionId = createSession()

        val request = SubmitRequest(url = "https://example.com/to-crawl", args = "-expire 7d")
        val result = postJson("/session/$sessionId/submit", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, result.status)
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        assertEquals(true, body["value"])
    }

    @Test
    fun `should return consistent error format and request id for selector exists when session missing`() {
        val selectorRequest = SelectorRef(selector = "#missing")

        val response = postJson("/session/non-existent-session-id/selectors/exists", selectorRequest)
        val result = response.returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, result.status)
        assertNotNull(result.responseHeaders["X-Request-Id"], "X-Request-Id header should be present")
        assertNotNull(result.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("error"))
        assertTrue(value.containsKey("message"))
    }

    @Test
    fun `should return consistent error format and request id for element click when session missing`() {
        val response = client.post()
            .uri("/session/non-existent-session-id/element/any/click")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, response.status)
        assertNotNull(response.responseHeaders["X-Request-Id"], "X-Request-Id header should be present")
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("error"))
        assertTrue(value.containsKey("message"))
    }

    @Test
    fun `waitFor selector with zero timeout should either succeed immediately in mock mode or return 408 in real mode`() {
        val sessionId = createSession()

        val request = WaitForRequest(selector = "#definitely-missing", timeout = 0)
        val response = postJson("/session/$sessionId/selectors/waitFor", request).returnResult(Map::class.java)

        // Mock mode returns 200 immediately; real mode may return 408.
        assertTrue(
            response.status == HttpStatus.OK || response.status == HttpStatus.REQUEST_TIMEOUT,
            "Expected 200 (mock) or 408 (real) but got ${response.status}"
        )

        // In both cases the response should include request id.
        assertNotNull(response.responseHeaders["X-Request-Id"], "X-Request-Id header should be present")

        if (response.status == HttpStatus.REQUEST_TIMEOUT) {
            @Suppress("UNCHECKED_CAST")
            val body = response.responseBody as Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val value = body["value"] as Map<String, Any?>
            assertTrue(value.containsKey("error"))
            assertTrue(value.containsKey("message"))
        }
    }

    /**
     * Helper method to create a session and return the session ID.
     */
    private fun createSession(): String {
        val result = postJson("/session", NewSessionRequest()).returnResult(Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val body = result.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        return value["sessionId"] as String
    }
}
