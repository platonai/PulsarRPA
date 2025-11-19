package ai.platon.pulsar.common.serialize.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DoubleBindModuleTest {

    private fun mapper(decimals: Int = 2, includeNulls: Boolean = false) =
        jacksonObjectMapper().apply {
            if (includeNulls) setSerializationInclusion(JsonInclude.Include.ALWAYS)
            registerModule(doubleBindModule(decimals))
        }

    data class Box(
        val a: Double,         // primitive
        val b: Double?,        // boxed
        val i: Int,            // other number types shouldn't be affected
        val l: Long,
        val any: Any,          // runtime Double
        val num: Number        // runtime Double as Number
    )

    @Test
    fun `primitive and boxed doubles are formatted`() {
        val m = mapper()
        val box = Box(
            a = 1.234,
            b = 2.0,
            i = 7,
            l = 9L,
            any = 3.14159,
            num = 4.0
        )
        val json = m.writeValueAsString(box)
        // a rounded to 1.23, b trimmed to 2, any rounded to 3.14, num rounded to 4
        assertEquals("{" +
                "\"a\":1.23," +
                "\"b\":2," +
                "\"i\":7," +
                "\"l\":9," +
                "\"any\":3.14," +
                "\"num\":4" +
                "}", json)
    }

    @Test
    fun `list array and map elements are formatted`() {
        val m = mapper()
        val arr = arrayOf(1.234, 1.0, 2.5)
        val list = listOf(1.234, 1.0, 2.5)
        val listAny: List<Any> = listOf(1.234, 1.0, 2.5)
        val listNum: List<Number> = listOf(1.234, 1.0, 2.5)
        val mapAny: Map<String, Any> = mapOf("x" to 1.234, "y" to 1.0, "z" to 2.5)
        val mapNum: Map<String, Number> = mapOf("x" to 1.234, "y" to 1.0, "z" to 2.5)

        assertEquals("[1.23,1,2.5]", m.writeValueAsString(arr))
        assertEquals("[1.23,1,2.5]", m.writeValueAsString(list))
        assertEquals("[1.23,1,2.5]", m.writeValueAsString(listAny))
        assertEquals("[1.23,1,2.5]", m.writeValueAsString(listNum))
        assertEquals("{\"x\":1.23,\"y\":1,\"z\":2.5}", m.writeValueAsString(mapAny))
        assertEquals("{\"x\":1.23,\"y\":1,\"z\":2.5}", m.writeValueAsString(mapNum))
    }

    @Test
    fun `configured decimals are honored`() {
        val m = mapper(decimals = 3)
        val list = listOf(1.2346, 0.005, -0.005)
        // HALF_UP rounding with 3 decimals
        assertEquals("[1.235,0.005,-0.005]", m.writeValueAsString(list))
    }

    @Test
    fun `nested structures format doubles without recursion`() {
        val m = mapper()
        val tree: Map<String, Any> = linkedMapOf(
            "n" to 1.234,
            "child" to linkedMapOf(
                "v" to 2.0,
                "arr" to listOf(3.0, 4.0, 5.005),
                "grand" to linkedMapOf(
                    "x" to listOf<Number>(6.0, 7.234, 8.0),
                    "y" to 9.999
                )
            )
        )
        val json = m.writeValueAsString(tree)
        assertEquals("{" +
                "\"n\":1.23," +
                "\"child\":{" +
                "\"v\":2," +
                "\"arr\":[3,4,5.01]," +
                "\"grand\":{" +
                "\"x\":[6,7.23,8]," +
                "\"y\":10" +
                "}" +
                "}" +
                "}", json)
    }
}
