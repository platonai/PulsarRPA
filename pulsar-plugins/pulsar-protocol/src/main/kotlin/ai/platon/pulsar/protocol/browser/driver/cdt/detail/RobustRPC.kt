package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.protocol.browser.driver.SessionLostException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.Throws

internal class RobustRPC(
    private val driver: ChromeDevtoolsDriver
) {
    private val logger = getLogger(this)
    
    val isActive get() = driver.isActive
    
    val rpcFailures = AtomicInteger()
    var maxRPCFailures = 5
    
    @Throws(SessionLostException::class)
    fun handleRPCException(e: ChromeRPCException, action: String? = null, message: String? = null) {
        if (rpcFailures.get() > maxRPCFailures) {
            logger.warn("Too many RPC failures: {} ({}/{}) | {}", action, rpcFailures, maxRPCFailures, e.message)
            throw SessionLostException("Too many RPC failures", driver)
        }
        
        if (message == null) {
            logger.info("[{}] ({}/{}) | {}, {}", action, rpcFailures, maxRPCFailures, e.code, e.message)
        } else {
            logger.info("[{}] ({}/{}) | {} | {}, {}", action, rpcFailures, maxRPCFailures, message, e.code, e.message)
        }
        if (e.cause != null) {
            logger.warn(e.cause?.brief("Caused by: "))
        }
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

    @Throws(ChromeRPCException::class)
    private suspend fun <T> invokeDeferred0(action: String, block: suspend CoroutineScope.() -> T): T? {
        if (!driver.checkState()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            if (!driver.checkState(action)) {
                return@withContext null
            }

            try {
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
}
