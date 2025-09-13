package ai.platon.pulsar.common.serialize.json

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JSONExtractorTest {

    @Test
    fun testEmptyString() {
        val input = ""
        val expectedOutput = emptyList<String>()
        assertEquals(expectedOutput, JSONExtractor.extractJsonBlocks(input))
    }

    @Test
    fun testNoJsonBlocks() {
        val input = "This is a plain text."
        val expectedOutput = emptyList<String>()
        assertEquals(expectedOutput, JSONExtractor.extractJsonBlocks(input))
    }

    @Test
    fun testSingleJsonBlock() {
        val input = "{\"key\": \"value\"}"
        val expectedOutput = listOf("{\"key\": \"value\"}")
        assertEquals(expectedOutput, JSONExtractor.extractJsonBlocks(input))
    }

    @Test
    fun testMultipleJsonBlocks() {
        val input = "{\"key1\": \"value1\"}{\"key2\": \"value2\"}"
        val expectedOutput = listOf("{\"key1\": \"value1\"}", "{\"key2\": \"value2\"}")
        assertEquals(expectedOutput, JSONExtractor.extractJsonBlocks(input))
    }

    @Test
    fun testNestedJsonBlock() {
        val input = "{\"key1\": {\"key2\": \"value2\"}}"
        val expectedOutput = listOf("{\"key1\": {\"key2\": \"value2\"}}")
        assertEquals(expectedOutput, JSONExtractor.extractJsonBlocks(input))
    }

    @Test
    fun testIncompleteJsonBlock() {
        val input = "{\"key\": \"value\""
        val expectedOutput = emptyList<String>()
        assertEquals(expectedOutput, JSONExtractor.extractJsonBlocks(input))
    }
}
