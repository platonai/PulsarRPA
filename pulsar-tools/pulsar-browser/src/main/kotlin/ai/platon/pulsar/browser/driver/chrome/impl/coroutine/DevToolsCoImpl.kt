package ai.platon.pulsar.browser.driver.chrome.impl.coroutine

import ai.platon.pulsar.browser.driver.chrome.DevToolsConfig
import ai.platon.pulsar.browser.driver.chrome.MethodInvocation
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.Transport
import ai.platon.pulsar.browser.driver.chrome.impl.EventDispatcher
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCTimeoutException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.warnForClose
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import com.fasterxml.jackson.databind.JsonNode
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class DevToolsCoImpl(
    private val browserClient: Transport,
    private val pageClient: Transport,
    private val config: DevToolsConfig
) : RemoteDevTools, AutoCloseable {
    
    companion object {
        private val instanceSequencer = AtomicInteger()
        
        private val startTime = Instant.now()
        private var lastActiveTime = startTime
        private val idleTime get() = Duration.between(lastActiveTime, Instant.now())
        
        private val metrics = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
        private val metricsPrefix = "c.i.BasicDevTools.global"
        private val numInvokes = metrics.counter("$metricsPrefix.invokes")
        val numAccepts = metrics.counter("$metricsPrefix.accepts")
        private val gauges = mapOf(
            "idleTime" to Gauge { idleTime.readable() }
        )
        
        init {
            gauges.forEach { (name, gauge) -> metrics.gauge("$metricsPrefix.$name") { gauge } }
        }
    }
    
    private val logger = LoggerFactory.getLogger(DevToolsCoImpl::class.java)
    private val id = instanceSequencer.incrementAndGet()
    
    private val closeLatch = CountDownLatch(1)
    private val closed = AtomicBoolean()
    override val isOpen get() = !closed.get() && !pageClient.isClosed()
    
    private val serializer = EventDispatcher()
    
    /**
     * Invokes a remote method and returns the result.
     * The method is blocking and will wait for the response.
     *
     * TODO: use non-blocking version
     *
     * @param returnProperty The property to return from the response.
     * @param clazz The class of the return type.
     * @param <T> The return type.
     * @return The result of the invocation.
     * */
    open suspend operator fun <T> invoke(
        returnProperty: String,
        clazz: Class<T>,
        methodInvocation: MethodInvocation
    ): T? {
        try {
            return invoke0(returnProperty, clazz, null, methodInvocation)
        }  catch (e: WebSocketServiceException) {
            throw ChromeRPCException("Web socket connection lost", e)
        } catch (e: InterruptedException) {
            logger.warn("Interrupted while invoke ${clazz::javaClass.name}.${methodInvocation.method}")
            Thread.currentThread().interrupt()
            return null
        } catch (e: IOException) {
            throw ChromeRPCException("Failed reading response message", e)
        }
    }
    
    /**
     * Invokes a remote method and returns the result.
     * The method is blocking and will wait for the response.
     *
     * @param returnProperty The property to return from the response.
     * @param clazz The class of the return type.
     * @param returnTypeClasses The classes of the return type.
     * @param method The method to invoke.
     * @param <T> The return type.
     * @return The result of the invocation.
     * */
    suspend fun <T> invokeDeferred(
        returnProperty: String?,
        clazz: Class<T>,
        returnTypeClasses: Array<Class<out Any>>?,
        method: MethodInvocation
    ): T? {
        try {
            return invoke0(returnProperty, clazz, returnTypeClasses, method)
        }  catch (e: WebSocketServiceException) {
            throw ChromeRPCException("Web socket connection lost", e)
        } catch (e: InterruptedException) {
            logger.warn("Interrupted while invoke ${method.method}")
            Thread.currentThread().interrupt()
            return null
        } catch (e: IOException) {
            throw ChromeRPCException("Failed reading response message", e)
        }
    }
    
    @Throws(InterruptedException::class, ChromeServiceException::class)
    private suspend fun <T> invoke0(
        returnProperty: String?,
        clazz: Class<T>,
        returnTypeClasses: Array<Class<out Any>>?,
        method: MethodInvocation
    ): T? {
        if (!isOpen) {
            return null
        }
        
        numInvokes.inc()
        lastActiveTime = Instant.now()
        
        val (response, responded) = invokeDeferred1(returnProperty, method)
        
        if (!responded) {
            val methodName = method.method
            val readTimeout = config.readTimeout
            throw ChromeRPCTimeoutException("Response timeout $methodName | #${numInvokes.count}, ($readTimeout)")
        }

        return when {
            Void.TYPE == clazz -> null
            returnTypeClasses != null -> serializer.deserialize(returnTypeClasses, clazz, response)
            else -> serializer.deserialize(clazz, response)
        }
    }
    
    private suspend fun invokeDeferred1(
        returnProperty: String?,
        method: MethodInvocation
    ): Pair<JsonNode, Boolean> {
        val ktorBrowserClient = browserClient as TransportKtorImpl
        val ktorPageClient = pageClient as TransportKtorImpl
        
        val message = serializer.serialize(method)
        
        // See https://github.com/hardkoded/puppeteer-sharp/issues/796 to understand why we need handle Target methods
        // differently.
        val messageReceived = if (method.method.startsWith("Target.")) {
            ktorBrowserClient.sendDeferred(message)
        } else {
            ktorPageClient.sendDeferred(message)
        }

        var responded = true
        val response = if (messageReceived != null) {
            // serializer.accept(messageReceived)
            pulsarObjectMapper().readTree(messageReceived)
        } else {
            responded = false
            pulsarObjectMapper().readTree("{error: 'No response'}")
        }
        
        return response to responded
    }
    
    override fun addEventListener(
        domainName: String,
        eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>
    ): EventListener {
        TODO("Not supported")
    }
    
    override fun removeEventListener(eventListener: EventListener) {
        TODO("Not supported")
    }
    
    /**
     * Waits for the DevTool to terminate.
     * */
    @Throws(InterruptedException::class)
    override fun awaitTermination() {
        try {
            // block the calling thread
            closeLatch.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            // discard all furthers in dispatcher?
            runCatching { doClose() }.onFailure { warnForClose(this, it) }
            
            // Decrements the count of the latch, releasing all waiting threads if the count reaches zero.
            // If the current count is greater than zero then it is decremented. If the new count is zero then all
            // waiting threads are re-enabled for thread scheduling purposes.
            // If the current count equals zero then nothing happens.
            closeLatch.countDown()
        }
    }
    
    @Throws(Exception::class)
    private fun doClose() {
        // NOTE:
        // 1. It's a bad idea to throw an InterruptedException in close() method
        // 2. No need to wait for the dispatcher to be idle
        // waitUntilIdle(Duration.ofSeconds(5))
        
        serializer.close()
        
        logger.info("Closing devtools client ...")
        
        browserClient.close()
        pageClient.close()
    }
}
