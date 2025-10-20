package ai.platon.pulsar.skeleton.crawl.fetch.driver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToolCallExecutorTest {

    @Test
    fun `parse simple call without args`() {
        val tc = ToolCallExecutor.parseKotlinFunctionExpression("driver.goBack()")
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("goBack", tc.name)
        assertTrue(tc.args.isEmpty())
    }

    @Test
    fun `parse call with one arg`() {
        val tc = ToolCallExecutor.parseKotlinFunctionExpression("driver.scrollToMiddle(0.4)")
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("scrollToMiddle", tc.name)
        assertEquals("0.4", tc.args["0"])
    }

    @Test
    fun `generate expression escapes quotes and backslashes`() {
        val url = "https://example.com?q=\"a b\"\\tail"
        val expr = ToolCallExecutor.toolCallToExpression(
            ToolCall("driver", "navigateTo", mapOf("url" to url))
        )
        assertNotNull(expr)
        // Expect the embedded string to have escaped quotes and backslashes
        assertTrue(expr!!.contains("\\\"a b\\\""))
        assertTrue(expr.contains("\\\\tail"))
    }

    @Test
    fun `generate expression for goBack and goForward`() {
        val back = ToolCallExecutor.toolCallToExpression(ToolCall("driver", "goBack", emptyMap()))
        val forward = ToolCallExecutor.toolCallToExpression(ToolCall("driver", "goForward", emptyMap()))
        assertEquals("driver.goBack()", back)
        assertEquals("driver.goForward()", forward)
    }

    @Test
    fun `generate expression for clickMatches with escaping`() {
        val tc = ToolCall(
            "driver",
            "clickMatches",
            mapOf(
                "selector" to "a.link",
                "attrName" to "data-title",
                "pattern" to "He said \"hi\"",
                "count" to 2
            )
        )
        val expr = ToolCallExecutor.toolCallToExpression(tc)
        assertNotNull(expr)
        assertTrue(expr!!.contains("\\\"hi\\\""))
        assertTrue(expr.contains("driver.clickMatches(\"a.link\", \"data-title\","))
        assertTrue(expr.endsWith(", 2)"))
    }

    @Test
    fun `parse args with comma inside quotes`() {
        val src = "driver.clickTextMatches(\"a.link\", \"hello, world\", 2)"
        val tc = ToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("clickTextMatches", tc.name)
        assertEquals("a.link", tc.args["0"]) // first arg
        assertEquals("hello, world", tc.args["1"]) // quoted comma preserved
        assertEquals("2", tc.args["2"]) // third arg
    }

    @Test
    fun `parse single-quoted arg with escapes`() {
        val src = "driver.fill('#input', 'He said \\\'hi\\\' and \\\\path')"
        val tc = ToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("fill", tc.name)
        assertEquals("#input", tc.args["0"]) // unquoted
        assertEquals("He said 'hi' and \\path", tc.args["1"]) // unescaped
    }

    @Test
    fun `parse nested parentheses and comma inside string`() {
        val arg = "(function(){ return (1,(2+3)); })()"
        val src = "driver.evaluate(\"$arg\")"
        val tc = ToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("evaluate", tc.name)
        assertEquals(arg, tc.args["0"]) // content preserved
    }

    @Test
    fun `parse trailing comma with one arg`() {
        val src = "driver.click(\"a.link\",)"
        val tc = ToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("click", tc.name)
        assertEquals(1, tc.args.size)
        assertEquals("a.link", tc.args["0"]) // single arg despite trailing comma
    }

    @Test
    fun `parse mixed whitespace and trailing comma`() {
        val src = "driver.scrollToMiddle(   0.75   ,   )"
        val tc = ToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("scrollToMiddle", tc.name)
        assertEquals(1, tc.args.size)
        assertEquals("0.75", tc.args["0"]) // trimmed numeric string
    }
}
