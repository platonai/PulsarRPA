package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

internal class RPC(
    private val devTools: RemoteDevTools
) {
    private val logger = LoggerFactory.getLogger(RPC::class.java)!!

    val isActive get() = devTools.isOpen

    val rpcFailures = AtomicInteger()
    var maxRPCFailures = 5

    fun <T> invoke(action: String, block: () -> T): T? {
        if (!refreshState(action)) {
            return null
        }

        try {
            return block().also { decreaseRPCFailures() }
        } catch (e: ChromeRPCException) {
            increaseRPCFailures()
            throw e
        }
    }

    suspend fun <T> invokeDeferred(action: String, maxRetry: Int = 2, block: suspend CoroutineScope.() -> T): T? {
        var i = maxRetry
        var result = kotlin.runCatching { invokeDeferred0(action, block) }
        while (result.isFailure && i-- > 0 && isActive) {
            result = kotlin.runCatching { invokeDeferred0(action, block) }
        }

        return result.getOrElse { throw it }
    }

    fun handleRPCException(e: ChromeRPCException, action: String? = null) {
        if (rpcFailures.get() > maxRPCFailures) {
            throw ChromeRPCException("Too many RPC failures")
        }

        logger.warn("Chrome RPC exception: {} ({}/{}) | {}", action, rpcFailures, maxRPCFailures, e.message)
    }

    private suspend fun <T> invokeDeferred0(action: String, block: suspend CoroutineScope.() -> T): T? {
        return withContext(Dispatchers.IO) {
            if (!refreshState(action)) {
                return@withContext null
            }

            try {
                block().also { decreaseRPCFailures() }
            } catch (e: ChromeRPCException) {
                increaseRPCFailures()
                throw e
            }
        }
    }

    private fun refreshState(action: String): Boolean {
        return isActive
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
