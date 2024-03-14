package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun <T : Any> getLogger(clazz: KClass<T>): Logger = getLogger(clazz, "")

fun <T : Any> getLogger(clazz: KClass<T>, postfix: String): Logger = LoggerFactory.getLogger(clazz.java.name + postfix)

fun getLogger(target: Any): Logger = getLogger(target, "")

fun getLogger(target: Any, postfix: String): Logger = when (target) {
    is Logger -> target
    is KClass<*> -> LoggerFactory.getLogger(target.java.name + postfix)
    is Class<*> -> LoggerFactory.getLogger(target.name + postfix)
    is String -> LoggerFactory.getLogger(target + postfix)
    else -> LoggerFactory.getLogger(target::class.java.name + postfix)
}

fun getTracer(target: Any): Logger? = if (target is Logger) {
    target.takeIf { it.isTraceEnabled }
} else {
    getLogger(target).takeIf { it.isTraceEnabled }
}

fun getRandomLogger(): Logger = LoggerFactory.getLogger(RandomStringUtils.randomAlphabetic(8))

fun warn(target: Any, message: String, vararg args: Any?) {
    getLogger(target).warn(message, *args)
}

fun warn(target: Any, t: Throwable, message: String, vararg args: Any?) {
    getLogger(target).warn(message, t, *args)
}

fun warnInterruptible(target: Any, t: Throwable) = warnInterruptible(target, t, t.stringify())

fun warnInterruptible(target: Any, t: Throwable, message: String, vararg args: Any?) {
    getLogger(target).warn(message, *args)
    
    if (t is InterruptedException) {
        // Preserve interrupt status
        Thread.currentThread().interrupt()
    }
}

fun warnForClose(target: Any, t: Throwable) = warnForClose(target, t, t.stringify())

fun warnForClose(target: Any, t: Throwable, message: String, vararg args: Any?) {
    val logger = getLogger(target)
    logger.warn(message, *args)

    if (t is InterruptedException) {
        // Preserve interrupt status
        Thread.currentThread().interrupt()
        
        val message = """
                 * <p><em>Implementers of AutoClosable interface are strongly advised
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
