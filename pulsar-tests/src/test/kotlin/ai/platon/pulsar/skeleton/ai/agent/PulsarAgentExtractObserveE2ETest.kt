package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.agentic.ai.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.TestHelper
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.ChatModelFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

@Tag("E2ETest")
class PulsarAgentExtractObserveE2ETest : WebDriverTestBase() {
    private val testURL get() = interactiveDynamicURL

    private data class Metrics(
        val url: String,
        val timestamp: String,
        val case: String,
        val observedCount: Int? = null,
        val extractSuccess: Boolean? = null,
        val extractJsonSize: Int? = null,
        val notes: String? = null,
        val differenceType: String = "meta"
    )

    private fun writeMetrics(metrics: Metrics) {
        val dir = File("logs/chat-model")
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "domservice-e2e.json")
        val mapper = prettyPulsarObjectMapper()
        out.appendText(mapper.writeValueAsString(metrics) + System.lineSeparator())
    }

    @BeforeEach
    fun checkLLM() {
        TestHelper.checkLLMConfiguration(session)
    }

    @Test
    fun `Given interactive page When observe Then get actionable elements`() = runEnhancedWebDriverTest(testURL) { driver ->
        val agent = BrowserPerceptiveAgent(driver, session)
        val observed = agent.observe("Understand the page and list actionable elements")

        Assertions.assertTrue(observed.isNotEmpty(), "Observed elements should not be empty")
        Assertions.assertTrue(
            observed.all { it.description!!.isNotBlank() },
            "Each observed item should have a non-blank description"
        )

        writeMetrics(
            Metrics(
                url = testURL,
                timestamp = Instant.now().toString(),
                case = "PulsarAgentObserveE2E",
                observedCount = observed.size,
                notes = "Basic observe path"
            )
        )
    }

    @Test
    fun `Given interactive page When extract Then get structured data`() = runEnhancedWebDriverTest(testURL) { driver ->
        val agent = BrowserPerceptiveAgent(driver, session)
        val result = agent.extract("Extract key structured data from the page")

        assertTrue(result.success, "Extract should succeed")
        val jsonText = result.data.toString()
        assertTrue(jsonText.isNotBlank(), "Extracted data should be non-empty JSON")

        writeMetrics(
            Metrics(
                url = testURL,
                timestamp = Instant.now().toString(),
                case = "PulsarAgentExtractE2E",
                extractSuccess = result.success,
                extractJsonSize = jsonText.length,
                notes = "Basic extract path"
            )
        )
    }

    @Test
    fun `test element bounds calculation with positioned elements`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)
        val agent = BrowserPerceptiveAgent(driver, session)

        // Test bounds-related functionality
        val prompt = "分析页面中元素的定位和尺寸信息"
        val result = agent.observe(prompt)
        printlnPro(result)
    }

    @Test
    fun `test element visibility detection with interactive elements`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)
        val agent = BrowserPerceptiveAgent(driver, session)

        // Test visibility-related functionality
        val prompt = "分析页面中元素的可见性状态"
        val result = agent.observe(prompt)
        printlnPro(result)
    }

}
