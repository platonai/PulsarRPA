/**
 * Comprehensive planning tests for WebDriverAgent to evaluate multi-step planning,
 * decision making, adaptation to page changes, and strategic execution capabilities.
 */
package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.tta.ActionDescription
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@Tag("UnitTest")
class WebDriverAgentPlanningTest {

    // Mock server URLs for consistent testing
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
    private lateinit var planningConfig: WebDriverAgentConfig

    @BeforeEach
    fun setup() {
        mockDriver = mockk(relaxed = true)
        planningConfig = WebDriverAgentConfig(
            maxSteps = 15,
            maxRetries = 3,
            baseRetryDelayMs = 100,
            maxRetryDelayMs = 1000,
            consecutiveNoOpLimit = 5,
            enableStructuredLogging = true,
            enableDebugMode = true,
            enablePerformanceMetrics = true,
            enableAdaptiveDelays = true,
            enablePreActionValidation = true
        )
        webDriverAgent = WebDriverAgent(mockDriver, config = planningConfig)

        // Set up default mock behaviors
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot-base64"
        coEvery { mockDriver.currentUrl() } returns interactive1Url
        coEvery { mockDriver.navigateTo(any() as String) } returns Unit
        coEvery { mockDriver.click(any(), any()) } returns Unit
        coEvery { mockDriver.fill(any(), any()) } returns Unit
        coEvery { mockDriver.scrollDown(any()) } returns Unit
        coEvery { mockDriver.scrollUp(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `Given multi-step form filling task When planning Then creates sequential action plan`() {
        // Given - Complex multi-step form interaction
        val instruction = """
            Navigate to $interactive1Url,
            enter your name in the name input field,
            select a favorite color from the dropdown,
            enter two numbers in the calculator fields,
            click the Add button to calculate the sum,
            click the Toggle Message button to show/hide the message,
            and verify all interactive elements work correctly
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock step-by-step execution with realistic responses
        var stepCount = 0
        coEvery { mockDriver.captureScreenshot() } answers {
            stepCount++
            when (stepCount) {
                1 -> "initial-page-screenshot"
                2 -> "name-entered-screenshot"
                3 -> "color-selected-screenshot"
                4 -> "calculator-filled-screenshot"
                5 -> "sum-calculated-screenshot"
                6 -> "message-toggled-screenshot"
                else -> "final-verification-screenshot"
            }
        }

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(stepCount >= 6, "Should have executed at least 6 steps for complex form interaction")
        assertTrue(result.content.contains("name") || result.content.contains("color") ||
                  result.content.contains("calculator") || result.content.contains("toggle"),
                  "Should contain references to interactive elements")
    }

    @Test
    fun `Given dynamic content task When planning Then adapts to page state changes`() {
        // Given - Task requiring adaptation to dynamic content
        val instruction = """
            Navigate to $interactiveDynamicUrl,
            wait for the dynamic content to load,
            interact with the newly appeared elements,
            handle any popups or alerts that appear,
            and verify the dynamic functionality works correctly
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock dynamic behavior with state changes
        var pageState = "loading"
        coEvery { mockDriver.currentUrl() } answers {
            when (pageState) {
                "loaded" -> interactiveDynamicUrl
                else -> interactive1Url
            }
        }

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.contains("dynamic") || result.content.contains("load") ||
                  result.content.contains("popup") || result.content.contains("alert"),
                  "Should contain references to dynamic content handling")
    }

    @Test
    fun `Given multi-page navigation task When planning Then coordinates cross-page actions`() {
        // Given - Complex multi-page workflow
        val instruction = """
            Navigate to $interactive1Url and interact with the name input field,
            then navigate to $interactive2Url and interact with the font size slider,
            then navigate to $interactive3Url and toggle the demo box,
            then navigate to $interactive4Url and test the dark mode toggle,
            and provide a summary of all actions performed across all pages
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock multi-page navigation with different page states
        val pageStates = mapOf(
            interactive1Url to "name-interaction-complete",
            interactive2Url to "slider-adjusted",
            interactive3Url to "demo-box-toggled",
            interactive4Url to "dark-mode-tested"
        )

        var currentUrl = interactive1Url
        coEvery { mockDriver.currentUrl() } answers { currentUrl }
        coEvery { mockDriver.navigateTo(any() as String) } answers {
            currentUrl = firstArg()
            Unit
        }

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.length > 200, "Should contain detailed multi-page execution summary")
        assertTrue(result.content.contains("name") && result.content.contains("slider") &&
                  result.content.contains("demo") && result.content.contains("dark"),
                  "Should contain references to specific interactive elements from each page")
    }

    @Test
    fun `Given conditional logic task When planning Then handles if-then scenarios`() {
        // Given - Task with conditional execution paths
        val instruction = """
            Navigate to $interactive1Url,
            check if the name input field is visible,
            if visible then enter your name and proceed with form interaction,
            if not visible then try alternative interaction methods,
            check if the calculator section exists,
            if calculator exists then perform addition with two numbers,
            if calculator doesn't exist then interact with available elements,
            and provide a summary of what was actually executed
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock conditional scenarios with element existence checks
        coEvery { mockDriver.waitForSelector(any(), any() as Long) } returns 1000L

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.contains("visible") || result.content.contains("exists") ||
                  result.content.contains("alternative") || result.content.contains("available"),
                  "Should contain references to conditional logic execution")
    }

    @Test
    fun `Given error recovery task When planning Then implements fallback strategies`() {
        // Given - Task requiring error handling and recovery
        val instruction = """
            Navigate to $interactive1Url,
            attempt to interact with the name input field,
            if the field is not responsive, try alternative selectors,
            if standard interaction fails, use JavaScript execution,
            if JavaScript fails, try keyboard navigation,
            if all methods fail, move to next available element,
            continue with remaining interactions using available methods,
            and document which fallback strategies were successful
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock progressive fallback scenarios
        var attemptCount = 0
        coEvery { mockDriver.fill(any(), any()) } answers {
            attemptCount++
            if (attemptCount <= 2) {
                throw Exception("Element not interactable")
            }
            Unit
        }

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.contains("fallback") || result.content.contains("alternative") ||
                  result.content.contains("JavaScript") || result.content.contains("keyboard") ||
                  result.content.contains("available"),
                  "Should contain references to fallback strategy execution")
    }

    @Test
    fun `Given data extraction task When planning Then coordinates gathering and validation`() {
        // Given - Task requiring data collection and validation
        val instruction = """
            Navigate to $interactive2Url,
            extract all form field labels and their current values,
            collect information about slider positions and toggle states,
            gather data about CSS classes and styling information,
            validate that all required fields have appropriate data,
            check that interactive elements are in expected states,
            and provide a comprehensive data summary report
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock data extraction scenarios
        coEvery { mockDriver.evaluate(any()) } returnsMany listOf(
            "{\"name\": \"John Doe\", \"language\": \"Kotlin\", \"subscribed\": true}",
            "{\"fontSize\": 18, \"textSize\": 16}",
            "{\"classes\": [\"interactive\", \"form\"], \"styles\": {}}",
            "{\"validation\": \"passed\", \"required\": true}"
        )

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.contains("extract") || result.content.contains("collect") ||
                  result.content.contains("gather") || result.content.contains("validate") ||
                  result.content.contains("data") || result.content.contains("summary"),
                  "Should contain references to data extraction and validation")
    }

