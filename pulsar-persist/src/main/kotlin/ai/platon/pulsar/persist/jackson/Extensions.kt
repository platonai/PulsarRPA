package ai.platon.pulsar.persist.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * jacksonObjectMapper with java 8 data time support
 * */
fun pulsarObjectMapper(): ObjectMapper = jsonMapper {
    addModule(JavaTimeModule())
    addModule(kotlinModule())
}.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

fun prettyPulsarObjectMapper(): ObjectMapper = jsonMapper {
    addModule(JavaTimeModule())
    addModule(kotlinModule())
}.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(SerializationFeature.INDENT_OUTPUT, true)
