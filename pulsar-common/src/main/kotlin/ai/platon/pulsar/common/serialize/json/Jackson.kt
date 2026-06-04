@file:Suppress("Unused")

package ai.platon.pulsar.common.serialize.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Strategy to serialize non-finite floating point values (NaN / +/-Infinity).
 */
enum class NonFiniteDoubleStrategy {
    /** Write JSON null. Most compatible and always valid JSON. */
    NULL,

    /** Write a JSON string like "NaN", "Infinity", "-Infinity". */
    STRING,
}

class DoubleSerializer(
    val decimals: Int = 2,
    private val nonFiniteStrategy: NonFiniteDoubleStrategy = NonFiniteDoubleStrategy.NULL,
) : JsonSerializer<Double>() {
    override fun serialize(value: Double?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        if (!value.isFinite()) {
            when (nonFiniteStrategy) {
                NonFiniteDoubleStrategy.NULL -> gen.writeNull()
                NonFiniteDoubleStrategy.STRING -> {
                    val s = when {
                        value.isNaN() -> "NaN"
                        value == Double.POSITIVE_INFINITY -> "Infinity"
                        else -> "-Infinity"
                    }
                    gen.writeString(s)
                }
            }
            return
        }

        // remove tailing `0`,  remove tailing `.`
        val s = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toString()
        val s1 = s.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
        gen.writeNumber(s1)
    }
}

/**
 * A safe Number serializer that avoids recursion:
 * - Formats Double/Float using DoubleSerializer
 * - Writes other numeric types via direct generator methods
 * - Falls back to runtime-type serializer when needed (never defaultSerializeValue)
 */
class SmartNumberSerializer(private val decimals: Int = 2) : JsonSerializer<Number>() {
    // Keep decimals as a real property and build the delegate serializer from it (also avoids unused-param warnings).
    private val doubleSerializer = DoubleSerializer(decimals = decimals)

    override fun serialize(value: Number?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }
        when (value) {
            is Double -> doubleSerializer.serialize(value, gen, serializers)
            is Float -> doubleSerializer.serialize(value.toDouble(), gen, serializers)
            is Int -> gen.writeNumber(value)
            is Long -> gen.writeNumber(value)
            is Short -> gen.writeNumber(value.toInt())
            is Byte -> gen.writeNumber(value.toInt())
            is BigDecimal -> gen.writeNumber(value)
            is java.math.BigInteger -> gen.writeNumber(value)
            else -> {
                // Delegate to serializer bound to the concrete runtime class to avoid Number->Number recursion
                val s = serializers.findValueSerializer(value.javaClass, null)
                s.serialize(value, gen, serializers)
            }
        }
    }
}

fun doubleBindModule(decimals: Int = 2): SimpleModule {
    return SimpleModule().apply {
        val doubleSerializer = DoubleSerializer(decimals)
        addSerializer(Double::class.java, doubleSerializer)
        // Keep double value length minimal
        addSerializer(Double::class.javaPrimitiveType, doubleSerializer)
        // Handle Number containers (List<Number>, Map<String, Number>, Any) without recursion
        addSerializer(Number::class.java, SmartNumberSerializer(decimals))
    }
}

/**
 * jacksonObjectMapper with support:
 * 1. kotlin
 * 2. java 8 data time
 * */
fun pulsarObjectMapper(): ObjectMapper = jacksonObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
    .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    // Prefer non-deprecated API; setDefaultPropertyInclusion is deprecated in newer Jackson.
    .setDefaultPropertyInclusion(
        JsonInclude.Value.construct(
            JsonInclude.Include.NON_EMPTY,
            JsonInclude.Include.NON_EMPTY
        )
    )
    // Ensure Double/Number formatting works in containers like List<Number>, Map<String, Any>, etc.
    .registerModule(doubleBindModule())
    .registerModule(JavaTimeModule())

/**
 * jacksonObjectMapper with support:
 * 1. kotlin
 * 2. java 8 data time
 * 3. pretty print
 * */
fun prettyPulsarObjectMapper(): ObjectMapper = pulsarObjectMapper()
    .configure(SerializationFeature.INDENT_OUTPUT, true)

object PulsarJackson {
    private val mapper get() = pulsarObjectMapper()
    private val prettyMapper get() = prettyPulsarObjectMapper()

    fun toJson(vararg value: Pair<String, Any>): String = mapper.writeValueAsString(mapOf(*value))!!

    fun toJson(value: Any): String = mapper.writeValueAsString(value)!!
    fun toPrettyJson(value: Any): String = prettyMapper.writeValueAsString(value)!!

    fun toJsonOrNull(value: Any): String? = runCatching { mapper.writeValueAsString(value) }.getOrNull()
    fun toPrettyJsonOrNull(value: Any): String? = runCatching { prettyMapper.writeValueAsString(value) }.getOrNull()

    fun toJsonOrString(value: Any): String = toJsonOrNull(value) ?: value.toString()
    fun toPrettyJsonOrString(value: Any): String = toPrettyJsonOrNull(value) ?: value.toString()

    fun readTree(json: String): JsonNode? = mapper.readTree(json)
    fun readTreeOrNull(json: String): JsonNode? = runCatching { mapper.readTree(json) }.getOrNull()
}

/**
 * A shorter name following Gson naming conventions.
 * */
typealias Pson = PulsarJackson