    @Test
    fun `Given performance optimization task When planning Then implements efficient execution strategies`() {
        // Given - Task requiring performance-conscious execution
        val instruction = """
            Navigate to $interactive3Url,
            interact with all controls efficiently without unnecessary delays,
            batch similar operations together for better performance,
            avoid redundant screenshot captures where possible,
            use optimal selectors that minimize DOM traversal,
            implement smart waiting strategies instead of fixed delays,
            and measure the overall execution performance
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // When
        val startTime = System.currentTimeMillis()
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }
        val endTime = System.currentTimeMillis()

        // Then
        assertNotNull(result)
        assertTrue(endTime - startTime < 30000, "Should complete within reasonable time")
        assertTrue(result.content.contains("efficient") || result.content.contains("performance") ||
                  result.content.contains("batch") || result.content.contains("optimal") ||
                  result.content.contains("smart"),
                  "Should contain references to performance optimization")
    }

    @Test
    fun `Given ambiguous element task When planning Then implements disambiguation strategies`() {
        // Given - Task with element ambiguity requiring resolution
        val instruction = """
            Navigate to $interactiveAmbiguityUrl,
            when multiple elements match the same selector, use additional context to choose,
            consider element visibility, position, and surrounding text,
            use CSS classes and attributes to distinguish between similar elements,
            implement priority scoring for element selection,
            if ambiguity cannot be resolved, interact with the most likely candidate,
            and document the disambiguation strategy used
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock ambiguous element scenarios
        coEvery { mockDriver.waitForSelector(any(), any() as Long) } returns 1000L
        coEvery { mockDriver.evaluate(any()) } returnsMany listOf(
            "{\"count\": 3, \"visible\": 2}",
            "{\"positions\": [\"top\", \"middle\", \"bottom\"]}",
            "{\"texts\": [\"Submit\", \"Submit Form\", \"Send\"]}"
        )

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.contains("ambiguous") || result.content.contains("disambiguation") ||
                  result.content.contains("context") || result.content.contains("priority") ||
                  result.content.contains("selection"),
                  "Should contain references to ambiguity resolution")
    }

    @Test
    fun `Given comprehensive workflow task When planning Then orchestrates complex multi-phase execution`() {
        // Given - Complex workflow requiring sophisticated planning
        val instruction = """
            Navigate to $interactive1Url and complete the user information section,
            navigate to $interactive2Url and configure all preference settings,
            navigate to $interactive3Url and test all control interactions,
            navigate to $interactive4Url and verify advanced features,
            extract data from each page for cross-validation,
            perform end-to-end testing of the complete workflow,
            generate a comprehensive execution report with findings,
            and provide recommendations for any issues discovered
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock comprehensive workflow execution
        val workflowSteps = listOf(
            "user-info-complete", "preferences-configured",
            "controls-tested", "features-verified",
            "data-extracted", "cross-validation-complete",
            "end-to-end-tested", "report-generated"
        )

        var currentStep = 0
        coEvery { mockDriver.captureScreenshot() } answers {
            currentStep++
            "workflow-step-$currentStep-screenshot"
        }

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(currentStep >= 8, "Should have executed comprehensive workflow steps")
        assertTrue(result.content.length > 300, "Should contain detailed workflow report")
        assertTrue(result.content.contains("workflow") || result.content.contains("comprehensive") ||
                  result.content.contains("report") || result.content.contains("findings") ||
                  result.content.contains("recommendations"),
                  "Should contain references to comprehensive workflow execution")
    }

    @Test
    fun `Given planning adaptation task When environment changes Then revises strategy appropriately`() {
        // Given - Task requiring planning adaptation mid-execution
        val instruction = """
            Navigate to $interactiveDynamicUrl,
            start with the initial page layout and interact with visible elements,
            when the page layout changes due to dynamic loading, adapt the interaction strategy,
            if new elements appear, incorporate them into the execution plan,
            if existing elements disappear, find alternative interaction paths,
            maintain execution continuity despite environmental changes,
            and document how the plan adapted to the changing conditions
        """.trimIndent()
        val actionOptions = ActionOptions(instruction)

        // Mock dynamic environment changes
        var environmentState = "initial"
        coEvery { mockDriver.evaluate(any()) } answers {
            when (environmentState) {
                "initial" -> "{\"elements\": 5, \"layout\": \"simple\"}"
                "changed" -> "{\"elements\": 8, \"layout\": \"complex\"}"
                else -> "{\"elements\": 6, \"layout\": \"final\"}"
            }
        }

        // Simulate environment change mid-execution
        runBlocking {
            delay(100) // Small delay to simulate execution time
            environmentState = "changed"
        }

        // When
        val result = runBlocking {
            webDriverAgent.execute(actionOptions)
        }

        // Then
        assertNotNull(result)
        assertTrue(result.content.contains("adapt") || result.content.contains("change") ||
                  result.content.contains("dynamic") || result.content.contains("strategy") ||
                  result.content.contains("environment"),
                  "Should contain references to planning adaptation")
    }
}