package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun <T : Any> getLogger(clazz: KClass<T>): Logger = getLogger(clazz, "")

fun <T : Any> getLogger(clazz: KClass<T>, postfix: String): Logger = LoggerFactory.getLogger(clazz.java.name + postfix)

fun getLogger(any: Any): Logger = getLogger(any, "")

fun getLogger(any: Any, postfix: String): Logger = when (any) {
    is Logger -> any
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

fun warnForClose(any: Any, t: Throwable) = warnForClose(any, t, t.stringify())

fun warnForClose(any: Any, t: Throwable, message: String, vararg args: Any?) {
    val logger = getLogger(any)
    logger.warn(message, *args)

    if (t is InterruptedException) {
        // Preserve interrupt status
        Thread.currentThread().interrupt()
        
        val message = """
                 * <p><em>Implementers of close interface are strongly advised
                 * to not have the {@code close} method throw {@link
                 * InterruptedException}.</em>
                 *
                 * This exception interacts with a thread's interrupted status,
                 * and runtime misbehavior is likely to occur if an {@code
                 * InterruptedException} is {@linkplain Throwable#addSuppressed
                 * suppressed}.
                 *
                 * More generally, if it would cause problems for an
                 * exception to be suppressed, the {@code AutoCloseable.close}
                 * method should not throw it.
        """
        logger.warn(message)
        logger.warn(t.stringify())
    }
}
