package ai.platon.pulsar.skeleton.crawl.fetch.driver

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class SimpleCommandDispatcherTest {

    @Test
    fun testParseSimpleFunctionCall_validInput() {
        val input = "driver.open(\"https://t.tt\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://t.tt"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_noArguments() {
        val input = "driver.scrollToTop()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("scrollToTop", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_singleArgument() {
        val input = "driver.scrollToMiddle(0.4)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("scrollToMiddle", result?.second)
        assertEquals(listOf("0.4"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArguments() {
        val input = "driver.mouseWheelUp(2, 200, 200)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("mouseWheelUp", result?.second)
        assertEquals(listOf("2", "200", "200"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_multipleArgumentsWithSpaces() {
        val input = "driver.mouseWheelUp(2, 200, 200, 100)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("mouseWheelUp", result?.second)
        assertEquals(listOf("2", "200", "200", "100"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_invalidInput() {
        val input = "driver.open(\"https://t.tt"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyInput() {
        val input = ""
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noMethodCall() {
        val input = "driver"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noParentheses() {
        val input = "driver.open"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNull(result)
    }

    @Test
    fun testParseSimpleFunctionCall_noObject() {
        val input = ".open(\"https://t.tt\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNull(result)
    }

    @Ignore("Not supported currently")
    @Test
    fun testParseSimpleFunctionCall_extraSpaces() {
        val input = "  driver  .  open  (  \"https://t.tt\"  )  "
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://t.tt"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_emptyArguments() {
        val input = "driver.open(   )"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_malformedArguments() {
        val input = "driver.open(\"https://t.tt\", )"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://t.tt"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_unquotedStringArgument() {
        val input = "driver.open(https://t.tt)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://t.tt"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_specialCharactersInArgument() {
        val input = "driver.open(\"https://t.tt?query=123&param=abc\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://t.tt?query=123&param=abc"), result?.third)
    }
}
