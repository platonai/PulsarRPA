package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun <T : Any> getLogger(clazz: KClass<T>) = LoggerFactory.getLogger(clazz.java)

fun getLogger(any: Any): Logger = if (any is KClass<*>) {
    LoggerFactory.getLogger(any.java)
} else LoggerFactory.getLogger(any::class.java)

fun getTracer(any: Any): Logger? = getLogger(any).takeIf { it.isTraceEnabled }

fun getRandomLogger(): Logger = LoggerFactory.getLogger(RandomStringUtils.randomAlphabetic(8))
