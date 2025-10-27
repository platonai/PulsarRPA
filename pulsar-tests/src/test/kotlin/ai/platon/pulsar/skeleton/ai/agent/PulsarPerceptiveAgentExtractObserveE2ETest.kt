package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.agentic.ai.BrowserPerceptiveAgent
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.ChatModelFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

/**
 * E2E tests for observe/extract with metrics logged to logs/chat-model/domservice-e2e.json
 */
@Tag("E2ETest")
class PulsarPerceptiveAgentExtractObserveE2ETest : WebDriverTestBase() {
    private val testURL get() = interactiveDynamicURL

    private data class Metrics(
        val url: String,
        val timestamp: String,
        val case: String,
        val observedCount: Int? = null,
        val extractSuccess: Boolean? = null,
        val extractJsonSize: Int? = null,
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
    fun `Given interactive page When observe Then actionable elements returned`() = runEnhancedWebDriverTest(testURL) { driver ->
        assumeTrue(ChatModelFactory.hasModel(conf), "LLM not configured; skipping E2E")

        val agent = BrowserPerceptiveAgent(driver, session)
        val observed = agent.observe("Understand the page and list actionable elements")

        assertTrue(observed.isNotEmpty())
        assertTrue(observed.all { it.description!!.isNotBlank() })

        writeMetrics(
            Metrics(
                url = testURL,
                timestamp = Instant.now().toString(),
                case = "PulsarPerceptiveAgentObserveE2E",
                observedCount = observed.size
            )
        )
    }

    @Test
    fun `Given interactive page When extract Then structured data returned`() = runEnhancedWebDriverTest(testURL) { driver ->
        assumeTrue(ChatModelFactory.hasModel(conf), "LLM not configured; skipping E2E")

        val agent = BrowserPerceptiveAgent(driver, session)
        val result = agent.extract("Extract key structured data from the page")

        assertTrue(result.success)
        val jsonText = result.data.toString()
        assertTrue(jsonText.isNotBlank())

        writeMetrics(
            Metrics(
                url = testURL,
                timestamp = Instant.now().toString(),
                case = "PulsarPerceptiveAgentExtractE2E",
                extractSuccess = result.success,
                extractJsonSize = jsonText.length
            )
        )
    }
}
