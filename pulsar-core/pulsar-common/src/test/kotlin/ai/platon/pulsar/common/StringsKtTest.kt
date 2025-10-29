package ai.platon.pulsar.common

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import kotlin.test.assertEquals

class StringsKtTest {

    @Test
    fun `test readableClassName with KClass object`() {
        val kclass = KStringsTest::class
        val result = readableClassName(kclass)
        assertEquals("a.p.p.c.StringsKtTest", result)
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
    fun `test prependReadableClassName with custom separator`() {
        val obj = KStringsTest()
        val name = "testName"
        val separator = "-"
        val result = prependReadableClassName(obj, name, separator)
        assertEquals("a-p-p-c-StringsKtTest-testName", result)
    }

    @Test
    fun `test prependReadableClassName with whitespace ident`() {
        val obj = KStringsTest()
        val ident = "   "
        val name = "testName"
        val result = prependReadableClassName(obj, ident, name, ".")
        assertEquals("a.p.p.c.StringsKtTest.testName", result)
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
    fun `test readableClassName with very long class name`() {
        val obj = TestVeryLongClassNameThatGoesOnAndOn()
        val result = readableClassName(obj, maxPartCount = 5)
        assertTrue(result.contains("TestVeryLongClassNameThatGoesOnAndOn"))
    }

    @ParameterizedTest
    @ValueSource(strings = [".", "-", "_", "/", "|", "::"])
    fun `test prependReadableClassName with various separators`(separator: String) {
        val obj = KStringsTest()
        val name = "testName"
        val result = prependReadableClassName(obj, name, separator)
        assertTrue(result.contains("StringsKtTest")) { result }
        assertTrue(result.contains("testName")) { result }
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
