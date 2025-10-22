package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.common.config.ImmutableConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextToActionParsingTest {

    private val tta = TextToAction(ImmutableConfig())

    @Test
    fun `parseToolCalls should parse multiple tool calls with mixed argument types`() {
        val json = """
            {"tool_calls":[
              {"name":"click","args":{"selector":"#submit"}},
              {"name":"fill","args":{"selector":"#q","text":"Hello World"}},
              {"name":"scrollDown","args":{"count":3}},
              {"name":"waitForSelector","args":{"selector":"#result","timeoutMillis":8000}},
              {"name":"navigateTo","args":{"url":"https://example.com"}}
            ]}
        """.trimIndent()

        val calls = tta.parseToolCalls(json)
        Assertions.assertEquals(5, calls.size)
        Assertions.assertEquals("click", calls[0].name)
        Assertions.assertEquals("#submit", calls[0].args["selector"])
        Assertions.assertEquals(3, calls[2].args["count"]) // numeric preserved
        Assertions.assertEquals(8000, calls[3].args["timeoutMillis"]) // numeric preserved
    }

    @Test
    fun `toolCallToDriverLine should map tool calls to driver code`() {
        val toolCallsJson = """{"tool_calls":[{"name":"fill","args":{"selector":"#q","text":"Hi"}}]}"""
        val calls = tta.parseToolCalls(toolCallsJson)
        val line = tta.toolCallToDriverLine(calls.first())
        Assertions.assertEquals("driver.fill(\"#q\", \"Hi\")", line)
    }

    @Test
    fun `parseToolCalls invalid json returns empty list`() {
        assertTrue(tta.parseToolCalls("not-json").isEmpty())
        assertTrue(tta.parseToolCalls("{}").isEmpty())
    }
}
