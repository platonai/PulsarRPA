package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.protocol.browser.driver.SessionLostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.Throws

internal class RobustRPC(
    private val driver: ChromeDevtoolsDriver
) {
    private val logger = LoggerFactory.getLogger(RobustRPC::class.java)!!

    val isActive get() = driver.isActive

    val rpcFailures = AtomicInteger()
    var maxRPCFailures = 5

    @Throws(SessionLostException::class)
    fun handleRPCException(e: ChromeRPCException, action: String? = null, url: String? = null) {
        if (rpcFailures.get() > maxRPCFailures) {
            logger.warn("Too many RPC failures: {} ({}/{}) | {}", action, rpcFailures, maxRPCFailures, e.message)
            throw SessionLostException("Too many RPC failures", driver)
        }

        logger.info("Chrome RPC exception: [{}] ({}/{}) | {}", action, rpcFailures, maxRPCFailures, e.message)
    }

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
                throw e
            }
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
