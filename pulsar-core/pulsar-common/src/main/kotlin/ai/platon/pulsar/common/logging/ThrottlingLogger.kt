package ai.platon.pulsar.common.logging

import ai.platon.pulsar.common.concurrent.ConcurrentPassiveExpiringSet
import org.slf4j.Logger
import org.slf4j.helpers.MessageFormatter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A logger wrapper that ensures a message (including formatted ones) is logged only once within a given time-to-live (TTL).
 */
class ThrottlingLogger(
    private val logger: Logger,
    private val ttl: Duration = Duration.ofMinutes(30),
    private val enableSuppressedCount: Boolean = false
) {
    private val expiringMessageCache = ConcurrentPassiveExpiringSet<String>(ttl)
    private val suppressedCountMap = if (enableSuppressedCount) ConcurrentHashMap<String, AtomicInteger>() else null

    /**
     * Logs a formatted message at the specified level if it hasn't been logged in the last [ttl].
     */
    fun logIfNotExpired(format: String, args: Array<out Any?>, logLevel: (String) -> Unit) {
        val message = MessageFormatter.arrayFormat(format, args).message
        if (!expiringMessageCache.contains(message)) {
            expiringMessageCache.add(message)
            logLevel(message)
        } else if (enableSuppressedCount) {
            suppressedCountMap?.computeIfAbsent(message) { AtomicInteger() }?.incrementAndGet()
        }
    }

    /**
     * Logs a formatted message at the debug level if not expired.
     */
    fun debug(format: String, vararg args: Any?) =
        logIfNotExpired(format, args, logger::debug)

    /**
     * Logs a formatted message at the info level if not expired.
     */
    fun info(format: String, vararg args: Any?) =
        logIfNotExpired(format, args, logger::info)

    /**
     * Logs a formatted message at the warn level if not expired.
     */
    fun warn(format: String, vararg args: Any?) =
        logIfNotExpired(format, args, logger::warn)

    /**
     * Logs a formatted message at the error level if not expired.
     */
    fun error(format: String, vararg args: Any?) =
        logIfNotExpired(format, args, logger::error)

    /**
     * Logs a formatted message and associated exception at the error level if not expired.
     */
    fun error(throwable: Throwable, format: String, vararg args: Any?) {
        val message = String.format(format, *args)
        if (!expiringMessageCache.contains(message)) {
            expiringMessageCache.add(message)
            logger.error(message, throwable)
        } else if (enableSuppressedCount) {
            suppressedCountMap?.computeIfAbsent(message) { AtomicInteger() }?.incrementAndGet()
        }
    }

    /**
     * Gets the number of times each message has been suppressed due to TTL throttling.
     *
     * Only available if [enableSuppressedCount] is true.
     */
    fun getSuppressedCounts(): Map<String, Int>? {
        return suppressedCountMap?.mapValues { it.value.get() }
    }

    /**
     * Clears all tracked messages and resets suppression counters.
     */
    fun reset() {
        expiringMessageCache.clear()
        suppressedCountMap?.clear()
    }
}
