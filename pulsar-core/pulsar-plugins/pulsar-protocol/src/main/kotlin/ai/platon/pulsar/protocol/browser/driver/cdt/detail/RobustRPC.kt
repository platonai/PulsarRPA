package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeIOException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserUnavailableException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.IllegalWebDriverStateException
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RobustRPC(
    private val driver: PulsarWebDriver
) {
    companion object {
        private val logger = getLogger(this)
        // handle to many exceptions
        private val exceptionCounts = ConcurrentHashMap<Long, AtomicInteger>()
        private val exceptionMessages = ConcurrentHashMap<Long, String>()

        var MAX_RPC_FAILURES = 5
    }

    private val logger = getLogger(this)

    val isActive get() = driver.isActive

    val rpcFailures = AtomicInteger()
    var maxRPCFailures = MAX_RPC_FAILURES

    @Throws(ChromeRPCException::class)
    suspend fun <T> invoke(action: String, block: suspend () -> T): T? {
        if (!driver.checkState(action)) {
            return null
        }

        try {
            return block().also { decreaseRPCFailures() }
        } catch (e: ChromeRPCException) {
            increaseRPCFailures()
            throw e
        }
    }

    @Throws(Exception::class)
    suspend fun <T> invokeWithRetry(action: String, maxRetry: Int = 2, block: suspend () -> T): T? {
        if (!driver.checkState(action)) {
            return null
        }

        var result = kotlin.runCatching { invokeDeferred0(action, block) }
            .onFailure { logger.info("Failed to execute action: [$action], retrying 1/$maxRetry time ...", it.brief()) }

        var i = 1
        while (result.isFailure && i++ < maxRetry && driver.checkState()) {
            result = kotlin.runCatching { invokeDeferred0(action, block) }
                .onFailure { logger.warn("Failed to execute action: [$action], retrying $i/$maxRetry times", it) }
        }

        return result.getOrElse { throw it }
    }

    suspend fun <T> invokeSilently(action: String, message: String? = null, block: suspend () -> T): T? {
        return try {
            invoke(action, block)
        } catch (e: ChromeRPCException) {
            handleChromeException(e, action, message)
            null
        }
    }

    suspend fun <T> invokeDeferredSilently(
        action: String, message: String? = null, maxRetry: Int = 2, block: suspend () -> T
    ): T? {
        return try {
            invokeWithRetry(action, maxRetry, block)
        } catch (e: ChromeRPCException) {
            handleChromeException(e, action, message)
            null
        }
    }

    @Throws(IllegalWebDriverStateException::class, ChromeDriverException::class)
    fun handleChromeException(e: ChromeDriverException, action: String? = null, message: String? = null) {
        when (e) {
            is ChromeIOException -> {
                handleChromeIOException(e, action, message)
            }
            is ChromeRPCException -> {
                handleChromeRPCException(e, action, message)
            }
            else -> throw e
        }
    }

    @Throws(BrowserUnavailableException::class, IllegalWebDriverStateException::class)
    fun handleChromeIOException(e: ChromeIOException, action: String? = null, message: String? = null) {
        if (!AppContext.isActive) {
            logger.info("Ignored chrome IO exception because of system shutting down")
            return
        }

        val message2 = MessageFormat.format("Browser unavailable: {0} ({1}/{2}) | {3}",
            action, rpcFailures, maxRPCFailures, e.message)

        if (!e.isOpen) {
            throw BrowserUnavailableException("Browser connection closed | $message2", e)
        } else if (!driver.isConnectable) {
            throw BrowserUnavailableException("Browser connection lost | $message2", e)
        } else {
            throw IllegalWebDriverStateException("Unknown chrome IO error | $message2", e)
        }
    }

    @Throws(IllegalWebDriverStateException::class)
    fun handleChromeRPCException(e: ChromeRPCException, action: String? = null, message: String? = null) {
        if (rpcFailures.get() > maxRPCFailures) {
            logger.warn("Too many RPC failures: {} ({}/{}) | {}", action, rpcFailures, maxRPCFailures, e.message)
            throw IllegalWebDriverStateException("Too many RPC failures", driver = driver)
        }

        val count = exceptionCounts.computeIfAbsent(e.code) { AtomicInteger() }.get()
        traceException(e)

        if (count < 10L) {
            logException(count, e, action, message)
        } else if (count < 100L && count % 10 == 0) {
            logException(count, e, action, message)
        } else if (count < 1000L && count % 50 == 0) {
            logException(count, e, action, message)
        }
    }

    @Throws(ChromeRPCException::class)
    private suspend fun <T> invokeDeferred0(action: String, block: suspend () -> T): T? {
        if (!driver.checkState(action)) {
            return null
        }

        try {
            return block().also { decreaseRPCFailures() }
        } catch (e: ChromeRPCException) {
            increaseRPCFailures()
            fixCDTAgentIfNecessary(e)
            throw e
        }
    }

    @Throws(ChromeIOException::class)
    private suspend fun fixCDTAgentIfNecessary(e: Exception) {
        if (e.toString().contains("agent was not enabled")) {
            logger.warn(e.stringify())
            try {
                driver.enableAPIAgents()
            } catch (ex: Exception) {
                throw ChromeIOException("Failed to enable CDT agents", e)
            }
            decreaseRPCFailures()
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

    private fun traceException(e: ChromeRPCException) {
        val code = e.code
        exceptionCounts.computeIfAbsent(code) { AtomicInteger() }.incrementAndGet()
        exceptionMessages[code] = normalizeMessage(e.message)
    }

    private fun logException(count: Int, e: ChromeRPCException, action: String? = null, message: String? = null) {
        if (message == null) {
            logger.info("{}.\t[{}] ({}/{}) | code: {}, {}", count, action, rpcFailures, maxRPCFailures, e.code, e.message)
        } else {
            logger.info("{}.\t[{}] ({}/{}) | {} | code: {}, {}", count, action, rpcFailures, maxRPCFailures, message, e.code, e.message)
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
