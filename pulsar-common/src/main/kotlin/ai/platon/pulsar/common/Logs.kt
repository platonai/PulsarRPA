package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun <T : Any> getLogger(clazz: KClass<T>) = getLogger(clazz, "")

fun <T : Any> getLogger(clazz: KClass<T>, postfix: String) = LoggerFactory.getLogger(clazz.java.name + postfix)

fun getLogger(any: Any): Logger = getLogger(any, "")

fun getLogger(any: Any, postfix: String): Logger = when (any) {
    is KClass<*> -> LoggerFactory.getLogger(any.java.name + postfix)
    is Class<*> -> LoggerFactory.getLogger(any.name + postfix)
    is String -> LoggerFactory.getLogger(any + postfix)
    else -> LoggerFactory.getLogger(any::class.java.name + postfix)
}

fun getTracer(any: Any): Logger? = if (any is Logger) {
    any.takeIf { it.isTraceEnabled }
} else {
    getLogger(any).takeIf { it.isTraceEnabled }
}

fun getRandomLogger(): Logger = LoggerFactory.getLogger(RandomStringUtils.randomAlphabetic(8))

fun warn(any: Any, message: String, vararg args: Any?) {
    getLogger(any).warn(message, *args)
}

fun warn(any: Any, t: Throwable, message: String, vararg args: Any?) {
    getLogger(any).warn(message, t, *args)
}

fun warnInterruptible(any: Any, t: Throwable) = warnInterruptible(any, t, t.stringify())

fun warnInterruptible(any: Any, t: Throwable, message: String, vararg args: Any?) {
    getLogger(any).warn(message, *args)
    
    if (t is InterruptedException) {
        // Preserve interrupt status
        Thread.currentThread().interrupt()
    }
}
