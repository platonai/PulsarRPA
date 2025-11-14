package ai.platon.pulsar.agentic.support

import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolCallExecutorTest {
    private val parser = SimpleKotlinParser()

    @Test
    fun `parse simple call without args`() {
        val tc = parser.parseFunctionExpression("driver.goBack()")
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("goBack", tc.method)
        assertTrue(tc.arguments.isEmpty())
    }

    @Test
    fun `parse call with one arg`() {
        val tc = parser.parseFunctionExpression("driver.scrollToMiddle(0.4)")
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("scrollToMiddle", tc.method)
        assertEquals("0.4", tc.arguments["0"])
    }

    @Test
    fun `parse args with comma inside quotes`() {
        val src = "driver.clickTextMatches(\"a.link\", \"hello, world\", 2)"
        val tc = parser.parseFunctionExpression(src)
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
        val tc = parser.parseFunctionExpression(src)
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
        val tc = parser.parseFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("evaluate", tc.method)
        assertEquals(arg, tc.arguments["0"]) // content preserved
    }

    @Test
    fun `parse trailing comma with one arg`() {
        val src = "driver.click(\"a.link\",)"
        val tc = parser.parseFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("click", tc.method)
        assertEquals(1, tc.arguments.size)
        assertEquals("a.link", tc.arguments["0"]) // single arg despite trailing comma
    }

    @Test
    fun `parse mixed whitespace and trailing comma`() {
        val src = "driver.scrollToMiddle(   0.75   ,   )"
        val tc = parser.parseFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("scrollToMiddle", tc.method)
        assertEquals(1, tc.arguments.size)
        assertEquals("0.75", tc.arguments["0"]) // trimmed numeric string
    }

    @Test
    fun testParseSimpleFunctionCall_validInput() {
        val input = "driver.open(\"https://t.tt\")"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_noArguments() {
        val input = "driver.scrollToTop()"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("scrollToTop", result?.method)
        assertTrue(result?.arguments?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_singleArgument() {
        val input = "driver.scrollToMiddle(0.4)"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("scrollToMiddle", result?.method)
        assertEquals(mutableMapOf("0" to "0.4"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArguments() {
        val input = "driver.mouseWheelUp(2, 200, 200)"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("mouseWheelUp", result?.method)
        assertEquals(mutableMapOf("0" to "2", "1" to "200", "2" to "200"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArgumentsWithSpaces() {
        val input = "driver.mouseWheelUp(2, 200, 200, 100)"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("mouseWheelUp", result?.method)
        assertEquals(mutableMapOf("0" to "2", "1" to "200", "2" to "200", "3" to "100"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_invalidInput() {
        val input = "driver.open(\"https://t.tt"
        val result = parser.parseFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyInput() {
        val input = ""
        val result = parser.parseFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noMethodCall() {
        val input = "driver"
        val result = parser.parseFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noParentheses() {
        val input = "driver.open"
        val result = parser.parseFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noObject() {
        val input = ".open(\"https://t.tt\")"
        val result = parser.parseFunctionExpression(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_extraSpaces() {
        val input = "  driver  .  open  (  \"https://t.tt\"  )  "
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyArguments() {
        val input = "driver.open(   )"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertTrue(result?.arguments?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_malformedArguments() {
        val input = "driver.open(\"https://t.tt\", )"
        val result = parser.parseFunctionExpression(input)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_unquotedStringArgument() {
        val input = "driver.open(https://t.tt)"
        val result = parser.parseFunctionExpression(input)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun testParseSimpleFunctionCall_specialCharactersInArgument() {
        val input = "driver.open(\"https://t.tt?query=123&param=abc\")"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt?query=123&param=abc"), result?.arguments)
    }

    // Additional focused coverage
    @Test
    fun `parse input with trailing semicolon`() {
        val input = "driver.open(\"https://t.tt\");"
        val result = parser.parseFunctionExpression(input)
        assertNotNull(result)
        assertEquals("driver", result?.domain)
        assertEquals("open", result?.method)
        assertEquals(mutableMapOf("0" to "https://t.tt"), result?.arguments)
    }

    @Test
    fun `parse double-quoted arg with escapes`() {
        val src = "driver.fill(\"#input\", \"He said \"hi\" and \\path\")"
        val tc = parser.parseFunctionExpression(src)
        assertNotNull(tc)
        tc!!
        assertEquals("driver", tc.domain)
        assertEquals("fill", tc.method)
        assertEquals("#input", tc.arguments["0"]) // unquoted
        assertEquals("He said \"hi\" and \\path", tc.arguments["1"]) // unescaped
    }
}
