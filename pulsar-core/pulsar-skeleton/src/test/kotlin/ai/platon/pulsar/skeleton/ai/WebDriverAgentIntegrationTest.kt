package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.llm.TTATestBase
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for WebDriverAgent using mock server resources
 */
@Tag("IntegrationTest")
class WebDriverAgentIntegrationTest : TTATestBase() {

    // Local mock server URLs for reliable testing
    private val mockServerBaseURL = "http://127.0.0.1:18182/generated/tta"
    private val interactive1Url = "$mockServerBaseURL/interactive-1.html"
    private val interactive2Url = "$mockServerBaseURL/interactive-2.html"
    private val interactive3Url = "$mockServerBaseURL/interactive-3.html"
    private val interactive4Url = "$mockServerBaseURL/interactive-4.html"
    private val interactiveAmbiguityUrl = "$mockServerBaseURL/interactive-ambiguity.html"
    private val interactiveDynamicUrl = "$mockServerBaseURL/interactive-dynamic.html"
    private val interactiveScreensUrl = "$mockServerBaseURL/interactive-screens.html"

    private lateinit var mockDriver: WebDriver
    private lateinit var webDriverAgent: WebDriverAgent

    @BeforeEach
    fun setup() {
        // Create mock driver for testing
        mockDriver = mockk(relaxed = true)

        // Setup mock driver behavior
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns interactive1Url
        coEvery { mockDriver.navigateTo(any() as String) } returns Unit

        // Create WebDriverAgent with mock driver
        webDriverAgent = WebDriverAgent(mockDriver, maxSteps = 20)
    }

    @AfterEach
    fun tearDown() {
        // Clean up mocks
        clearAllMocks()
    }

    @Test
    fun `Given real browser context When navigating to interactive test page Then executes successfully`() {
        // Given
        val testUrl = interactive1Url
        val instruction = "Navigate to $testUrl and verify the page loads"
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        assertTrue(result.state.name in listOf("SUCCESS", "OTHER"))
    }

    @Test
    fun `Given form interaction task When filling and submitting Then handles form controls correctly`() {
        // Given
        val testUrl = interactive1Url
        val instruction = "Navigate to $testUrl, enter name 'Test User', select favorite color, perform addition calculation, and toggle the hidden message"
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Verify that interactive elements were manipulated
        assertTrue(result.content.contains("name") || result.content.contains("color") ||
                  result.content.contains("calculation") || result.content.contains("toggle"),
                  "Should contain references to interactive elements")
    }

    @Test
    fun `Given screenshot capture When requested Then saves screenshots successfully`() {
        // Given
        val testUrl = interactive1Url
        val instruction = "Navigate to $testUrl, take a screenshot of the page, and verify the page content is visible"
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Screenshots should be captured without errors
        assertFalse(result.content.contains("ERROR"), "Should not contain screenshot errors")
    }

    @Test
    fun `Given navigation task with multiple steps When executing Then maintains browser state correctly`() {
        // Given
        val instruction = """
            Navigate to $interactive1Url,
            interact with the name input field,
            then navigate to $interactive2Url,
            and interact with the summary button
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should complete multiple navigation steps with interactions
        assertTrue(result.content.contains("navigateTo") || result.content.contains("complete") ||
                  result.content.contains("name") || result.content.contains("summary"),
            "Should contain navigation actions or interactive element references")
    }

    @Test
    fun `Given element interaction task When clicking and typing Then performs actions correctly`() {
        // Given
        val testUrl = interactive1Url
        val instruction = """
            Navigate to $testUrl,
            enter your name in the name input field,
            select a favorite color from the dropdown,
            click the Add button after entering two numbers,
            and verify all interactions work correctly
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should contain specific interactive element references
        assertTrue(result.content.contains("name") || result.content.contains("color") ||
                  result.content.contains("Add") || result.content.contains("click"),
            "Should contain specific interactive element references")
    }

    @Test
    fun `Given waiting task When waiting for elements Then handles timeouts gracefully`() {
        // Given
        val testUrl = interactive1Url
        val instruction = """
            Navigate to $testUrl,
            wait for the name input field to be visible,
            and verify the interactive elements are present
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should handle waiting for specific interactive elements
        assertFalse(result.content.contains("timeout") || result.content.contains("ERROR"),
            "Should not contain timeout errors when waiting for interactive elements")
    }

    @Test
    fun `Given scrolling task When scrolling page Then performs scroll actions`() {
        // Given
        val testUrl = interactiveScreensUrl
        val instruction = """
            Navigate to $testUrl,
            scroll down to see all sections,
            scroll back to the top,
            and verify all interactive elements are visible
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should contain scroll actions for multi-screen content
        assertTrue(result.content.contains("scroll") || result.content.contains("complete") ||
                  result.content.contains("section"),
            "Should contain scroll actions or section references")
    }

    @Test
    fun `Given complex multi-step task When executing Then maintains execution history`() {
        // Given
        val instruction = """
            Navigate to $interactive1Url,
            interact with the name input field,
            navigate to $interactive2Url,
            interact with the font size slider,
            navigate to $interactive3Url,
            toggle the demo box,
            and provide a summary of all actions performed
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should provide detailed execution summary with specific interactive elements
        assertTrue(result.content.length > 100, "Should contain detailed multi-step execution summary")
        assertTrue(result.content.contains("name") || result.content.contains("slider") ||
                  result.content.contains("box") || result.content.contains("toggle"),
                  "Should contain references to specific interactive elements")
    }

    @Test
    fun `Given error-prone task When encountering issues Then handles errors gracefully`() {
        // Given
        val instruction = """
            Navigate to $interactive1Url,
            try to interact with elements that might not respond immediately,
            handle any errors that occur,
            and complete the task gracefully
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should complete despite potential interaction issues
        assertTrue(result.content.contains("complete") || result.content.contains("summary") ||
                  result.content.contains("name") || result.content.contains("interactive"),
            "Should complete execution and provide summary with interactive references")
    }

    @Test
    fun `Given task completion detection When task is complete Then stops execution properly`() {
        // Given
        val instruction = """
            Navigate to $interactive1Url,
            interact with the name input field by entering 'Test User',
            verify the interactive elements are working correctly,
            and mark the task as complete
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotBlank())
        // Should indicate successful completion with interactive elements
        assertTrue(result.content.contains("complete") || result.content.contains("success") ||
                  result.content.contains("name") || result.content.contains("interactive"),
            "Should indicate task completion with interactive elements")
    }
}