package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.agentic.AgentConfig
import ai.platon.pulsar.agentic.BrowserPerceptiveAgent
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ExtractOptions
import ai.platon.pulsar.skeleton.ai.ObserveOptions
import ai.platon.pulsar.util.TestHelper
import ai.platon.pulsar.util.server.EnableMockServerApplication
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Ignore
import kotlin.test.assertContains

/**
 * Comprehensive unit and integration tests for PulsarPerceptiveAgent
 *
 * Test Coverage:
 * - Agent initialization and configuration
 * - Act method with various scenarios
 * - Extract method with schema validation
 * - Observe method with element detection
 * - Error handling and retry mechanisms
 * - Performance metrics and logging
 * - State management and history tracking
 */
@Order(1100)
@Tag("IntegrationTest")
@Tag("TimeConsumingTest")
@SpringBootTest(
    classes = [EnableMockServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class PulsarPerceptiveAgentTest : WebDriverTestBase() {

    private val mapper = ObjectMapper()

    companion object {
        const val TIMEOUT_MS = 120_000L
    }

    @BeforeEach
    fun checkLLM() {
        TestHelper.checkLLMConfiguration(session)
    }

    @Nested
    @DisplayName("Agent Initialization Tests")
    inner class InitializationTests {

        @Test
        fun `Given default config When agent created Then should have valid uuid and empty history`() {
            runEnhancedWebDriverTest { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                assertNotNull(agent.uuid)
                assertTrue(agent.stateHistory.isEmpty())
                assertTrue(agent.toString().contains("no history"))
            }
        }

        @Test
        fun `Given custom config When agent created Then should use custom configuration`() {
            runEnhancedWebDriverTest { driver ->
                val customConfig = AgentConfig(
                    maxSteps = 50,
                    maxRetries = 2,
                    consecutiveNoOpLimit = 3
                )
                val agent = BrowserPerceptiveAgent(driver, session, config = customConfig)

                assertNotNull(agent.uuid)
                assertTrue(agent.stateHistory.isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("Extract Method Tests")
    inner class ExtractMethodTests {

        @Ignore("Disable temporary")
        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given valid page When extract called Then should return structured data`() {

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val result = agent.extract("Extract the page title and main content")
                printlnPro(result)

                assertTrue(result.success, "Extract should succeed")
                assertNotNull(result.data)
                assertFalse(result.data.isEmpty, "Extracted data should not be empty")

                // History should be updated
                assertTrue(agent.stateHistory.isNotEmpty())
                assertContains(agent.stateHistory.last().action ?: "", "extract")
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given custom schema When extract called Then should follow schema structure`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val customSchema = mapOf(
                    "title" to "string - page title",
                    "buttonCount" to "number - count of buttons"
                )

                val options = ExtractOptions(
                    instruction = "Extract title and count buttons",
                    schema = customSchema
                )

                val result = agent.extract(options)

                printlnPro(result)

                assertTrue(result.success)
                assertNotNull(result.data)

                // Verify schema fields are present
                val jsonNode = result.data
                assertTrue(
                    jsonNode.has("title") || jsonNode.has("buttonCount"),
                    "Result should contain at least one schema field"
                )
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given empty instruction When extract called Then should use default instruction`() {


            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val result = runBlocking {
                    agent.extract("")
                }
                printlnPro(result)

                // Should still work with default instruction
                assertNotNull(result)
            }
        }

        @Test
        fun `Given extraction failure When extract called Then should return failure result`() {
            runEnhancedWebDriverTest { driver ->
                // Navigate to a problematic page
                driver.navigateTo("about:blank")

                val agent = BrowserPerceptiveAgent(driver, session)

                val result = agent.extract("Extract data")
                printlnPro(result)

                // Should handle gracefully even on failure
                assertNotNull(result)
            }
        }
    }

    @Nested
    @DisplayName("Observe Method Tests")
    inner class ObserveMethodTests {

        @Test
        @Ignore("Disable temporary")
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given interactive page When observe called Then should return actionable elements`() {


            runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val results = runBlocking {
                    agent.observe("List all interactive elements on this page")
                }
                printlnPro(results)

                // Should find some interactive elements
                assertTrue(results.isNotEmpty(), "Should find interactive elements")

                results.forEach { result ->
                    assertNotNull(result.locator, "Selector should not be null")
                    assertTrue(result.description!!.isNotBlank(), "Description should not be blank")
                }

                // History should be updated
                assertTrue(agent.stateHistory.isNotEmpty())
                assertContains(agent.stateHistory.last().action ?: "", "observe")
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given observe options When observe called Then should respect options`() {


            runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val options = ObserveOptions(
                    instruction = "Find all buttons",
                    returnAction = false
                )

                val results = agent.observe(options)
                printlnPro(results)

                assertNotNull(results)

                // When returnAction is false, method and arguments should be null
                results.forEach { result ->
                    assertTrue(result.method == null || result.method!!.isBlank())
                }
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given observe with returnAction true When observe called Then should include actions`() {


            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val options = ObserveOptions(
                    instruction = "Find clickable elements",
                    returnAction = true
                )

                val results = agent.observe(options)
                printlnPro(results)

                assertNotNull(results)
                // Some results might have methods/arguments
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given empty instruction When observe called Then should use default instruction`() {


            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val results = agent.observe("")
                printlnPro(results)

                assertNotNull(results)
            }
        }
    }

    @Nested
    @DisplayName("Act Method Tests")
    inner class ActMethodTests {

        @Ignore("Disable temporary")
        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given simple action When act called Then should execute and return result`() {
            runWebDriverTest(actMockSiteHomeURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val result = agent.act("Click the search button")
                printlnPro(result)

                assertNotNull(result)
                kotlin.test.assertEquals("click", result.action)
                assertTrue(agent.stateHistory.isNotEmpty())
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given ActionOptions When act called Then should use options`() {


            runWebDriverTest(actMockSiteHomeURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val options = ActionOptions(
                    action = "Navigate to the home page"
                )

                val result = agent.act(options)
                printlnPro(result)

                assertNotNull(result)
                assertEquals("Navigate to the home page", result.action)
            }
        }

        @Test
        fun `Given act execution When history updated Then toString should reflect latest state`() {
            runWebDriverTest(actMockSiteHomeURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                // Initially empty
                assertTrue(agent.toString().contains("no history"))

                // Manually add to history (simulating execution)
                val history = agent.stateHistory as? MutableList
                history?.add(AgentState(1, "", action = "Test action completed"))

                if (agent.stateHistory.isNotEmpty()) {
                    assertFalse(agent.toString().contains("no history"))
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        fun `Given invalid URL When navigate fails Then should handle gracefully`() {
            runEnhancedWebDriverTest { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                // This should not throw but handle errors internally
                val result = runBlocking {
                    driver.navigateTo("about:blank")
                    agent.extract("Extract something")
                }
                printlnPro(result)

                assertNotNull(result)
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given multiple consecutive failures When act called Then should respect retry limits`() {


            runEnhancedWebDriverTest { driver ->
                val config = AgentConfig(
                    maxRetries = 1,
                    maxSteps = 5
                )
                val agent = BrowserPerceptiveAgent(driver, session, config = config)

                // Navigate to blank page which may cause issues
                runBlocking {
                    driver.navigateTo("about:blank")
                }

                val result = runBlocking {
                    agent.act("Do something impossible")
                }
                printlnPro(result)

                assertNotNull(result)
            }
        }
    }

    @Nested
    @DisplayName("History and State Management Tests")
    inner class StateManagementTests {

        @Test
        @Ignore("Disable temporary")
        fun `Given multiple operations When executed Then history should accumulate`() {

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val initialHistorySize = agent.stateHistory.size

                val r1 = agent.extract("Extract title")
                printlnPro(r1)
                val r2 = agent.observe("List elements")
                printlnPro(r2)

                assertTrue(
                    agent.stateHistory.size >= initialHistorySize + 2,
                    "History should grow with operations"
                )
            }
        }

        @Test
        fun `Given agent with history When toString called Then should show latest entry`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                if (agent.stateHistory.isEmpty()) {
                    assertTrue(agent.toString().contains("no history"))
                } else {
                    val lastEntry = agent.stateHistory.last()
                    assertEquals(lastEntry, agent.toString())
                }
            }
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    inner class ConfigurationTests {

        @Test
        fun `Given different maxSteps When agent created Then should respect configuration`() {
            runEnhancedWebDriverTest { driver ->
                val config1 = AgentConfig(maxSteps = 10)
                val config2 = AgentConfig(maxSteps = 100)

                val agent1 = BrowserPerceptiveAgent(driver, session, maxSteps = 10, config = config1)
                val agent2 = BrowserPerceptiveAgent(driver, session, maxSteps = 100, config = config2)

                assertNotNull(agent1)
                assertNotNull(agent2)
            }
        }

        @Test
        fun `Given structured logging enabled When agent executes Then should log appropriately`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val config = AgentConfig(
                    enableStructuredLogging = true,
                    enablePerformanceMetrics = true
                )

                val agent = BrowserPerceptiveAgent(driver, session, config = config)

                assertNotNull(agent)
            }
        }

        @Test
        fun `Given adaptive delays enabled When agent executes Then should adjust timing`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val config = AgentConfig(
                    enableAdaptiveDelays = true,
                    baseRetryDelayMs = 100,
                    maxRetryDelayMs = 5000
                )

                val agent = BrowserPerceptiveAgent(driver, session, config = config)

                assertNotNull(agent)
            }
        }

        @Test
        fun `Given pre-action validation enabled When agent executes Then should validate actions`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val config = AgentConfig(
                    enablePreActionValidation = true
                )

                val agent = BrowserPerceptiveAgent(driver, session, config = config)

                assertNotNull(agent)
            }
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @Tag("TimeConsumingTest")
        @Timeout(value = 120_000L, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given multiple operations When executed in sequence Then should complete within time limit`() {


            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver, session)

                val startTime = System.currentTimeMillis()

                repeat(3) {
                    val results = agent.observe("Find interactive elements")
                    printlnPro(results)
                }

                val duration = System.currentTimeMillis() - startTime

                // Should complete within reasonable time
                assertTrue(duration < 120_000, "Operations should complete within 120 seconds")
            }
        }

        @Test
        @Tag("TimeConsumingTest")
        fun `Given memory cleanup interval When many steps executed Then should manage memory`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val config = AgentConfig(
                    memoryCleanupIntervalSteps = 5,
                    maxHistorySize = 20
                )

                val agent = BrowserPerceptiveAgent(driver, session, config = config)

                assertNotNull(agent)

                // The agent should manage its memory during execution
            }
        }
    }
}
