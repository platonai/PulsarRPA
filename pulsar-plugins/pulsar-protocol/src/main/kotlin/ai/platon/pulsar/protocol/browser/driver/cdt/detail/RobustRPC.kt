package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.browser.driver.chrome.util.ChromeDevToolsClosedException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.protocol.browser.driver.SessionLostException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.Throws

internal class RobustRPC(
    private val driver: ChromeDevtoolsDriver
) {
    companion object {
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
    fun <T> invoke(action: String, block: () -> T): T? {
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
    suspend fun <T> invokeDeferred(action: String, maxRetry: Int = 2, block: suspend CoroutineScope.() -> T): T? {
        if (!driver.checkState(action)) {
            return null
        }
        
        var i = maxRetry
        var result = kotlin.runCatching { invokeDeferred0(action, block) }
        while (result.isFailure && i-- > 0 && driver.checkState()) {
            result = kotlin.runCatching { invokeDeferred0(action, block) }
        }
        
        return result.getOrElse { throw it }
    }
    
    fun <T> invokeSilently(action: String, message: String? = null, block: () -> T): T? {
        return try {
            invoke(action, block)
        } catch (e: ChromeRPCException) {
            handleRPCException(e, action, message)
            null
        }
    }
    
    suspend fun <T> invokeDeferredSilently(
        action: String, message: String? = null, maxRetry: Int = 2, block: suspend CoroutineScope.() -> T): T? {
        return try {
            invokeDeferred(action, maxRetry, block)
        } catch (e: ChromeRPCException) {
            handleRPCException(e, action, message)
            null
        }
    }
    
    @Throws(SessionLostException::class)
    fun handleRPCException(e: ChromeRPCException, action: String? = null, message: String? = null) {
        if (rpcFailures.get() > maxRPCFailures) {
            logger.warn("Too many RPC failures: {} ({}/{}) | {}", action, rpcFailures, maxRPCFailures, e.message)
            throw SessionLostException("Too many RPC failures", driver)
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
            } catch (e: ChromeRPCException) {
                increaseRPCFailures()
                fixCDTAgentIfNecessary(e)
                throw e
            }
        }
    }

    private fun fixCDTAgentIfNecessary(e: Exception) {
        if (e.toString().contains("agent was not enabled")) {
            logger.warn(e.stringify())
            driver.enableAPIAgents()
            decreaseRPCFailures()
        }
    }

    private fun decreaseRPCFailures() {
        rpcFailures.decrementAndGet()
        if (rpcFailures.get() < 0) {
            rpcFailures.set(0)
        }
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
