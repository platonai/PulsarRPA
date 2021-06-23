package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun <T : Any> getLogger(clazz: KClass<T>, postfix: String = "") = LoggerFactory.getLogger(clazz.java.name + postfix)

fun getLogger(any: Any, postfix: String = ""): Logger = if (any is KClass<*>) {
    LoggerFactory.getLogger(any.java.name + postfix)
} else LoggerFactory.getLogger(any::class.java.name + postfix)

fun getTracer(any: Any): Logger? = getLogger(any).takeIf { it.isTraceEnabled }

fun getRandomLogger(): Logger = LoggerFactory.getLogger(RandomStringUtils.randomAlphabetic(8))
