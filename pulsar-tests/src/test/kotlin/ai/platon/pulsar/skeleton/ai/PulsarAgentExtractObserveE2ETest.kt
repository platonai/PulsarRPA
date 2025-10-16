package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.skeleton.ai.agent.PulsarPerceptiveAgent
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.ChatModelFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

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

    @Test
    fun `Given interactive page When observe Then get actionable elements`() = runEnhancedWebDriverTest(testURL) { driver ->
        assumeTrue(ChatModelFactory.hasModel(conf), "LLM not configured; skipping observe E2E test")

        val agent = PulsarPerceptiveAgent(driver)
        val observed = agent.observe("Understand the page and list actionable elements")

        assertTrue(observed.isNotEmpty(), "Observed elements should not be empty")
        assertTrue(observed.all { it.description.isNotBlank() }, "Each observed item should have a non-blank description")

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
        assumeTrue(ChatModelFactory.hasModel(conf), "LLM not configured; skipping extract E2E test")

        val agent = PulsarPerceptiveAgent(driver)
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
}
