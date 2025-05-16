package ai.platon.pulsar.common.logging

import ai.platon.pulsar.common.concurrent.ConcurrentPassiveExpiringSet
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A logger wrapper that ensures a message is logged only once within a given time-to-live (TTL).
 *
 * This helps avoid spamming logs with repetitive messages while still ensuring important information gets through.
 */
class ThrottlingLogger(
    private val logger: Logger,
    private val ttl: Duration = Duration.ofMinutes(30),
    private val enableSuppressedCount: Boolean = false
) {
    private val expiringMessageCache = ConcurrentPassiveExpiringSet<String>(ttl)
    private val suppressedCountMap = if (enableSuppressedCount) ConcurrentHashMap<String, AtomicInteger>() else null

    /**
     * Logs a message at the specified level if it hasn't been logged in the last [ttl].
     */
    fun logIfNotExpired(message: String, logLevel: (String) -> Unit) {
        if (!expiringMessageCache.contains(message)) {
            expiringMessageCache.add(message)
            logLevel(message)
        } else if (enableSuppressedCount) {
            suppressedCountMap?.computeIfAbsent(message) { AtomicInteger() }?.incrementAndGet()
        }
    }

    /**
     * Logs a message at the debug level if not expired.
     */
    fun debug(message: String) = logIfNotExpired(message, logger::debug)

    /**
     * Logs a message at the info level if not expired.
     */
    fun info(message: String) = logIfNotExpired(message, logger::info)

    /**
     * Logs a message at the warn level if not expired.
     */
    fun warn(message: String) = logIfNotExpired(message, logger::warn)

    /**
     * Logs a message at the error level if not expired.
     */
    fun error(message: String) = logIfNotExpired(message, logger::error)

    /**
     * Logs a message and associated exception at the error level if not expired.
     */
    fun error(message: String, throwable: Throwable) {
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