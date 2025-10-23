package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private class TestableTextToAction(conf: ImmutableConfig): TextToAction(conf) {
    fun parse(response: ModelResponse): ActionDescription = modelResponseToActionDescription(response, 1)
}

class TextToActionParsingTest {

    private val tta = TestableTextToAction(ImmutableConfig())

    @Test
    fun `elements JSON with click builds ToolCall with selector as arg0`() {
        val json = """
            {
              "elements": [
                {
                  "locator": "#submit",
                  "description": "Submit button",
                  "method": "click",
                  "arguments": []
                }
              ]
            }
        """.trimIndent()
        val resp = ModelResponse(json, ResponseState.STOP)

        val ad = tta.parse(resp)
        assertNotNull(ad.toolCall)
        assertEquals("driver", ad.toolCall!!.domain)
        assertEquals("click", ad.toolCall!!.name)
        assertEquals("#submit", ad.toolCall!!.args["selector"])
        // Optional: expression rendering should match
        assertTrue(ad.cssFriendlyExpressions.firstOrNull()?.startsWith("driver.click(") == true)
        assertFalse(ad.isComplete)
        assertNull(ad.summary)
        assertTrue(ad.suggestions.isEmpty())
    }

    @Test
    fun `elements JSON with type maps locator to arg0 and text to arg1`() {
        val json = """
            {
              "elements": [
                {
                  "locator": "#q",
                  "description": "Search input",
                  "method": "type",
                  "arguments": [ { "name": "text", "value": "hello" } ]
                }
              ]
            }
        """.trimIndent()
        val resp = ModelResponse(json, ResponseState.STOP)

        val ad = tta.parse(resp)
        val tc = requireNotNull(ad.toolCall)
        assertEquals("driver", tc.domain)
        assertEquals("type", tc.name)
        assertEquals("#q", tc.args["selector"])
        assertEquals("hello", tc.args["text"]) // named mapping for argument values

        assertFalse(ad.isComplete)
    }

    @Test
    fun `completion JSON sets isComplete summary and suggestions`() {
        val json = """
            {
              "isComplete": true,
              "summary": "Done searching",
              "suggestions": ["Refine query", "Open first result"]
            }
        """.trimIndent()
        val resp = ModelResponse(json, ResponseState.STOP)

        val ad = tta.parse(resp)
        assertNull(ad.toolCall)
        assertTrue(ad.cssFriendlyExpressions.isEmpty())
        assertTrue(ad.isComplete)
        assertEquals("Done searching", ad.summary)
        assertEquals(listOf("Refine query", "Open first result"), ad.suggestions)
    }

    @Test
    fun `plain driver expression fallback is preserved`() {
        val text = """
            driver.click("#ok")
            // some commentary from model
            driver.scrollToMiddle(0.5)
        """.trimIndent()
        val resp = ModelResponse(text, ResponseState.STOP)

        val ad = tta.parse(resp)
        // Should extract driver.* lines only
        assertTrue(ad.toolCall == null || ad.cssFriendlyExpressions.isNotEmpty())
        assertTrue(ad.cssFriendlyExpressions.any { it.startsWith("driver.click(") })
        assertTrue(ad.cssFriendlyExpressions.any { it.startsWith("driver.scrollToMiddle(") })
        assertFalse(ad.isComplete)
    }
}
