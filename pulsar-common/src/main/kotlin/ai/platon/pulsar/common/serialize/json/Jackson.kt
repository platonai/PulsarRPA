package ai.platon.pulsar.common.serialize.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * jacksonObjectMapper with support:
 * 1. kotlin
 * 2. java 8 data time
 * */
fun pulsarObjectMapper(): ObjectMapper = jsonMapper {
    addModule(JavaTimeModule())
    addModule(kotlinModule())
}.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

/**
 * jacksonObjectMapper with support:
 * 1. kotlin
 * 2. java 8 data time
 * 3. pretty print
 * */
fun prettyPulsarObjectMapper(): ObjectMapper = pulsarObjectMapper()
    .configure(SerializationFeature.INDENT_OUTPUT, true)
