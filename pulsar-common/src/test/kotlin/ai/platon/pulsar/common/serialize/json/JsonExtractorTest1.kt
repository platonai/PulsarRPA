package ai.platon.pulsar.common.serialize.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonExtractorTest {

    @Test
    fun testNoJson() {
        val text = "This text has no json."
        val result = JsonExtractor.extractJsonBlocks(text)
        assertTrue(result.isEmpty(), "Expected no JSON blocks")
    }

    @Test
    fun testSingleJson() {
        val text = "Before text {\"key\":\"value\"} after text."
        val result = JsonExtractor.extractJsonBlocks(text)
        assertEquals(1, result.size)
        assertEquals("{\"key\":\"value\"}", result[0])
    }

    @Test
    fun testMultipleJson() {
        val text = "First {\"a\":1} second {\"b\":2}"
        val result = JsonExtractor.extractJsonBlocks(text)
        assertEquals(2, result.size)
        assertEquals("{\"a\":1}", result[0])
        assertEquals("{\"b\":2}", result[1])
    }

    @Test
    fun testNestedJson() {
        val text = "Nested {\"outer\":{\"inner\":true}} text"
        val result = JsonExtractor.extractJsonBlocks(text)
        assertEquals(1, result.size)
        assertEquals("{\"outer\":{\"inner\":true}}", result[0])
    }
}

