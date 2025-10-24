package ai.platon.pulsar.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class StringsTest {

    @Test
    fun `test readableClassName with simple class`() {
        val obj = StringsTest()
        val result = readableClassName(obj)
        assertEquals("s-c-StringsTest", result)
    }

    @Test
    fun `test readableClassName with fullNameCount parameter`() {
        val obj = StringsTest()
        val result = readableClassName(obj, fullNameCount = 2)
        assertEquals("s.c.StringsTest", result)
    }

    @Test
    fun `test readableClassName with partCount parameter`() {
        val obj = StringsTest()
        val result = readableClassName(obj, partCount = 2)
        assertEquals("c-StringsTest", result)
    }

    @Test
    fun `test readableClassName with Class object`() {
        val clazz = StringsTest::class.java
        val result = readableClassName(clazz)
        assertEquals("s-c-StringsTest", result)
    }

    @Test
    fun `test readableClassName with KClass object`() {
        val kclass = StringsTest::class
        val result = readableClassName(kclass)
        assertEquals("s-c-StringsTest", result)
    }

    @Test
    fun `test readableClassName with Companion object`() {
        val obj = TestCompanion
        val result = readableClassName(obj)
        assertTrue(result.contains("C"))
    }

    @Test
    fun `test readableClassName with nested class`() {
        val obj = TestNestedClass()
        val result = readableClassName(obj)
        assertTrue(result.contains("_"))
    }

    @Test
    fun `test prependReadableClassName with default separator`() {
        val obj = StringsTest()
        val name = "testName"
        val result = prependReadableClassName(obj, name)
        assertEquals("s-c-StringsTest.testName", result)
    }

    @Test
    fun `test prependReadableClassName with custom separator`() {
        val obj = StringsTest()
        val name = "testName"
        val separator = "-"
        val result = prependReadableClassName(obj, name, separator)
        assertEquals("s-c-StringsTest-testName", result)
    }

    @Test
    fun `test prependReadableClassName with multiple dots`() {
        val obj = StringsTest()
        val name = "test...name"
        val result = prependReadableClassName(obj, name)
        assertEquals("s-c-StringsTest.test.name", result)
    }

    @Test
    fun `test prependReadableClassName with ident and default separator`() {
        val obj = StringsTest()
        val ident = "testIdent"
        val name = "testName"
        val result = prependReadableClassName(obj, ident, name, ".")
        assertEquals("s-c-StringsTest.testIdent.testName", result)
    }

    @Test
    fun `test prependReadableClassName with blank ident`() {
        val obj = StringsTest()
        val ident = ""
        val name = "testName"
        val result = prependReadableClassName(obj, ident, name, ".")
        assertEquals("s-c-StringsTest.testName", result)
    }

    @Test
    fun `test prependReadableClassName with whitespace ident`() {
        val obj = StringsTest()
        val ident = "   "
        val name = "testName"
        val result = prependReadableClassName(obj, ident, name, ".")
        assertEquals("s-c-StringsTest.testName", result)
    }

    @Test
    fun `test prependReadableClassName with custom separator and ident`() {
        val obj = StringsTest()
        val ident = "testIdent"
        val name = "testName"
        val separator = "_"
        val result = prependReadableClassName(obj, ident, name, separator)
        assertEquals("s_c_StringsTest_testIdent_testName", result)
    }

    @Test
    fun `test stringifyException with simple exception`() {
        val exception = RuntimeException("Test exception")
        val result = stringifyException(exception)
        assertTrue(result.contains("Test exception"))
        assertTrue(result.contains("RuntimeException"))
        assertTrue(result.contains("at ai.platon.pulsar.common.StringsTest"))
    }

    @Test
    fun `test stringifyException with prefix`() {
        val exception = RuntimeException("Test exception")
        val prefix = "ERROR: "
        val result = stringifyException(exception, prefix)
        assertTrue(result.startsWith("ERROR: "))
        assertTrue(result.contains("Test exception"))
    }

    @Test
    fun `test stringifyException with postfix`() {
        val exception = RuntimeException("Test exception")
        val postfix = " [END]"
        val result = stringifyException(exception, postfix = postfix)
        assertTrue(result.contains("Test exception"))
        assertTrue(result.endsWith(" [END]"))
    }

    @Test
    fun `test stringifyException with both prefix and postfix`() {
        val exception = RuntimeException("Test exception")
        val prefix = "ERROR: "
        val postfix = " [END]"
        val result = stringifyException(exception, prefix, postfix)
        assertTrue(result.startsWith("ERROR: "))
        assertTrue(result.contains("Test exception"))
        assertTrue(result.endsWith(" [END]"))
    }

    @Test
    fun `test stringifyException with nested exception`() {
        val innerException = IllegalArgumentException("Inner exception")
        val outerException = RuntimeException("Outer exception", innerException)
        val result = stringifyException(outerException)
        assertTrue(result.contains("Outer exception"))
        assertTrue(result.contains("Inner exception"))
        assertTrue(result.contains("Caused by:"))
    }

    @Test
    fun `test simplifyException with simple message`() {
        val exception = RuntimeException("Simple error message")
        val result = simplifyException(exception)
        assertEquals("Simple error message", result)
    }

    @Test
    fun `test simplifyException with null message`() {
        val exception = RuntimeException()
        val result = simplifyException(exception)
        assertTrue(result.contains("RuntimeException"))
        assertTrue(result.contains("at ai.platon.pulsar.common.StringsTest"))
    }

    @Test
    fun `test simplifyException with multiline message`() {
        val exception = RuntimeException("First line\nSecond line")
        val result = simplifyException(exception)
        assertEquals("First line\tSecond line", result)
    }

    @Test
    fun `test simplifyException with three line message`() {
        val exception = RuntimeException("First line\nSecond line\nThird line")
        val result = simplifyException(exception)
        assertEquals("First line\tSecond line ...", result)
    }

    @Test
    fun `test simplifyException with prefix`() {
        val exception = RuntimeException("Error message")
        val prefix = "ALERT: "
        val result = simplifyException(exception, prefix)
        assertEquals("ALERT: Error message", result)
    }

    @Test
    fun `test simplifyException with postfix`() {
        val exception = RuntimeException("Error message")
        val postfix = " (handled)"
        val result = simplifyException(exception, postfix = postfix)
        assertEquals("Error message (handled)", result)
    }

    @Test
    fun `test simplifyException with both prefix and postfix`() {
        val exception = RuntimeException("Error message")
        val prefix = "ALERT: "
        val postfix = " (handled)"
        val result = simplifyException(exception, prefix, postfix)
        assertEquals("ALERT: Error message (handled)", result)
    }

    @Test
    fun `test simplifyException with empty message`() {
        val exception = RuntimeException("")
        val result = simplifyException(exception)
        assertTrue(result.contains("RuntimeException"))
    }

    @Test
    fun `test readableClassName with very long class name`() {
        val obj = TestVeryLongClassNameThatGoesOnAndOn()
        val result = readableClassName(obj, partCount = 5)
        assertTrue(result.contains("TestVeryLongClassNameThatGoesOnAndOn"))
    }

    @Test
    fun `test readableClassName with different fullNameCount values`() {
        val obj = StringsTest()
        val result1 = readableClassName(obj, fullNameCount = 0)
        val result2 = readableClassName(obj, fullNameCount = 1)
        val result3 = readableClassName(obj, fullNameCount = 2)
        val result4 = readableClassName(obj, fullNameCount = 3)

        assertTrue(result1.contains("s.c.StringsTest"))
        assertEquals("s-c-StringsTest", result2)
        assertEquals("s.c.StringsTest", result3)
        assertEquals("s.c.StringsTest", result4)
    }

    @ParameterizedTest
    @CsvSource(
        "0, 1, c-StringsTest",
        "0, 2, c.StringsTest",
        "1, 1, s-c-StringsTest",
        "1, 2, s.c.StringsTest",
        "2, 2, s.c.StringsTest",
        "2, 3, a.s.c.StringsTest"
    )
    fun `test readableClassName with various parameter combinations`(fullNameCount: Int, partCount: Int, expected: String) {
        val obj = StringsTest()
        val result = readableClassName(obj, fullNameCount, partCount)
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @ValueSource(strings = [".", "-", "_", "/", "|", "::"])
    fun `test prependReadableClassName with various separators`(separator: String) {
        val obj = StringsTest()
        val name = "testName"
        val result = prependReadableClassName(obj, name, separator)
        assertTrue(result.contains("StringsTest"))
        assertTrue(result.contains("testName"))
    }

    @Test
    fun `test stringifyException with IOException`() {
        val exception = IOException("File not found")
        val result = stringifyException(exception)
        assertTrue(result.contains("File not found"))
        assertTrue(result.contains("IOException"))
    }

    @Test
    fun `test simplifyException with IOException`() {
        val exception = IOException("File not found")
        val result = simplifyException(exception)
        assertEquals("File not found", result)
    }

    companion object TestCompanion

    class TestNestedClass

    class TestVeryLongClassNameThatGoesOnAndOn
}