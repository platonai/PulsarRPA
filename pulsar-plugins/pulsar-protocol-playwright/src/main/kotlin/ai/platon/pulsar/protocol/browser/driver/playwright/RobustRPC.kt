package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserUnavailableException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.IllegalWebDriverStateException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class RobustRPC(
    private val driver: PlaywrightDriver
) {
    companion object {
        // handle to many exceptions
        private val exceptionCounts = ConcurrentHashMap<String, AtomicInteger>()
        private val exceptionMessages = ConcurrentHashMap<String, String>()
        var MAX_RPC_FAILURES = 5
    }

    private val logger = getLogger(this)

    val rpcFailures = AtomicInteger()
    var maxRPCFailures = MAX_RPC_FAILURES

    @Throws(WebDriverException::class)
    fun <T> invoke(action: String, block: () -> T): T? {
        if (!driver.checkState(action)) {
            return null
        }

        try {
            return block().also { decreaseRPCFailures() }
        } catch (e: Exception) {
            increaseRPCFailures()

            if (e is WebDriverException) {
                throw e
            }
            else {
                throw WebDriverException(cause = e)
            }
        }
    }

    @Throws(WebDriverException::class)
    suspend fun <T> invokeDeferred(action: String, maxRetry: Int = 2, block: suspend CoroutineScope.() -> T): T? {
        if (!driver.checkState(action)) {
            return null
        }

        var i = maxRetry
        var result = kotlin.runCatching { invokeDeferred0(action, block) }
            .onFailure {
                // no handler here
            }
        while (result.isFailure && i-- > 0 && driver.checkState(action)) {
            result = kotlin.runCatching { invokeDeferred0(action, block) }
                .onFailure {
                    // no handler here
                }
        }

        return result.getOrElse { throw it }
    }

    suspend fun <T> invokeDeferredSilently(
        action: String, message: String? = null, maxRetry: Int = 2, block: suspend CoroutineScope.() -> T
    ): T? {
        return try {
            invokeDeferred(action, maxRetry, block)
        } catch (e: WebDriverException) {
            handleWebDriverException(e, action, message)
            null
        }
    }

    @Throws(WebDriverException::class)
    fun handleWebDriverException(e: Exception, action: String? = null, message: String? = null) {
        when (e) {
            is WebDriverException -> {
                handlePlaywrightIOException(e, action, message)
            }
            else -> throw e
        }
    }

    @Throws(BrowserUnavailableException::class, IllegalWebDriverStateException::class)
    fun handlePlaywrightIOException(e: WebDriverException, action: String? = null, message: String? = null) {
        val message2 = MessageFormat.format("[{0}] ({1}/{2}) | {3}", action, rpcFailures, maxRPCFailures, message)

        if (!driver.isConnectable) {
            throw BrowserUnavailableException("Browser connection closed | $message2", e)
        } else if (!driver.isConnectable) {
            throw BrowserUnavailableException("Browser connection lost | $message2", e)
        } else {
            throw IllegalWebDriverStateException("Unknown playwright IO error | $message2", e)
        }
    }

    @Throws(WebDriverException::class)
    private suspend fun <T> invokeDeferred0(action: String, block: suspend CoroutineScope.() -> T): T? {
        return withContext(Dispatchers.IO) {
            if (!driver.checkState(action)) {
                return@withContext null
            }

            try {
                // It's bad if block() is blocking, it will block the whole thread and no other coroutine can run within this
                // thread, so we should avoid blocking in the block(). Unfortunately, the block() is usually a rpc call,
                // the rpc call blocks its calling thread and wait for the response.
                // We should find a way to avoid the blocking in the block() and make it non-blocking.
                block().also { decreaseRPCFailures() }
            } catch (e: Exception) {
                increaseRPCFailures()

                if (e is WebDriverException) {
                    throw e
                } else {
                    throw WebDriverException(cause = e)
                }
            }
        }
    }

    private fun decreaseRPCFailures() {
        rpcFailures.getAndUpdate { it.dec().coerceAtLeast(0) }
    }

    private fun increaseRPCFailures() {
        rpcFailures.incrementAndGet()
    }

    /**
     * Normalize message, remove all digits
     * */
    private fun normalizeMessage(message: String?): String {
        if (message == null) {
            return ""
        }

        return message.filterNot { it.isDigit() }
    }

    private fun traceException(e: Exception) {
        val key = e.javaClass.name
        exceptionCounts.computeIfAbsent(key) { AtomicInteger() }.incrementAndGet()
        exceptionMessages[key] = normalizeMessage(e.message)
    }

    private fun logException(count: Int, e: Exception, action: String? = null, message: String? = null) {
        if (message == null) {
            logger.info(
                "{}.\t[{}] ({}/{}) | {}",
                count,
                action,
                rpcFailures,
                maxRPCFailures,
                e.message
            )
        } else {
            logger.info(
                "{}.\t[{}] ({}/{}) | {} | {}",
                count,
                action,
                rpcFailures,
                maxRPCFailures,
                message,
                e.message
            )
        }

        if (e.cause != null) {
            if (driver.browser.isActive) {
                logger.warn(e.cause?.stringify("Caused by: "))
            } else {
                // The browser is closing, nothing to do
            }
        }
    }
}