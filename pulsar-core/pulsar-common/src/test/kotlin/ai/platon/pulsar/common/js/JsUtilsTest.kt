package ai.platon.pulsar.common.js

import ai.platon.pulsar.common.js.JsUtils.toIIFEOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class JsUtilsTest {

    @Test
    fun `test normal function expression`() {
        val input = "function() { console.log('hello'); }"
        val expected = "(function() { console.log('hello'); })();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test function with leading and trailing spaces`() {
        val input = "   function() {}   "
        val expected = "(function() {})();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test function with semicolons`() {
        val input = ";(function(){});"
        val expected = "((function(){}))();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test async function`() {
        val input = "async function() { await doSomething(); }"
        val expected = "(async function() { await doSomething(); })();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test arrow function`() {
        val input = "() => { return 42; }"
        val expected = "(() => { return 42; })();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test arrow function with arguments`() {
        val input = "x => x * 2"
        val args = "5"
        val expected = "(x => x * 2)(5);"
        assertEquals(expected, toIIFEOrNull(input, args))
    }

    @Test
    fun `test object literal should not be treated as function`() {
        val input = "{ key: 'value' }"
        val expected = "({ key: 'value' });"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test invalid format returns error message`() {
        val input = "This is not a function"
        // ❌ Unsupported format: not a valid JS function
        assertNull(toIIFEOrNull(input))
    }

    @Test
    fun `test empty string returns error message`() {
        val input = ""
        // ❌ Unsupported format: not a valid JS function
        assertNull(toIIFEOrNull(input))
    }

    @Test
    fun `test function with multi-line expressions`() {
        val input = """
            const a = 10;
            const b = 20;
            return a * b;
        """.trimIndent()

        // ❌ Unsupported format: not a valid JS function
        assertNull(toIIFEOrNull(input))
    }

    @Test
    fun `test function starting with x`() {
        val input = "x => x + 1"
        val expected = "(x => x + 1)();"
        assertEquals(expected, toIIFEOrNull(input))
    }

    @Test
    fun `test function with custom arguments`() {
        val input = "function(a, b) { return a + b; }"
        val args = "1, 2"
        val expected = "(function(a, b) { return a + b; })(1, 2);"
        assertEquals(expected, toIIFEOrNull(input, args))
    }
}
