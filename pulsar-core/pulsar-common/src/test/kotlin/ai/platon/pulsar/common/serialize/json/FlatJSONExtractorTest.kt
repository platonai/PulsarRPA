package ai.platon.pulsar.common.serialize.json

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class FlatJSONExtractorTest {
    @Test
    fun `extract should return empty map for blank text`() {
        val extractor = FlatJSONExtractor("")
        val result = extractor.extract()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extract should return empty map when no JSON blocks found`() {
        val extractor = FlatJSONExtractor("This is plain text with no JSON")
        val result = extractor.extract()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extract should parse single JSON block correctly`() {
        val json = """{"name": "John", "age": "30"}"""
        val extractor = FlatJSONExtractor(json)
        val result = extractor.extract()

        assertEquals(2, result.size)
        assertEquals("John", result["name"])
        assertEquals("30", result["age"])
    }

    @Test
    fun `extract should combine multiple JSON blocks`() {
        val text = """
            Here is some text with JSON:
            {"name": "John", "age": "30"}
            And more text
            {"city": "New York", "country": "USA"}
        """.trimIndent()

        val extractor = FlatJSONExtractor(text)
        val result = extractor.extract()

        assertEquals(4, result.size)
        assertEquals("John", result["name"])
        assertEquals("30", result["age"])
        assertEquals("New York", result["city"])
        assertEquals("USA", result["country"])
    }

    @Test
    fun `extract should return empty map for invalid JSON block`() {
        val invalidJson = """{"name": {"first_name": "John", "last_name": "Yue"}, "age": 30}"""
        val extractor = FlatJSONExtractor(invalidJson)

        val result = extractor.extract()
        assertTrue(result.isEmpty(), "Expected empty map for invalid JSON block")
    }
}
