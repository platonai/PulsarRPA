package ai.platon.pulsar.skeleton.crawl.fetch.driver

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class ToolCallExecutor1Test {

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

    @Ignore("Not supported currently")
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
}
