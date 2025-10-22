package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.common.config.ImmutableConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Deprecated: tool_calls is no longer supported; tests will be rewritten")
class TextToActionParsingTest {

    private val tta = TextToAction(ImmutableConfig())

    @Test
    fun `parseToolCalls should parse multiple tool calls with mixed argument types`() {
        // Deprecated: tool_calls parsing removed
    }

    @Test
    fun `toolCallToDriverLine should map tool calls to driver code`() {
        // Deprecated: tool_calls parsing removed
    }

    @Test
    fun `parseToolCalls invalid json returns empty list`() {
        // Deprecated: tool_calls parsing removed
        // assertTrue(tta.parseToolCalls("not-json").isEmpty())
        // assertTrue(tta.parseToolCalls("{}").isEmpty())
        assertTrue(true)
    }
}
