package ai.platon.pulsar.common.serialize.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.RoundingMode
import java.text.DecimalFormat

class Double2Serializer : JsonSerializer<Double>() {
    private val df = DecimalFormat("#.##").apply {
        roundingMode = RoundingMode.HALF_UP
    }

    override fun serialize(value: Double?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
        } else {
            val s = df.format(value)
            // remove tailing `0`,  remove tailing `.`

            gen.writeNumber(s)
        }
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
    .registerModule(JavaTimeModule())

/**
 * jacksonObjectMapper with support:
 * 1. kotlin
 * 2. java 8 data time
 * 3. pretty print
 * */
fun prettyPulsarObjectMapper(): ObjectMapper = pulsarObjectMapper()
    .configure(SerializationFeature.INDENT_OUTPUT, true)
