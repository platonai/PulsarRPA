package ai.platon.pulsar.rest.api.webdriver

import ai.platon.pulsar.rest.api.webdriver.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

/**
 * Comprehensive integration tests for all WebDriver API endpoints.
 * Tests all controllers: Session, Navigation, Selector, Element, Script, Control, Events, Agent, PulsarSession, Health.
 */
@SpringBootTest(
    classes = [WebDriverTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComprehensiveApiIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private val baseUrl: String
        get() = "http://localhost:$port"

    private fun jsonHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
    }

    // ==================== Health Endpoints ====================

    @Test
    fun `should return health status with mode and active sessions`() {
        val response = restTemplate.getForEntity(
            "$baseUrl/health",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val body = response.body!!
        assertEquals("UP", body["status"])
        assertTrue(body.containsKey("mode"))
        assertTrue(body["mode"] in listOf("mock", "real"))
        assertTrue(body.containsKey("activeSessions"))
    }

    @Test
    fun `should return readiness status`() {
        val response = restTemplate.getForEntity(
            "$baseUrl/health/ready",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val body = response.body!!
        assertEquals(true, body["ready"])
        assertTrue(body.containsKey("mode"))
    }

    @Test
    fun `should return liveness status`() {
        val response = restTemplate.getForEntity(
            "$baseUrl/health/live",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val body = response.body!!
        assertEquals(true, body["live"])
    }

    // ==================== Session Management ====================

    @Test
    fun `should create session with custom capabilities`() {
        val request = NewSessionRequest(
            capabilities = mapOf(
                "browserName" to "chrome",
                "browserVersion" to "latest",
                "platformName" to "linux"
            )
        )
        val entity = HttpEntity(request, jsonHeaders())

        val response = restTemplate.postForEntity(
            "$baseUrl/session",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("sessionId"))
        assertTrue(value.containsKey("capabilities"))
    }

    @Test
    fun `should get session details`() {
        val sessionId = createSession()

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("sessionId"))
        assertEquals(sessionId, value["sessionId"])
    }

    // ==================== Navigation Endpoints ====================

    @Test
    fun `should navigate and retrieve current URL`() {
        val sessionId = createSession()

        // Navigate
        val navRequest = SetUrlRequest(url = "https://example.com/test-page")
        val navEntity = HttpEntity(navRequest, jsonHeaders())
        val navResponse = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/url",
            navEntity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, navResponse.statusCode)

        // Get current URL
        val urlResponse = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/url",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, urlResponse.statusCode)
        assertEquals("https://example.com/test-page", urlResponse.body!!["value"])
    }

    @Test
    fun `should get document URI`() {
        val sessionId = createSession()

        // Navigate first
        navigateToUrl(sessionId, "https://example.com/page")

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/documentUri",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        val documentUri = response.body!!["value"] as String
        assertTrue(documentUri.isNotBlank())
    }

    @Test
    fun `should get base URI`() {
        val sessionId = createSession()

        // Navigate first
        navigateToUrl(sessionId, "https://example.com/path/page")

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/baseUri",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        val baseUri = response.body!!["value"] as String
        assertTrue(baseUri.startsWith("https://"))
    }

    // ==================== Selector Operations ====================

    @Test
    fun `should find multiple elements by selector`() {
        val sessionId = createSession()

        val request = SelectorRef(selector = ".product-item")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/elements",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as List<Map<String, Any?>>
        assertTrue(value.isNotEmpty())
        // Check that each element has a WebDriver element reference
        value.forEach { elem ->
            assertTrue(elem.containsKey("element-6066-11e4-a52e-4f735466cecf"))
        }
    }

    @Test
    fun `should wait for selector to appear`() {
        val sessionId = createSession()

        val request = WaitForRequest(selector = "#dynamic-element", timeout = 1000)
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/waitFor",
            entity,
            Map::class.java
        )

        // Should succeed in mock mode or timeout in real mode
        assertTrue(
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.REQUEST_TIMEOUT
        )
    }

    @Test
    fun `should fill input field by selector`() {
        val sessionId = createSession()

        val request = FillRequest(selector = "input[name='username']", value = "testuser")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/fill",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should press key by selector`() {
        val sessionId = createSession()

        val request = PressRequest(selector = "input[name='search']", key = "Enter")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/press",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should get outer HTML by selector`() {
        val sessionId = createSession()

        val request = SelectorRef(selector = "#main-content")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/outerHtml",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Response is HtmlResponse(value = String)
        assertTrue(response.body!!.containsKey("value"))
        val html = response.body!!["value"] as String?
        assertNotNull(html)
        assertTrue(html!!.isNotBlank())
    }

    @Test
    fun `should take screenshot of element by selector`() {
        val sessionId = createSession()

        val request = SelectorRef(selector = "#header")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/screenshot",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Response is ScreenshotResponse(value = String)
        assertTrue(response.body!!.containsKey("value"))
        val screenshot = response.body!!["value"] as String?
        assertNotNull(screenshot)
        assertTrue(screenshot!!.isNotBlank())
    }

    // ==================== Element Operations by ID ====================

    @Test
    fun `should find element using WebDriver locator`() {
        val sessionId = createSession()

        val request = FindElementRequest(using = "css selector", value = ".login-button")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/element",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("element-6066-11e4-a52e-4f735466cecf"))
    }

    @Test
    fun `should find multiple elements using WebDriver locator`() {
        val sessionId = createSession()

        val request = FindElementRequest(using = "css selector", value = ".item")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/elements",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as List<Map<String, Any?>>
        assertTrue(value.isNotEmpty())
    }

    @Test
    fun `should click element by ID`() {
        val sessionId = createSession()
        val elementId = getElementId(sessionId, "button.submit")

        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/element/$elementId/click",
            HttpEntity<String>(jsonHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `should send keys to element by ID`() {
        val sessionId = createSession()
        val elementId = getElementId(sessionId, "input[name='email']")

        val request = SendKeysRequest(text = "test@example.com")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/element/$elementId/value",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `should get element attribute`() {
        val sessionId = createSession()
        val elementId = getElementId(sessionId, "a.link")

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/element/$elementId/attribute/href",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        // In mock mode, may return null or a mock value
        assertTrue(response.body!!.containsKey("value"))
    }

    @Test
    fun `should get element text`() {
        val sessionId = createSession()
        val elementId = getElementId(sessionId, "h1.title")

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/element/$elementId/text",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.containsKey("value"))
    }

    // ==================== Script Execution ====================

    @Test
    fun `should execute synchronous JavaScript`() {
        val sessionId = createSession()

        val request = ScriptRequest(
            script = "return document.title;",
            args = emptyList()
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/execute/sync",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should execute asynchronous JavaScript`() {
        val sessionId = createSession()

        val request = ScriptRequest(
            script = "var callback = arguments[arguments.length - 1]; setTimeout(function(){ callback('done'); }, 100);",
            args = emptyList()
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/execute/async",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should execute script with arguments`() {
        val sessionId = createSession()

        val request = ScriptRequest(
            script = "return arguments[0] + arguments[1];",
            args = listOf(5, 10)
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/execute/sync",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    // ==================== Control Operations ====================

    @Test
    fun `should delay execution`() {
        val sessionId = createSession()

        val request = DelayRequest(ms = 100)
        val entity = HttpEntity(request, jsonHeaders())
        
        val startTime = System.currentTimeMillis()
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/control/delay",
            entity,
            Map::class.java
        )
        val duration = System.currentTimeMillis() - startTime

        assertEquals(HttpStatus.OK, response.statusCode)
        // Verify that some delay occurred
        assertTrue(duration >= 90, "Expected at least 90ms delay, but got ${duration}ms")
    }

    @Test
    fun `should reject excessive delay`() {
        val sessionId = createSession()

        // Request delay longer than maximum
        val request = DelayRequest(ms = 60_000)
        val entity = HttpEntity(request, jsonHeaders())
        
        val startTime = System.currentTimeMillis()
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/control/delay",
            entity,
            Map::class.java
        )
        val duration = System.currentTimeMillis() - startTime

        // Should succeed but cap the delay at MAX_DELAY_MS (30s)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(duration < 35_000, "Delay should be capped at 30s")
    }

    @Test
    fun `should pause session`() {
        val sessionId = createSession()

        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/control/pause",
            HttpEntity<String>(jsonHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `should stop session`() {
        val sessionId = createSession()

        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/control/stop",
            HttpEntity<String>(jsonHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    // ==================== Events Operations ====================

    @Test
    fun `should get all event configs`() {
        val sessionId = createSession()

        // Create some event configs first
        createEventConfig(sessionId, "click")
        createEventConfig(sessionId, "load")

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/event-configs",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as List<Map<String, Any?>>
        assertTrue(value.size >= 2)
    }

    @Test
    fun `should get events from session`() {
        val sessionId = createSession()

        val response = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId/events",
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as List<Any>
        // Should return list of events (may be empty)
        assertNotNull(value)
    }

    @Test
    fun `should subscribe to events`() {
        val sessionId = createSession()

        val request = SubscribeRequest(eventTypes = listOf("click", "load"))
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/events/subscribe",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("subscriptionId"))
    }

    // ==================== Agent API Tests ====================

    @Test
    fun `should run agent with detailed task`() {
        val sessionId = createSession()

        val request = AgentRunRequest(
            task = "Navigate to the login page, find the username field, fill it with 'admin', and click the login button"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/agent/run",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("message"))
    }

    @Test
    fun `should observe page and return action options`() {
        val sessionId = createSession()

        val request = AgentObserveRequest(
            instruction = "Identify all clickable buttons and links on the page"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/agent/observe",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Response is AgentObserveResponse(value = List<ObserveResultDto>)
        assertTrue(response.body!!.containsKey("value"))
        @Suppress("UNCHECKED_CAST")
        val observations = response.body!!["value"] as List<Map<String, Any?>>
        assertTrue(observations.isNotEmpty())
        // Check structure of first observation
        val first = observations[0]
        assertTrue(first.containsKey("locator") || first.containsKey("description"))
    }

    @Test
    fun `should execute specific agent action`() {
        val sessionId = createSession()

        val request = AgentActRequest(
            action = "Click the 'Add to Cart' button"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/agent/act",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("action"))
    }

    @Test
    fun `should extract structured data with schema`() {
        val sessionId = createSession()

        val request = AgentExtractRequest(
            instruction = "Extract product details: name, price, description, and rating"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/agent/extract",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("data"))
    }

    @Test
    fun `should summarize page content`() {
        val sessionId = createSession()

        val request = AgentSummarizeRequest(
            instruction = "Provide a concise summary of the main content on this page"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/agent/summarize",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Response is AgentSummarizeResponse(value = String)
        assertTrue(response.body!!.containsKey("value"))
        val summary = response.body!!["value"] as String?
        assertNotNull(summary)
        assertTrue(summary!!.isNotBlank())
    }

    // ==================== PulsarSession API Tests ====================

    @Test
    fun `should normalize URL without scheme`() {
        val sessionId = createSession()

        val request = NormalizeRequest(url = "example.com/path", args = "-expire 1d")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/normalize",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        val normalized = value["url"] as String
        assertTrue(normalized.startsWith("http"))
    }

    @Test
    fun `should normalize URL with arguments`() {
        val sessionId = createSession()

        val request = NormalizeRequest(
            url = "https://example.com",
            args = "-expire 7d -ignoreFailure"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/normalize",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("spec"))
    }

    @Test
    fun `should open URL bypassing cache`() {
        val sessionId = createSession()

        val request = OpenRequest(url = "https://example.com/fresh")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/open",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertEquals("https://example.com/fresh", value["url"])
    }

    @Test
    fun `should load URL with cache check`() {
        val sessionId = createSession()

        val request = LoadRequest(
            url = "https://example.com/cached",
            args = "-expire 1h"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/load",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertTrue(value.containsKey("protocolStatus"))
    }

    @Test
    fun `should submit URL to crawl pool`() {
        val sessionId = createSession()

        val request = SubmitRequest(
            url = "https://example.com/submit",
            args = "-queue"
        )
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/submit",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!["value"])
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should return 404 for control operations on non-existent session`() {
        val response = restTemplate.postForEntity(
            "$baseUrl/session/invalid-session-id/control/pause",
            HttpEntity<String>(jsonHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.headers["X-Request-Id"])
    }

    @Test
    fun `should return error for agent operations on non-existent session`() {
        val request = AgentRunRequest(task = "test task")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/non-existent/agent/run",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should return error for PulsarSession operations on non-existent session`() {
        val request = NormalizeRequest(url = "example.com")
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/non-existent/normalize",
            entity,
            Map::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    // ==================== End-to-End Workflow Tests ====================

    @Test
    fun `should complete full workflow - create session, navigate, interact, extract data, cleanup`() {
        // 1. Create session
        val sessionId = createSession()
        assertNotNull(sessionId)

        // 2. Navigate to page
        navigateToUrl(sessionId, "https://example.com/products")

        // 3. Check if element exists
        val existsRequest = SelectorRef(selector = "#product-list")
        val existsEntity = HttpEntity(existsRequest, jsonHeaders())
        val existsResponse = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/exists",
            existsEntity,
            Map::class.java
        )
        assertEquals(HttpStatus.OK, existsResponse.statusCode)

        // 4. Click an element
        val clickRequest = SelectorRef(selector = ".product-card")
        val clickEntity = HttpEntity(clickRequest, jsonHeaders())
        val clickResponse = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/click",
            clickEntity,
            Map::class.java
        )
        assertEquals(HttpStatus.OK, clickResponse.statusCode)

        // 5. Extract data using agent
        val extractRequest = AgentExtractRequest(instruction = "Extract product information")
        val extractEntity = HttpEntity(extractRequest, jsonHeaders())
        val extractResponse = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/agent/extract",
            extractEntity,
            Map::class.java
        )
        assertEquals(HttpStatus.OK, extractResponse.statusCode)

        // 6. Delete session
        restTemplate.delete("$baseUrl/session/$sessionId")

        // 7. Verify session is deleted
        val verifyResponse = restTemplate.getForEntity(
            "$baseUrl/session/$sessionId",
            Map::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, verifyResponse.statusCode)
    }

    @Test
    fun `should handle multiple concurrent sessions`() {
        val session1 = createSession()
        val session2 = createSession()
        val session3 = createSession()

        assertNotEquals(session1, session2)
        assertNotEquals(session2, session3)
        assertNotEquals(session1, session3)

        // Verify all sessions exist
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("$baseUrl/session/$session1", Map::class.java).statusCode)
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("$baseUrl/session/$session2", Map::class.java).statusCode)
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("$baseUrl/session/$session3", Map::class.java).statusCode)

        // Cleanup
        restTemplate.delete("$baseUrl/session/$session1")
        restTemplate.delete("$baseUrl/session/$session2")
        restTemplate.delete("$baseUrl/session/$session3")
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to create a session and return the session ID.
     */
    private fun createSession(): String {
        val request = NewSessionRequest()
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session",
            entity,
            Map::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        return value["sessionId"] as String
    }

    /**
     * Helper method to navigate to a URL.
     */
    private fun navigateToUrl(sessionId: String, url: String) {
        val request = SetUrlRequest(url = url)
        val entity = HttpEntity(request, jsonHeaders())
        restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/url",
            entity,
            Map::class.java
        )
    }

    /**
     * Helper method to get an element ID by selector.
     */
    private fun getElementId(sessionId: String, selector: String): String {
        val request = SelectorRef(selector = selector)
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/selectors/element",
            entity,
            Map::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        return value["element-6066-11e4-a52e-4f735466cecf"] as String
    }

    /**
     * Helper method to create an event config.
     */
    private fun createEventConfig(sessionId: String, eventType: String): String {
        val request = EventConfig(eventType = eventType, enabled = true)
        val entity = HttpEntity(request, jsonHeaders())
        val response = restTemplate.postForEntity(
            "$baseUrl/session/$sessionId/event-configs",
            entity,
            Map::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val value = response.body!!["value"] as Map<String, Any?>
        return value["configId"] as String
    }
}
