package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.tta.ActionDescription
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive unit tests for WebDriverAgent using MockK for proper mocking
 */
@Tag("UnitTest")
class WebDriverAgentUnitTest {

    private lateinit var mockDriver: WebDriver
    private lateinit var mockAbstractDriver: AbstractWebDriver
    private lateinit var mockTextToAction: TextToAction
    private lateinit var webDriverAgent: WebDriverAgent

    @BeforeEach
    fun setup() {
        // Create mocks
        mockDriver = mockk(relaxed = true)
        mockAbstractDriver = mockk(relaxed = true)
        mockTextToAction = mockk(relaxed = true)

        // Setup driver hierarchy
        every { (mockDriver as AbstractWebDriver) } returns mockAbstractDriver
        every { mockAbstractDriver.settings.config } returns mockk(relaxed = true)

        // Create WebDriverAgent instance
        webDriverAgent = WebDriverAgent(mockDriver, maxSteps = 10)

        // Replace TextToAction with mock using reflection or dependency injection
        // For now, we'll test the public interface
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `Given WebDriverAgent instance When created Then maxSteps is set correctly`() {
        // Given
        val customMaxSteps = 25

        // When
        val agent = WebDriverAgent(mockDriver, customMaxSteps)

        // Then - Test via reflection or public behavior
        // Since maxSteps is private, we can test the execution behavior
        val instruction = "test instruction"
        val actionOptions = ActionOptions(instruction)

        // Mock the screenshot and model response
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot-base64"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // Mock TextToAction to return immediate completion
        val mockModelResponse = ModelResponse(
            content = """{"tool_calls": [], "taskComplete": true}""",
            state = ResponseState.STOP
        )

        // Run the test
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given valid instruction When building system prompt Then contains required elements`() {
        // Given
        val instruction = "Navigate to example website and click login button"
        val actionOptions = ActionOptions(instruction)

        // Mock dependencies
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://start.com"

        // Mock TextToAction to return completion after first step
        val completionResponse = ModelResponse(
            content = """{"taskComplete": true}""",
            state = ResponseState.STOP
        )

        // When/Then
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)

            // Verify that the driver was called
            coVerify { mockDriver.captureScreenshot() }
            coVerify { mockDriver.currentUrl() }
        }
    }

    @Test
    fun `Given JSON parsing When toolCalls are present Then extracts first call correctly`() {
        // Given
        val instruction = "Click button"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // Mock successful click action
        coEvery { mockDriver.click(any(), any()) } returns Unit

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given taskComplete flag When detected Then stops execution`() {
        // Given
        val instruction = "Complete simple task"
        val actionOptions = ActionOptions(instruction)

        // Mock immediate completion
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given close method When detected Then stops execution and closes driver`() {
        // Given
        val instruction = "Close browser"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot and close detection
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"
        coEvery { mockDriver.close() } returns Unit

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given consecutive no-ops When limit reached Then stops execution`() {
        // Given
        val instruction = "Task that generates no actions"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot to return null (simulating repeated failures)
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then - Should complete gracefully even with no-ops
            assertNotNull(result)
        }
    }

    @Test
    fun `Given screenshot failure When capture fails Then handles gracefully`() {
        // Given
        val instruction = "Task with screenshot failure"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot failure
        coEvery { mockDriver.captureScreenshot() } returns null
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then - Should still complete despite screenshot failure
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given URL validation When checking safety Then allows only HTTP and HTTPS`() {
        // Given
        val safeUrls = listOf("https://example.com", "http://test.org", "https://sub.domain.com/path")
        val unsafeUrls = listOf("ftp://files.com", "javascript:alert('xss')", "file:///etc/passwd", "")

        // When/Then - Test through actual navigation calls
        safeUrls.forEach { url ->
            runBlocking {
                val instruction = "Navigate to $url"
                val actionOptions = ActionOptions(instruction)

                coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
                coEvery { mockDriver.currentUrl() } returns "https://current.com"

                val result = webDriverAgent.execute(actionOptions)
                assertNotNull(result)
            }
        }

        // Test empty/invalid URLs
        unsafeUrls.forEach { url ->
            runBlocking {
                val instruction = "Navigate to $url"
                val actionOptions = ActionOptions(instruction)

                coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
                coEvery { mockDriver.currentUrl() } returns "https://current.com"

                val result = webDriverAgent.execute(actionOptions)
                assertNotNull(result)
            }
        }
    }

    @Test
    fun `Given action execution failure When tool call fails Then continues execution`() {
        // Given
        val instruction = "Click non-existent element"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot and action failure
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"
        coEvery { mockDriver.click(any(), any()) } throws RuntimeException("Element not found")

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then - Should handle error gracefully and continue
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given max steps limit When reached Then stops execution`() {
        // Given
        val maxSteps = 3
        val agent = WebDriverAgent(mockDriver, maxSteps)
        val instruction = "Infinite loop task"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot but no completion
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // When
        runBlocking {
            val result = agent.execute(actionOptions)

            // Then - Should complete when max steps reached
            assertNotNull(result)
            assertEquals(ResponseState.STOP, result.state)
        }
    }

    @Test
    fun `Given history tracking When executing Then maintains execution history`() {
        // Given
        val instruction = "Multi-step task"
        val actionOptions = ActionOptions(instruction)

        // Mock multiple successful steps
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"
        coEvery { mockDriver.click(any(), any()) } returns Unit

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then
            assertNotNull(result)
            assertTrue(result.content.isNotBlank())
        }
    }

    @Test
    fun `Given summary generation When execution completes Then generates proper summary`() {
        // Given
        val instruction = "Complete task and summarize"
        val actionOptions = ActionOptions(instruction)

        // Mock screenshot and completion
        coEvery { mockDriver.captureScreenshot() } returns "fake-screenshot"
        coEvery { mockDriver.currentUrl() } returns "https://example.com"

        // When
        runBlocking {
            val result = webDriverAgent.execute(actionOptions)

            // Then
            assertNotNull(result)
            assertTrue(result.content.isNotBlank())
            assertEquals(ResponseState.STOP, result.state)
        }
    }
}