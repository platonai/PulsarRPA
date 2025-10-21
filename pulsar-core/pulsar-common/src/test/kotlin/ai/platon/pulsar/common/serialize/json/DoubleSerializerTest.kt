package ai.platon.pulsar.common.serialize.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringWriter

class DoubleSerializerTest {

    private fun mapper(includeNulls: Boolean = true): ObjectMapper =
        jacksonObjectMapper().apply {
            if (includeNulls) {
                setSerializationInclusion(JsonInclude.Include.ALWAYS)
            }
            val module = SimpleModule().apply {
                addSerializer(Double::class.java, DoubleSerializer())
                addSerializer(Double::class.javaPrimitiveType, DoubleSerializer())
            }
            registerModule(module)
        }

    private fun serializeWithCustom(value: Double?): String {
        val writer = StringWriter()
        val gen = JsonFactory().createGenerator(writer)
        // Provider is not used by serializer, but Jackson API requires it
        val provider = mapper().serializerProvider
        DoubleSerializer().serialize(value, gen, provider)
        gen.flush()
        return writer.toString()
    }

    data class Holder(val a: Double, val b: Double?, val c: Double)

    @Test
    fun `direct serialize - rounding and trimming basic numbers`() {
        assertEquals("1.1", serializeWithCustom(1.101))
        assertEquals("1.23", serializeWithCustom(1.234))
        assertEquals("1.2", serializeWithCustom(1.2))
        assertEquals("1", serializeWithCustom(1.0))
        assertEquals("1", serializeWithCustom(1.000000))
        assertEquals("1", serializeWithCustom(1.0000001))
        assertEquals("1.1", serializeWithCustom(1.1000000))
        assertEquals("1.1", serializeWithCustom(1.1000001))
        assertEquals("0", serializeWithCustom(0.0))
        assertEquals("2.5", serializeWithCustom(2.5))
        assertEquals("-1.23", serializeWithCustom(-1.234))
        assertEquals("-1.2", serializeWithCustom(-1.2))
        assertEquals("2", serializeWithCustom(1.999))
    }

    @Test
    fun `direct serialize - null value`() {
        assertEquals("null", serializeWithCustom(null))
    }

    @Test
    fun `direct serialize - rounding boundaries`() {
        // HALF_UP rounding
        assertEquals("0.01", serializeWithCustom(0.005))
        assertEquals("0", serializeWithCustom(0.0049))
        assertEquals("-0.01", serializeWithCustom(-0.005))
        assertEquals("123456789.56", serializeWithCustom(123_456_789.555))
    }

    @Test
    fun `direct serialize - non-finite numbers as tokens`() {
        // DecimalFormat renders special values as symbolic tokens; generator writes them as-is
        assertEquals("NaN", serializeWithCustom(Double.NaN))
        assertEquals("∞", serializeWithCustom(Double.POSITIVE_INFINITY))
        assertEquals("-∞", serializeWithCustom(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `integration - serialize primitive and boxed fields`() {
        val m = mapper()
        val h = Holder(1.234, 1.0, 2.0)
        val json = m.writeValueAsString(h)
        // Observed: primitive Doubles (a, c) are handled by custom serializer; boxed nullable Double (b) uses default
        assertEquals("{" +
                "\"a\":1.23," +
                "\"b\":1.0," +
                "\"c\":2" +
                "}", json)
    }

    @Test
    fun `integration - serialize with null boxed field included`() {
        val m = mapper(includeNulls = true)
        val h = Holder(1.234, null, 2.0)
        val json = m.writeValueAsString(h)
        assertEquals("{" +
                "\"a\":1.23," +
                "\"b\":null," +
                "\"c\":2" +
                "}", json)
    }

    @Test
    fun `integration - serialize arrays and lists use default for elements`() {
        val m = mapper()
        val arr = arrayOf(1.234, 1.0, 2.5)
        val list = listOf(1.234, 1.0, 2.5)

        val arrJson = m.writeValueAsString(arr)
        val listJson = m.writeValueAsString(list)

        // Observed behavior: custom serializer is not applied to collection elements in this configuration
        assertEquals("[1.234,1.0,2.5]", arrJson)
        assertEquals("[1.234,1.0,2.5]", listJson)
    }
}
