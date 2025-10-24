package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.skeleton.ai.support.ToolCall
import org.junit.jupiter.api.Assertions.*
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

    @Test
    fun testParseSimpleFunctionCall_validInput() {
        val input = "driver.open(\"https://t.tt\")"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertEquals(mapOf("0" to "https://t.tt"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_noArguments() {
        val input = "driver.scrollToTop()"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("scrollToTop", result?.name)
        assertTrue(result?.args?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_singleArgument() {
        val input = "driver.scrollToMiddle(0.4)"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("scrollToMiddle", result?.name)
        assertEquals(mapOf("0" to "0.4"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArguments() {
        val input = "driver.mouseWheelUp(2, 200, 200)"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("mouseWheelUp", result?.name)
        assertEquals(mapOf("0" to "2", "1" to "200", "2" to "200"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArgumentsWithSpaces() {
        val input = "driver.mouseWheelUp(2, 200, 200, 100)"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("mouseWheelUp", result?.name)
        assertEquals(mapOf("0" to "2", "1" to "200", "2" to "200", "3" to "100"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_invalidInput() {
        val input = "driver.open(\"https://t.tt"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyInput() {
        val input = ""
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noMethodCall() {
        val input = "driver"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noParentheses() {
        val input = "driver.open"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noObject() {
        val input = ".open(\"https://t.tt\")"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_extraSpaces() {
        val input = "  driver  .  open  (  \"https://t.tt\"  )  "
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertEquals(mapOf("0" to "https://t.tt"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyArguments() {
        val input = "driver.open(   )"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertTrue(result?.args?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_malformedArguments() {
        val input = "driver.open(\"https://t.tt\", )"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertEquals(mapOf("0" to "https://t.tt"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_unquotedStringArgument() {
        val input = "driver.open(https://t.tt)"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertEquals(mapOf("0" to "https://t.tt"), result?.args)
    }

    @Test
    fun testParseSimpleFunctionCall_specialCharactersInArgument() {
        val input = "driver.open(\"https://t.tt?query=123&param=abc\")"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertEquals(mapOf("0" to "https://t.tt?query=123&param=abc"), result?.args)
    }

    // Additional focused coverage
    @Test
    fun `parse input with trailing semicolon`() {
        val input = "driver.open(\"https://t.tt\");"
        val result = ToolCallExecutor.parseKotlinFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.name)
        assertEquals(mapOf("0" to "https://t.tt"), result?.args)
    }

    @Test
    fun `parse double-quoted arg with escapes`() {
        val src = "driver.fill(\"#input\", \"He said \"hi\" and \\path\")"
        val tc = ToolCallExecutor.parseKotlinFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("fill", tc.name)
        assertEquals("#input", tc.args["0"]) // unquoted
        assertEquals("He said \"hi\" and \\path", tc.args["1"]) // unescaped
    }

    @Test
    fun `toolCallToExpression waitForSelector with timeout`() {
        val expr = ToolCallExecutor.toolCallToExpression(
            ToolCall("driver", "waitForSelector", mapOf("selector" to "#a", "timeoutMillis" to 1234))
        )
        assertEquals("driver.waitForSelector(\"#a\", 1234L)", expr)
    }

    @Test
    fun `toolCallToExpression captureScreenshot variants`() {
        val none = ToolCallExecutor.toolCallToExpression(ToolCall("driver", "captureScreenshot", emptyMap()))
        val withSel = ToolCallExecutor.toolCallToExpression(
            ToolCall("driver", "captureScreenshot", mapOf("selector" to "#root"))
        )
        assertEquals("driver.captureScreenshot()", none)
        assertEquals("driver.captureScreenshot(\"#root\")", withSel)
    }

    @Test
    fun `toolCallToExpression scrollToMiddle default`() {
        val expr = ToolCallExecutor.toolCallToExpression(ToolCall("driver", "scrollToMiddle", emptyMap()))
        assertEquals("driver.scrollToMiddle(0.5)", expr)
    }

    @Test
    fun `toolCallToExpression clickTextMatches escaping`() {
        val expr = ToolCallExecutor.toolCallToExpression(
            ToolCall("driver", "clickTextMatches", mapOf("selector" to "a", "pattern" to "He said \"hi\"", "count" to 2))
        )
        assertNotNull(expr)
        assertTrue(expr!!.contains("\\\"hi\\\""))
        assertTrue(expr.startsWith("driver.clickTextMatches(\"a\", \""))
        assertTrue(expr.endsWith(", 2)"))
    }

    @Test
    fun `toolCallToExpression goto alias`() {
        val expr = ToolCallExecutor.toolCallToExpression(
            ToolCall("driver", "goto", mapOf("url" to "https://example.com/x?q=\"y\""))
        )
        assertEquals("driver.navigateTo(\"https://example.com/x?q=\\\"y\\\"\")", expr)
    }

    @Test
    fun `toolCallToExpression press generation`() {
        val expr = ToolCallExecutor.toolCallToExpression(
            ToolCall("driver", "press", mapOf("selector" to "#i", "key" to "Enter"))
        )
        assertEquals("driver.press(\"#i\", \"Enter\")", expr)
    }
}
