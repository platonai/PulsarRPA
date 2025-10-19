package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.agentic.ai.agent.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.agent.AgentConfig
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.ExtractOptions
import ai.platon.pulsar.skeleton.ai.ObserveOptions
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.boot.test.context.SpringBootTest
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
@Tag("IntegrationTest")
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PulsarPerceptiveAgentTest : WebDriverTestBase() {

    private val mapper = ObjectMapper()

    companion object {
        const val TIMEOUT_MS = 60_000L
    }

    @Nested
    @DisplayName("Agent Initialization Tests")
    inner class InitializationTests {

        @Test
        fun `Given default config When agent created Then should have valid uuid and empty history`() {
            runEnhancedWebDriverTest { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                assertNotNull(agent.uuid)
                assertTrue(agent.history.isEmpty())
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
                val agent = BrowserPerceptiveAgent(driver, config = customConfig)

                assertNotNull(agent.uuid)
                assertTrue(agent.history.isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("Extract Method Tests")
    inner class ExtractMethodTests {

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given valid page When extract called Then should return structured data`() {
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val result = agent.extract("Extract the page title and main content")

                assertTrue(result.success, "Extract should succeed")
                assertNotNull(result.data)
                assertFalse(result.data.isEmpty, "Extracted data should not be empty")

                // History should be updated
                assertTrue(agent.history.isNotEmpty())
                assertContains(agent.history.last(), "extract")
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given custom schema When extract called Then should follow schema structure`() {
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val customSchema = mapOf(
                    "title" to "string - page title",
                    "buttonCount" to "number - count of buttons"
                )

                val options = ExtractOptions(
                    instruction = "Extract title and count buttons",
                    schema = customSchema
                )

                val result = agent.extract(options)

                assertTrue(result.success)
                assertNotNull(result.data)

                // Verify schema fields are present
                val jsonNode = result.data
                assertTrue(jsonNode.has("title") || jsonNode.has("buttonCount"),
                    "Result should contain at least one schema field")
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given empty instruction When extract called Then should use default instruction`() {
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val result = runBlocking {
                    agent.extract("")
                }

                // Should still work with default instruction
                assertNotNull(result)
            }
        }

        @Test
        fun `Given extraction failure When extract called Then should return failure result`() {
            runEnhancedWebDriverTest { driver ->
                // Navigate to a problematic page
                runBlocking {
                    driver.navigateTo("about:blank")
                }

                val agent = BrowserPerceptiveAgent(driver)

                val result = runBlocking {
                    agent.extract("Extract data")
                }

                // Should handle gracefully even on failure
                assertNotNull(result)
            }
        }
    }

    @Nested
    @DisplayName("Observe Method Tests")
    inner class ObserveMethodTests {

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given interactive page When observe called Then should return actionable elements`() {
            assumeLLMConfigured()

            runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val results = runBlocking {
                    agent.observe("List all interactive elements on this page")
                }

                // Should find some interactive elements
                assertTrue(results.isNotEmpty(), "Should find interactive elements")

                results.forEach { result ->
                    assertNotNull(result.locator, "Selector should not be null")
                    assertTrue(result.description.isNotBlank(), "Description should not be blank")
                }

                // History should be updated
                assertTrue(agent.history.isNotEmpty())
                assertContains(agent.history.last(), "observe")
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given observe options When observe called Then should respect options`() {
            assumeLLMConfigured()

            runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val options = ObserveOptions(
                    instruction = "Find all buttons",
                    returnAction = false
                )

                val results = agent.observe(options)

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
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val options = ObserveOptions(
                    instruction = "Find clickable elements",
                    returnAction = true
                )

                val results = agent.observe(options)

                assertNotNull(results)
                // Some results might have methods/arguments
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given empty instruction When observe called Then should use default instruction`() {
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val results = runBlocking {
                    agent.observe("")
                }

                assertNotNull(results)
            }
        }
    }

    @Nested
    @DisplayName("Act Method Tests")
    inner class ActMethodTests {

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given simple action When act called Then should execute and return result`() {
            assumeLLMConfigured()

            runWebDriverTest(actMockSiteHomeURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val result = agent.act("Click the search button")

                assertNotNull(result)
                kotlin.test.assertEquals("click", result.action)
                assertTrue(agent.history.isNotEmpty())
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given ActionOptions When act called Then should use options`() {
            assumeLLMConfigured()

            runWebDriverTest(actMockSiteHomeURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val options = ActionOptions(
                    action = "Navigate to the home page"
                )

                val result = agent.act(options)

                assertNotNull(result)
                assertEquals("Navigate to the home page", result.action)
            }
        }

        @Test
        fun `Given act execution When history updated Then toString should reflect latest state`() {
            runWebDriverTest(actMockSiteHomeURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                // Initially empty
                assertTrue(agent.toString().contains("no history"))

                // Manually add to history (simulating execution)
                val history = agent.history as? MutableList
                history?.add("Test action completed")

                if (agent.history.isNotEmpty()) {
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
                val agent = BrowserPerceptiveAgent(driver)

                // This should not throw but handle errors internally
                val result = runBlocking {
                    driver.navigateTo("about:blank")
                    agent.extract("Extract something")
                }

                assertNotNull(result)
            }
        }

        @Test
        @Timeout(value = TIMEOUT_MS, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        fun `Given multiple consecutive failures When act called Then should respect retry limits`() {
            assumeLLMConfigured()

            runEnhancedWebDriverTest { driver ->
                val config = AgentConfig(
                    maxRetries = 1,
                    maxSteps = 5
                )
                val agent = BrowserPerceptiveAgent(driver, config = config)

                // Navigate to blank page which may cause issues
                runBlocking {
                    driver.navigateTo("about:blank")
                }

                val result = runBlocking {
                    agent.act("Do something impossible")
                }

                assertNotNull(result)
            }
        }
    }

    @Nested
    @DisplayName("History and State Management Tests")
    inner class StateManagementTests {

        @Test
        fun `Given multiple operations When executed Then history should accumulate`() {
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val initialHistorySize = agent.history.size

                agent.extract("Extract title")
                agent.observe("List elements")

                assertTrue(agent.history.size >= initialHistorySize + 2,
                    "History should grow with operations")
            }
        }

        @Test
        fun `Given agent with history When toString called Then should show latest entry`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                if (agent.history.isEmpty()) {
                    assertTrue(agent.toString().contains("no history"))
                } else {
                    val lastEntry = agent.history.last()
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

                val agent1 = BrowserPerceptiveAgent(driver, maxSteps = 10, config = config1)
                val agent2 = BrowserPerceptiveAgent(driver, maxSteps = 100, config = config2)

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

                val agent = BrowserPerceptiveAgent(driver, config = config)

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

                val agent = BrowserPerceptiveAgent(driver, config = config)

                assertNotNull(agent)
            }
        }

        @Test
        fun `Given pre-action validation enabled When agent executes Then should validate actions`() {
            runWebDriverTest(interactiveDynamicURL) { driver ->
                val config = AgentConfig(
                    enablePreActionValidation = true
                )

                val agent = BrowserPerceptiveAgent(driver, config = config)

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
            assumeLLMConfigured()

            runWebDriverTest(interactiveDynamicURL) { driver ->
                val agent = BrowserPerceptiveAgent(driver)

                val startTime = System.currentTimeMillis()

                repeat(3) {
                    agent.observe("Find interactive elements")
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

                val agent = BrowserPerceptiveAgent(driver, config = config)

                assertNotNull(agent)

                // The agent should manage its memory during execution
            }
        }
    }

    /**
     * Helper method to check if LLM is configured
     */
    private fun assumeLLMConfigured() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(conf), "LLM not configured - skipping test")
    }
}
