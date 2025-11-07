package ai.platon.pulsar.agentic.support

import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolCallExecutorTest {

    @Test
    fun `parse simple call without args`() {
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression("driver.goBack()")
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("goBack", tc.method)
        assertTrue(tc.arguments.isEmpty())
    }

    @Test
    fun `parse call with one arg`() {
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression("driver.scrollToMiddle(0.4)")
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("scrollToMiddle", tc.method)
        assertEquals("0.4", tc.arguments["0"])
    }

    @Test
    fun `generate expression escapes quotes and backslashes`() {
        val url = "https://example.com?q=\"a b\"\\tail"
        val expr = BasicToolCallExecutor.toExpression(
            ToolCall("driver", "navigateTo", mutableMapOf("url" to url))
        )
        assertNotNull(expr)
        // Expect the embedded string to have escaped quotes and backslashes
        assertTrue(expr!!.contains("\\\"a b\\\""))
        assertTrue(expr.contains("\\\\tail"))
    }

    @Test
    fun `generate expression for goBack and goForward`() {
        val back = BasicToolCallExecutor.toExpression(ToolCall("driver", "goBack", mutableMapOf()))
        val forward = BasicToolCallExecutor.toExpression(ToolCall("driver", "goForward", mutableMapOf()))
        assertEquals("driver.goBack()", back)
        assertEquals("driver.goForward()", forward)
    }

    @Test
    fun `generate expression for clickMatches with escaping`() {
        val tc = ToolCall(
            "driver",
            "clickMatches",
            mutableMapOf(
                "selector" to "a.link",
                "attrName" to "data-title",
                "pattern" to "He said \"hi\"",
                "count" to "2"
            )
        )
        val expr = BasicToolCallExecutor.toExpression(tc)
        assertNotNull(expr)
        assertTrue(expr!!.contains("\\\"hi\\\""))
        assertTrue(expr.contains("driver.clickMatches(\"a.link\", \"data-title\","))
        assertTrue(expr.endsWith(", 2)"))
    }

    @Test
    fun `parse args with comma inside quotes`() {
        val src = "driver.clickTextMatches(\"a.link\", \"hello, world\", 2)"
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("clickTextMatches", tc.method)
        assertEquals("a.link", tc.arguments["0"]) // first arg
        assertEquals("hello, world", tc.arguments["1"]) // quoted comma preserved
        assertEquals("2", tc.arguments["2"]) // third arg
    }

    @Test
    fun `parse single-quoted arg with escapes`() {
        val src = "driver.fill('#input', 'He said \\\'hi\\\' and \\\\path')"
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("fill", tc.method)
        assertEquals("#input", tc.arguments["0"]) // unquoted
        assertEquals("He said 'hi' and \\path", tc.arguments["1"]) // unescaped
    }

    @Test
    fun `parse nested parentheses and comma inside string`() {
        val arg = "(function(){ return (1,(2+3)); })()"
        val src = "driver.evaluate(\"$arg\")"
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("evaluate", tc.method)
        assertEquals(arg, tc.arguments["0"]) // content preserved
    }

    @Test
    fun `parse trailing comma with one arg`() {
        val src = "driver.click(\"a.link\",)"
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("click", tc.method)
        assertEquals(1, tc.arguments.size)
        assertEquals("a.link", tc.arguments["0"]) // single arg despite trailing comma
    }

    @Test
    fun `parse mixed whitespace and trailing comma`() {
        val src = "driver.scrollToMiddle(   0.75   ,   )"
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("scrollToMiddle", tc.method)
        assertEquals(1, tc.arguments.size)
        assertEquals("0.75", tc.arguments["0"]) // trimmed numeric string
    }

    @Test
    fun testParseSimpleFunctionCall_validInput() {
        val input = "driver.open(\"https://t.tt\")"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_noArguments() {
        val input = "driver.scrollToTop()"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("scrollToTop", result?.method)
        assertTrue(result?.arguments?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_singleArgument() {
        val input = "driver.scrollToMiddle(0.4)"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("scrollToMiddle", result?.method)
        assertEquals(mutableMapOf("0" to "0.4"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArguments() {
        val input = "driver.mouseWheelUp(2, 200, 200)"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("mouseWheelUp", result?.method)
        assertEquals(mutableMapOf("0" to "2", "1" to "200", "2" to "200"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArgumentsWithSpaces() {
        val input = "driver.mouseWheelUp(2, 200, 200, 100)"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("mouseWheelUp", result?.method)
        assertEquals(mutableMapOf("0" to "2", "1" to "200", "2" to "200", "3" to "100"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_invalidInput() {
        val input = "driver.open(\"https://t.tt"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyInput() {
        val input = ""
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noMethodCall() {
        val input = "driver"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noParentheses() {
        val input = "driver.open"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noObject() {
        val input = ".open(\"https://t.tt\")"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_extraSpaces() {
        val input = "  driver  .  open  (  \"https://t.tt\"  )  "
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyArguments() {
        val input = "driver.open(   )"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertTrue(result?.arguments?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_malformedArguments() {
        val input = "driver.open(\"https://t.tt\", )"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_unquotedStringArgument() {
        val input = "driver.open(https://t.tt)"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_specialCharactersInArgument() {
        val input = "driver.open(\"https://t.tt?query=123&param=abc\")"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt?query=123&param=abc"), result?.arguments)
    }

    // Additional focused coverage
    @Test
    fun `parse input with trailing semicolon`() {
        val input = "driver.open(\"https://t.tt\");"
        val result = BasicToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun `parse double-quoted arg with escapes`() {
        val src = "driver.fill(\"#input\", \"He said \"hi\" and \\path\")"
        val tc = BasicToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("fill", tc.method)
        assertEquals("#input", tc.arguments["0"]) // unquoted
        assertEquals("He said \"hi\" and \\path", tc.arguments["1"]) // unescaped
    }

    @Test
    fun `toolCallToExpression waitForSelector with timeout`() {
        val expr = BasicToolCallExecutor.toExpression(
            ToolCall("driver", "waitForSelector", mutableMapOf("selector" to "#a", "timeoutMillis" to "1234"))
        )
        assertEquals("driver.waitForSelector(\"#a\", 1234)", expr)
    }

    @Test
    fun `toolCallToExpression captureScreenshot variants`() {
        val none = BasicToolCallExecutor.toExpression(ToolCall("driver", "captureScreenshot", mutableMapOf()))
        val withSel = BasicToolCallExecutor.toExpression(
            ToolCall("driver", "captureScreenshot", mutableMapOf("selector" to "#root"))
        )
        assertEquals("driver.captureScreenshot()", none)
        assertEquals("driver.captureScreenshot(\"#root\")", withSel)
    }

    @Test
    fun `toolCallToExpression scrollToMiddle default`() {
        val expr = BasicToolCallExecutor.toExpression(ToolCall("driver", "scrollToMiddle", mutableMapOf()))
        assertEquals("driver.scrollToMiddle(0.5)", expr)
    }

    @Test
    fun `toolCallToExpression clickTextMatches escaping`() {
        val expr = BasicToolCallExecutor.toExpression(
            ToolCall(
                "driver",
                "clickTextMatches",
                mutableMapOf("selector" to "a", "pattern" to "He said \"hi\"", "count" to "2")
            )
        )
        assertNotNull(expr)
        assertTrue(expr!!.contains("\\\"hi\\\""))
        assertTrue(expr.startsWith("driver.clickTextMatches(\"a\", \""))
        assertTrue(expr.endsWith(", 2)"))
    }

    @Test
    fun `toolCallToExpression press generation`() {
        val expr = BasicToolCallExecutor.toExpression(
            ToolCall("driver", "press", mutableMapOf("selector" to "#i", "key" to "Enter"))
        )
        assertEquals("driver.press(\"#i\", \"Enter\")", expr)
    }
}
