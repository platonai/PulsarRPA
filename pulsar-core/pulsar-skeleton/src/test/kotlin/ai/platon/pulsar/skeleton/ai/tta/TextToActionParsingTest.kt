package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.config.ImmutableConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TextToActionParsingTest {

    private val tta = TextToAction(ImmutableConfig.DEFAULT)

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
        assertEquals(5, calls.size)
        assertEquals("click", calls[0].name)
        assertEquals("#submit", calls[0].args["selector"])
        assertEquals(3, calls[2].args["count"]) // numeric preserved
        assertEquals(8000, calls[3].args["timeoutMillis"]) // numeric preserved
    }

    @Test
    fun `toolCallToDriverLine should map tool calls to driver code`() {
        val toolCallsJson = """{"tool_calls":[{"name":"fill","args":{"selector":"#q","text":"Hi"}}]}"""
        val calls = tta.parseToolCalls(toolCallsJson)
        val line = tta.toolCallToDriverLine(calls.first())
        assertEquals("driver.fill(\"#q\", \"Hi\")", line)
    }

    @Test
    fun `parseToolCalls invalid json returns empty list`() {
        assertTrue(tta.parseToolCalls("not-json").isEmpty())
        assertTrue(tta.parseToolCalls("{}").isEmpty())
    }

    @Test
    fun `parseElementsFromJsonString should parse valid elements array`() {
        val json = """[
          {"id":"a1","tagName":"BUTTON","selector":"#a1","text":"Click Me","isVisible":true,
           "bounds":{"x":10,"y":20,"width":100,"height":40}}
        ]""".trimIndent()
        val elements = tta.parseElementsFromJsonString(json)
        assertEquals(1, elements.size)
        val e = elements.first()
        assertEquals("a1", e.id)
        assertEquals("#a1", e.selector)
        assertTrue(e.isVisible)
        assertEquals(100.0, e.bounds.width)
    }

    @Test
    fun `parseElementsFromJsonString should handle wrapper object with elements field`() {
        val json = """{"elements":[{"id":"x","tagName":"DIV","selector":"#x","text":"Some Text",
            "isVisible":false,"bounds":{"x":0,"y":0,"width":1,"height":1}}]}"""
        val elements = tta.parseElementsFromJsonString(json)
        assertEquals(1, elements.size)
        assertEquals("x", elements.first().id)
    }

    @Test
    fun `element parsing returns empty list for invalid json`() {
        assertTrue(tta.parseElementsFromJsonString("not-json").isEmpty())
    }

    @Test
    fun `extractSelector variants`() {
        assertEquals("#id1", tta.extractSelector("driver.click(\"#id1\")"))
        assertEquals(".class-x", tta.extractSelector("driver.click(\".class-x\")"))
    }

    @Test
    fun `extractSelectorAndText from fill`() {
        val (sel, text) = tta.extractSelectorAndText("driver.fill(\"#q\", \"Hello\")")
        assertEquals("#q", sel)
        assertEquals("Hello", text)
    }

    @Test
    fun `extractRatio parses numeric`() {
        assertEquals(0.75, tta.extractRatio("driver.scrollToMiddle(0.75)"))
    }

    @Test
    fun `extractCount parses integer`() {
        assertEquals(5, tta.extractCount("driver.scrollDown(5)"))
    }

    @Test
    fun `extractSelectorAndTimeout parses both`() {
        val (sel, timeout) = tta.extractSelectorAndTimeout("driver.waitForSelector(\"#res\", 7000L)")
        assertEquals("#res", sel)
        assertEquals(7000L, timeout)
    }

    @Test
    fun `extractUrl parses url`() {
        assertEquals("https://example.com", tta.extractUrl("driver.navigateTo(\"https://example.com\")"))
    }
}

