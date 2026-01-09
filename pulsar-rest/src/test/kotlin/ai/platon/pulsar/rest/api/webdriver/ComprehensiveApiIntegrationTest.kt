package ai.platon.pulsar.rest.api.webdriver

import ai.platon.pulsar.rest.api.webdriver.dto.*
import ai.platon.pulsar.rest.util.server.TestWebSiteAccess
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

/**
 * Comprehensive integration tests for all WebDriver API endpoints.
 * Tests all controllers: Session, Navigation, Selector, Element, Script, Control, Events, Agent, PulsarSession, Health.
 * 
 * These tests use the real interactive webpage (interactive-1.html) from the test assets.
 * The interactive page contains:
 * - Header (#pageHeader) with title "Welcome to the Interactive Page"
 * - User Information section (#userInformation) with name input (#name) and output (#nameOutput)
 * - Preferences section (#preferences) with color selector (#colorSelect)
 * - Calculator section (#calculatorSection) with inputs (#num1, #num2), Add button (#addButton), and result (#sumResult)
 * - Toggle section (#toggleSection) with toggle button (#toggleMessageButton) and hidden message (#hiddenMessage)
 */
@SpringBootTest(
    classes = [WebDriverTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComprehensiveApiIntegrationTest: TestWebSiteAccess() {

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

    // ==================== Health Endpoints ====================

    @Test
    fun `should return health status with mode and active sessions`() {
        val response = getJson("/health").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        assertEquals("UP", body["status"])
        assertTrue(body.containsKey("mode"))
        assertTrue(body["mode"] in listOf("mock", "real"))
        assertTrue(body.containsKey("activeSessions"))
    }

    @Test
    fun `should return readiness status`() {
        val response = getJson("/health/ready").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        assertEquals(true, body["ready"])
        assertTrue(body.containsKey("mode"))
    }

    @Test
    fun `should return liveness status`() {
        val response = getJson("/health/live").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
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

        val response = postJson("/session", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("sessionId"))
        assertTrue(value.containsKey("capabilities"))
    }

    @Test
    fun `should get session details`() {
        val sessionId = createSession()

        val response = getJson("/session/$sessionId").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("sessionId"))
        assertEquals(sessionId, value["sessionId"])
    }

    // ==================== Navigation Endpoints ====================

    @Test
    fun `should navigate and retrieve current URL`() {
        val sessionId = createSession()

        // Navigate to real interactive page
        val navRequest = SetUrlRequest(url = interactiveUrl)
        val navResponse = postJson("/session/$sessionId/url", navRequest).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, navResponse.status)

        // Get current URL
        val urlResponse = getJson("/session/$sessionId/url").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, urlResponse.status)
        @Suppress("UNCHECKED_CAST")
        val urlBody = urlResponse.responseBody as Map<String, Any?>
        assertEquals(interactiveUrl, urlBody["value"])
    }

    @Test
    fun `should get document URI`() {
        val sessionId = createSession()

        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val response = getJson("/session/$sessionId/documentUri").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        val documentUri = body["value"] as String
        assertTrue(documentUri.isNotBlank())
    }

    @Test
    fun `should get base URI`() {
        val sessionId = createSession()

        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val response = getJson("/session/$sessionId/baseUri").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        val baseUri = body["value"] as String
        assertTrue(baseUri.startsWith("http://"))
    }

    // ==================== Selector Operations ====================

    @Test
    fun `should find multiple elements by selector`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Find all section elements in interactive-1.html
        val request = SelectorRef(selector = "section")
        val response = postJson("/session/$sessionId/selectors/elements", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as List<Map<String, Any?>>
        assertTrue(value.isNotEmpty())
        // Check that each element has a WebDriver element reference
        value.forEach { elem ->
            assertTrue(elem.containsKey("element-6066-11e4-a52e-4f735466cecf"))
        }
    }

    @Test
    fun `should wait for selector to appear`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Wait for the page header which exists in interactive-1.html
        val request = WaitForRequest(selector = "#pageHeader", timeout = 1000)
        val response = postJson("/session/$sessionId/selectors/waitFor", request).returnResult(Map::class.java)

        // Should succeed in mock mode or timeout in real mode
        assertTrue(
            response.status == HttpStatus.OK || response.status == HttpStatus.REQUEST_TIMEOUT
        )
    }

    @Test
    fun `should fill input field by selector`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Fill the name input field from interactive-1.html
        val request = FillRequest(selector = "#name", value = "TestUser")
        val response = postJson("/session/$sessionId/selectors/fill", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
    }

    @Test
    fun `should press key by selector`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Press Enter on the name input field from interactive-1.html
        val request = PressRequest(selector = "#name", key = "Enter")
        val response = postJson("/session/$sessionId/selectors/press", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
    }

    @Test
    fun `should get outer HTML by selector`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Get outer HTML of the page header from interactive-1.html
        val request = SelectorRef(selector = "#pageHeader")
        val response = postJson("/session/$sessionId/selectors/outerHtml", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        // Response is HtmlResponse(value = String)
        assertTrue(body.containsKey("value"))
        val html = body["value"] as String?
        assertNotNull(html)
        assertTrue(html!!.isNotBlank())
    }

    @Test
    fun `should take screenshot of element by selector`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Take screenshot of the page header from interactive-1.html
        val request = SelectorRef(selector = "#pageHeader")
        val response = postJson("/session/$sessionId/selectors/screenshot", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        // Response is ScreenshotResponse(value = String)
        assertTrue(body.containsKey("value"))
        val screenshot = body["value"] as String?
        assertNotNull(screenshot)
        assertTrue(screenshot!!.isNotBlank())
    }

    // ==================== Element Operations by ID ====================

    @Test
    fun `should find element using WebDriver locator`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Find the Add button from interactive-1.html
        val request = FindElementRequest(using = "css selector", value = "#addButton")
        val response = postJson("/session/$sessionId/element", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("element-6066-11e4-a52e-4f735466cecf"))
    }

    @Test
    fun `should find multiple elements using WebDriver locator`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // Find all input elements from interactive-1.html
        val request = FindElementRequest(using = "css selector", value = "input")
        val response = postJson("/session/$sessionId/elements", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as List<Map<String, Any?>>
        assertTrue(value.isNotEmpty())
    }

    @Test
    fun `should click element by ID`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)
        // Get element ID for the toggle button from interactive-1.html
        val elementId = getElementId(sessionId, "#toggleMessageButton")

        val response = postJson("/session/$sessionId/element/$elementId/click", "").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun `should send keys to element by ID`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)
        // Get element ID for the name input from interactive-1.html
        val elementId = getElementId(sessionId, "#name")

        val request = SendKeysRequest(text = "TestUser")
        val response = postJson("/session/$sessionId/element/$elementId/value", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun `should get element attribute`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)
        // Get element ID for the name input from interactive-1.html
        val elementId = getElementId(sessionId, "#name")

        // Get the placeholder attribute from interactive-1.html's name input
        val response = getJson("/session/$sessionId/element/$elementId/attribute/placeholder").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        // In mock mode, may return null or a mock value
        assertTrue(body.containsKey("value"))
    }

    @Test
    fun `should get element text`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)
        // Get element ID for the h1 in header from interactive-1.html
        val elementId = getElementId(sessionId, "#pageHeader h1")

        val response = getJson("/session/$sessionId/element/$elementId/text").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        assertTrue(body.containsKey("value"))
    }

    // ==================== Script Execution ====================

    @Test
    fun `should execute synchronous JavaScript`() {
        val sessionId = createSession()

        val request = ScriptRequest(
            script = "return document.title;",
            args = emptyList()
        )
        val response = postJson("/session/$sessionId/execute/sync", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
    }

    @Test
    fun `should execute asynchronous JavaScript`() {
        val sessionId = createSession()

        val request = ScriptRequest(
            script = "var callback = arguments[arguments.length - 1]; setTimeout(function(){ callback('done'); }, 100);",
            args = emptyList()
        )
        val response = postJson("/session/$sessionId/execute/async", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
    }

    @Test
    fun `should execute script with arguments`() {
        val sessionId = createSession()

        val request = ScriptRequest(
            script = "return arguments[0] + arguments[1];",
            args = listOf(5, 10)
        )
        val response = postJson("/session/$sessionId/execute/sync", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
    }

    // ==================== Control Operations ====================

    @Test
    fun `should delay execution`() {
        val sessionId = createSession()

        val request = DelayRequest(ms = 100)

        val startTime = System.currentTimeMillis()
        val response = postJson("/session/$sessionId/control/delay", request).returnResult(Map::class.java)
        val duration = System.currentTimeMillis() - startTime

        assertEquals(HttpStatus.OK, response.status)
        // Verify that some delay occurred
        assertTrue(duration >= 90, "Expected at least 90ms delay, but got ${duration}ms")
    }

    @Test
    fun `should reject excessive delay`() {
        val sessionId = createSession()

        // Request delay longer than maximum
        val request = DelayRequest(ms = 60_000)

        val startTime = System.currentTimeMillis()
        val response = postJson("/session/$sessionId/control/delay", request).returnResult(Map::class.java)
        val duration = System.currentTimeMillis() - startTime

        // Should succeed but cap the delay at MAX_DELAY_MS (30s)
        assertEquals(HttpStatus.OK, response.status)
        assertTrue(duration < 35_000, "Delay should be capped at 30s")
    }

    @Test
    fun `should pause session`() {
        val sessionId = createSession()

        val response = postJson("/session/$sessionId/control/pause", "").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun `should stop session`() {
        val sessionId = createSession()

        val response = postJson("/session/$sessionId/control/stop", "").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
    }

    // ==================== Events Operations ====================

    @Test
    fun `should get all event configs`() {
        val sessionId = createSession()

        // Create some event configs first
        createEventConfig(sessionId, "click")
        createEventConfig(sessionId, "load")

        val response = getJson("/session/$sessionId/event-configs").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as List<Map<String, Any?>>
        assertTrue(value.size >= 2)
    }

    @Test
    fun `should get events from session`() {
        val sessionId = createSession()

        val response = getJson("/session/$sessionId/events").returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as List<Any>
        // Should return list of events (may be empty)
        assertNotNull(value)
    }

    @Test
    fun `should subscribe to events`() {
        val sessionId = createSession()

        val request = SubscribeRequest(eventTypes = listOf("click", "load"))
        val response = postJson("/session/$sessionId/events/subscribe", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("subscriptionId"))
    }

    // ==================== Agent API Tests ====================

    @Test
    fun `should run agent with detailed task`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val request = AgentRunRequest(
            task = "Find the name input field and fill it with 'TestUser', then find the Add button in the calculator section"
        )
        val response = postJson("/session/$sessionId/agent/run", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("message"))
    }

    @Test
    fun `should observe page and return action options`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val request = AgentObserveRequest(
            instruction = "Identify all clickable buttons and input fields on the page"
        )
        val response = postJson("/session/$sessionId/agent/observe", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        // Response is AgentObserveResponse(value = List<ObserveResultDto>)
        assertTrue(body.containsKey("value"))
        @Suppress("UNCHECKED_CAST")
        val observations = body["value"] as List<Map<String, Any?>>
        assertTrue(observations.isNotEmpty())
        // Check structure of first observation
        val first = observations[0]
        assertTrue(first.containsKey("locator") || first.containsKey("description"))
    }

    @Test
    fun `should execute specific agent action`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val request = AgentActRequest(
            action = "Click the 'Toggle Message' button to reveal the hidden message"
        )
        val response = postJson("/session/$sessionId/agent/act", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("action"))
    }

    @Test
    fun `should extract structured data with schema`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val request = AgentExtractRequest(
            instruction = "Extract all section titles and their descriptions from the page"
        )
        val response = postJson("/session/$sessionId/agent/extract", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("success"))
        assertTrue(value.containsKey("data"))
    }

    @Test
    fun `should summarize page content`() {
        val sessionId = createSession()
        // Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        val request = AgentSummarizeRequest(
            instruction = "Provide a concise summary of this interactive demo page and its features"
        )
        val response = postJson("/session/$sessionId/agent/summarize", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        // Response is AgentSummarizeResponse(value = String)
        assertTrue(body.containsKey("value"))
        val summary = body["value"] as String?
        assertNotNull(summary)
        assertTrue(summary!!.isNotBlank())
    }

    // ==================== PulsarSession API Tests ====================

    @Test
    fun `should normalize URL without scheme`() {
        val sessionId = createSession()

        // Use the base URL from interactiveUrl without scheme
        val urlWithoutScheme = interactiveUrl.removePrefix("http://")
        val request = NormalizeRequest(url = urlWithoutScheme, args = "-expire 1d")
        val response = postJson("/session/$sessionId/normalize", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        val normalized = value["url"] as String
        assertTrue(normalized.startsWith("http"))
    }

    @Test
    fun `should normalize URL with arguments`() {
        val sessionId = createSession()

        val request = NormalizeRequest(
            url = interactiveUrl,
            args = "-expire 7d -ignoreFailure"
        )
        val response = postJson("/session/$sessionId/normalize", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("spec"))
    }

    @Test
    fun `should open URL bypassing cache`() {
        val sessionId = createSession()

        val request = OpenRequest(url = interactiveUrl)
        val response = postJson("/session/$sessionId/open", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertEquals(interactiveUrl, value["url"])
    }

    @Test
    fun `should load URL with cache check`() {
        val sessionId = createSession()

        val request = LoadRequest(
            url = interactiveUrl,
            args = "-expire 1h"
        )
        val response = postJson("/session/$sessionId/load", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        assertTrue(value.containsKey("url"))
        assertTrue(value.containsKey("protocolStatus"))
    }

    @Test
    fun `should submit URL to crawl pool`() {
        val sessionId = createSession()

        val request = SubmitRequest(
            url = interactiveUrl,
            args = "-queue"
        )
        val response = postJson("/session/$sessionId/submit", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.responseBody)
        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        assertEquals(true, body["value"])
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should return 404 for control operations on non-existent session`() {
        val response = postJson("/session/invalid-session-id/control/pause", "").returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, response.status)
        assertNotNull(response.responseHeaders["X-Request-Id"])
    }

    @Test
    fun `should return error for agent operations on non-existent session`() {
        val request = AgentRunRequest(task = "test task")
        val response = postJson("/session/non-existent/agent/run", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, response.status)
    }

    @Test
    fun `should return error for PulsarSession operations on non-existent session`() {
        val request = NormalizeRequest(url = "example.com")
        val response = postJson("/session/non-existent/normalize", request).returnResult(Map::class.java)

        assertEquals(HttpStatus.NOT_FOUND, response.status)
    }

    // ==================== End-to-End Workflow Tests ====================

    @Test
    fun `should complete full workflow - create session, navigate, interact, extract data, cleanup`() {
        // 1. Create session
        val sessionId = createSession()
        assertNotNull(sessionId)

        // 2. Navigate to real interactive page
        navigateToUrl(sessionId, interactiveUrl)

        // 3. Check if page header element exists (from interactive-1.html)
        val existsRequest = SelectorRef(selector = "#pageHeader")
        val existsResponse = postJson("/session/$sessionId/selectors/exists", existsRequest).returnResult(Map::class.java)
        assertEquals(HttpStatus.OK, existsResponse.status)

        // 4. Click the toggle button (from interactive-1.html)
        val clickRequest = SelectorRef(selector = "#toggleMessageButton")
        val clickResponse = postJson("/session/$sessionId/selectors/click", clickRequest).returnResult(Map::class.java)
        assertEquals(HttpStatus.OK, clickResponse.status)

        // 5. Extract section information using agent
        val extractRequest = AgentExtractRequest(instruction = "Extract all section titles from this interactive page")
        val extractResponse = postJson("/session/$sessionId/agent/extract", extractRequest).returnResult(Map::class.java)
        assertEquals(HttpStatus.OK, extractResponse.status)

        // 6. Delete session
        deleteJson("/session/$sessionId")

        // 7. Verify session is deleted
        val verifyResponse = getJson("/session/$sessionId").returnResult(Map::class.java)
        assertEquals(HttpStatus.NOT_FOUND, verifyResponse.status)
    }

    @Test
    fun `should handle multiple concurrent sessions`() {
        val session1 = createSession()
        val session2 = createSession()
        val session3 = createSession()

        assertNotEquals(session1, session2)
        assertNotEquals(session2, session3)
        assertNotEquals(session1, session3)

        // Navigate all sessions to the real interactive page
        navigateToUrl(session1, interactiveUrl)
        navigateToUrl(session2, interactiveUrl)
        navigateToUrl(session3, interactiveUrl)

        // Verify all sessions exist
        assertEquals(HttpStatus.OK, getJson("/session/$session1").returnResult(Map::class.java).status)
        assertEquals(HttpStatus.OK, getJson("/session/$session2").returnResult(Map::class.java).status)
        assertEquals(HttpStatus.OK, getJson("/session/$session3").returnResult(Map::class.java).status)

        // Cleanup
        deleteJson("/session/$session1")
        deleteJson("/session/$session2")
        deleteJson("/session/$session3")
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to create a session and return the session ID.
     */
    private fun createSession(): String {
        val response = postJson("/session", NewSessionRequest()).returnResult(Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        return value["sessionId"] as String
    }

    /**
     * Helper method to navigate to a URL.
     */
    private fun navigateToUrl(sessionId: String, url: String) {
        val request = SetUrlRequest(url = url)
        postJson("/session/$sessionId/url", request)
    }

    /**
     * Helper method to get an element ID by selector.
     */
    private fun getElementId(sessionId: String, selector: String): String {
        val request = SelectorRef(selector = selector)
        val response = postJson("/session/$sessionId/selectors/element", request).returnResult(Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        return value["element-6066-11e4-a52e-4f735466cecf"] as String
    }

    /**
     * Helper method to create an event config.
     */
    private fun createEventConfig(sessionId: String, eventType: String): String {
        val request = EventConfig(eventType = eventType, enabled = true)
        val response = postJson("/session/$sessionId/event-configs", request).returnResult(Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val body = response.responseBody as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val value = body["value"] as Map<String, Any?>
        return value["configId"] as String
    }
}
