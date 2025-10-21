package ai.platon.pulsar.common.serialize.json

import ai.platon.pulsar.common.math.roundTo
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.lang3.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode

class DoubleSerializer(val decimals: Int = 2): JsonSerializer<Double>() {
    override fun serialize(value: Double?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
        } else if (value.isNaN()) {
            gen.writeRawValue("NaN")
        } else if (value == Double.POSITIVE_INFINITY) {
            gen.writeRawValue("∞")
        } else if (value == Double.NEGATIVE_INFINITY) {
            gen.writeRawValue("-∞")
        } else {
            // remove tailing `0`,  remove tailing `.`
            val s = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toString()
            val s1 = s.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
            gen.writeNumber(s1)
        }
    }
}

fun doubleBindModule(decimals: Int = 2): SimpleModule {
    return SimpleModule().apply {
        val doubleSerializer = DoubleSerializer(decimals)
        addSerializer(Double::class.java, doubleSerializer)
        // Keep double value length minimal
        addSerializer(Double::class.javaPrimitiveType, doubleSerializer)

        // Ensure doubles inside containers (List/Map/Any) are also formatted by DoubleSerializer.
        // Register a Number serializer that delegates to Double serializer for Double values,
        // and falls back to the default provider for other numeric types.
        addSerializer(Number::class.java, object : JsonSerializer<Number>() {
            override fun serialize(
                value: Number?,
                gen: JsonGenerator,
                serializers: SerializerProvider
            ) {
                if (value == null) {
                    gen.writeNull()
                    return
                }

                if (value is Double) {
                    // Delegate to the configured Double serializer for consistent formatting
                    doubleSerializer.serialize(value, gen, serializers)
                } else {
                    // For other numeric types, fall back to default serialization
                    serializers.defaultSerializeValue(value, gen)
                }
            }
        })
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
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
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
    private val mapper = pulsarObjectMapper()
    private val prettyMapper = prettyPulsarObjectMapper()

    fun toJson(any: Any) = mapper.writeValueAsString(any)!!
    fun toPrettyJson(any: Any) = prettyMapper.writeValueAsString(any)!!
}

/**
 * A shorter name following Gson naming conventions.
 * */
typealias Pson = PulsarJackson
