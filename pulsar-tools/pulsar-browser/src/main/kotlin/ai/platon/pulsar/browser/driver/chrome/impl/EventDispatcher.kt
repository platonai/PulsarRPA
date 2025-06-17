package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.getTracerOrNull
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.type.TypeFactory
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

class InvocationFuture(val returnProperty: String? = null) {
    var result: JsonNode? = null
    var isSuccess = false
    private val countDownLatch = CountDownLatch(1)
    
    fun signal(isSuccess: Boolean, result: JsonNode?) {
        this.isSuccess = isSuccess
        this.result = result
        countDownLatch.countDown()
    }
    
    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted, or the specified waiting time elapses.
     * */
    @Throws(InterruptedException::class)
    fun await(timeout: Duration) = await(timeout.toMillis(), TimeUnit.MILLISECONDS)
    
    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted, or the specified waiting time elapses.
     * */
    @Throws(InterruptedException::class)
    fun await(timeout: Long, timeUnit: TimeUnit): Boolean {
        return if (timeout == 0L) {
            countDownLatch.await()
            true
        } else countDownLatch.await(timeout, timeUnit)
    }
}

/** Error object returned from dev tools. */
internal class ErrorObject {
    var code: Long = 0
    var message: String = ""
    var data: String? = null
}

class EventDispatcher : Consumer<String>, AutoCloseable {
    companion object {
        const val ID_PROPERTY = "id"
        const val ERROR_PROPERTY = "error"
        const val RESULT_PROPERTY = "result"
        const val METHOD_PROPERTY = "method"
        const val PARAMS_PROPERTY = "params"
        
        val OBJECT_MAPPER = ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
    
    private val logger = getLogger(this)
    
    private val tracer = getTracerOrNull(this)
    
    private val closed = AtomicBoolean()
    private val invocationFutures: MutableMap<Long, InvocationFuture> = ConcurrentHashMap()
    private val eventListeners: ConcurrentHashMap<String, ConcurrentSkipListSet<DevToolsEventListener>> =
        ConcurrentHashMap()
    
    private val eventDispatcherScope = CoroutineScope(Dispatchers.Default) + CoroutineName("EventDispatcher")
    
    val isActive get() = !closed.get()

    /**
     * The last time a message was received from the Chrome DevTools.
     * */
    var lastReceivedTime: Instant? = null
        private set

    @Throws(JsonProcessingException::class)
    fun serialize(message: Any): String = OBJECT_MAPPER.writeValueAsString(message)
    
    @Throws(IOException::class)
    fun <T> deserialize(classParameters: Array<Class<*>>, parameterizedClazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeRPCException("Failed converting null response to clazz $parameterizedClazz")
        }
        
        val typeFactory: TypeFactory = OBJECT_MAPPER.typeFactory
        var javaType: JavaType? = null
        if (classParameters.size > 1) {
            for (i in classParameters.size - 2 downTo 0) {
                javaType = if (javaType == null) {
                    typeFactory.constructParametricType(classParameters[i], classParameters[i + 1])
                } else {
                    typeFactory.constructParametricType(classParameters[i], javaType)
                }
            }
            javaType = typeFactory.constructParametricType(parameterizedClazz, javaType)
        } else {
            javaType = typeFactory.constructParametricType(parameterizedClazz, classParameters[0])
        }
        
        return OBJECT_MAPPER.readerFor(javaType).readValue(jsonNode)
    }
    
    @Throws(IOException::class, ChromeRPCException::class)
    fun <T> deserialize(clazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeRPCException("Failed converting null response to clazz " + clazz.name)
        }
        
        try {
            // Here is a typical response sequence:
            // println(clazz)
            // RequestWillBeSent, RequestWillBeSentExtraInfo, ResponseReceivedExtra, ResponseReceived, LoadingFinished,
            return OBJECT_MAPPER.readerFor(clazz).readValue(jsonNode)
        } catch (e: MismatchedInputException) {
            val message = """
                Failed converting response to clazz ${clazz.name}
                $jsonNode
                """.trimIndent()
            logger.warn(message, e)
            throw e
        } catch (e: IOException) {
            // logger.warn("Failed converting response to clazz {}", clazz.name, e)
            throw e
        }
    }
    
    fun hasFutures() = invocationFutures.isNotEmpty()
    
    fun subscribe(id: Long, returnProperty: String?): InvocationFuture {
        return invocationFutures.computeIfAbsent(id) { InvocationFuture(returnProperty) }
    }
    
    fun unsubscribe(id: Long) {
        invocationFutures.remove(id)
    }
    
    fun unsubscribeAll() {
        invocationFutures.keys.forEach {
            invocationFutures.remove(it)?.signal(false, null)
        }
    }
    
    fun registerListener(key: String, listener: DevToolsEventListener) {
        eventListeners.computeIfAbsent(key) { ConcurrentSkipListSet<DevToolsEventListener>() }.add(listener)
    }
    
    fun unregisterListener(key: String, listener: DevToolsEventListener) {
        eventListeners[key]?.removeIf { listener.handler == it.handler }
    }
    
    fun removeAllListeners() {
        eventListeners.clear()
    }
    
    @Throws(ChromeRPCException::class, IOException::class)
    override fun accept(message: String) {
        lastReceivedTime = Instant.now()

        tracer?.trace("◀ Accept {}", StringUtils.abbreviateMiddle(message, "...", 500))
        
        ChromeDevToolsImpl.numAccepts.inc()
        try {
            val jsonNode = OBJECT_MAPPER.readTree(message)
            val idNode = jsonNode.get(ID_PROPERTY)
            if (idNode != null) {
                val id = idNode.asLong()
                val future = invocationFutures[id]
                if (future != null) {
                    var resultNode = jsonNode.get(RESULT_PROPERTY)
                    val errorNode = jsonNode.get(ERROR_PROPERTY)
                    if (errorNode != null) {
                        future.signal(false, errorNode)
                    } else {
                        if (future.returnProperty != null) {
                            if (resultNode != null) {
                                resultNode = resultNode.get(future.returnProperty)
                            }
                        }
                        
                        if (resultNode != null) {
                            future.signal(true, resultNode)
                        } else {
                            future.signal(true, null)
                        }
                    }
                } else {
                    logger.warn("Received response with unknown invocation #{} - {}", id, jsonNode.asText())
                }
            } else {
                val methodNode = jsonNode.get(METHOD_PROPERTY)
                val paramsNode = jsonNode.get(PARAMS_PROPERTY)
                if (methodNode != null) {
                    handleEvent(methodNode.asText(), paramsNode)
                }
            }
        } catch (e: IOException) {
            logger.error("Failed reading web socket message", e)
        }
    }
    
    /**
     * Closes the dispatcher. All event listeners will be removed and all waiting futures are signaled with failed.
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            unsubscribeAll()
            removeAllListeners()
        }
    }

    private fun handleEvent(name: String, params: JsonNode) {
        val listeners = eventListeners[name] ?: return

        // make a copy
        val unmodifiedListeners = mutableSetOf<DevToolsEventListener>()
        synchronized(listeners) { listeners.toCollection(unmodifiedListeners) }
        if (unmodifiedListeners.isEmpty()) {
            return
        }

        eventDispatcherScope.launch {
            handleEvent0(params, unmodifiedListeners)
        }
    }

    /**
     * Handles the event by deserializing the params and calling the event handler.
     *
     * Do not throw any exception, all exceptions are caught and logged.
     *
     * @param params the params node
     * @param unmodifiedListeners the listeners
     * @throws ChromeRPCException if the event could not be handled
     * */
    private fun handleEvent0(params: JsonNode, unmodifiedListeners: Iterable<DevToolsEventListener>) {
        try {
            handleEvent1(params, unmodifiedListeners)
        } catch (e: MismatchedInputException) {
            logger.warn("Mismatched input, Chrome might have upgraded the protocol | {}", e.message)
        } catch (t: Throwable) {
            logger.warn("Failed to handle event", t)
        }
    }

    @Throws(ChromeRPCException::class, IOException::class)
    private fun handleEvent1(params: JsonNode, unmodifiedListeners: Iterable<DevToolsEventListener>) {
        var event: Any? = null
        for (listener in unmodifiedListeners) {
            if (event == null) {
                event = deserialize(listener.paramType, params)
            }
            
            if (event != null) {
                try {
                    listener.handler.onEvent(event)
                } catch (e: Exception) {
                    logger.warn("Failed to handle event, rethrow ChromeRPCException. Enable debug logging to see the stack trace | {}", e.message)
                    logger.debug("Failed to handle event", e)
                    // Let the exception throw again, they might be caught by RobustRPC, or somewhere else
                    throw ChromeRPCException("Failed to handle event | ${listener.key}, ${listener.paramType}", e)
                }
            }
        }
    }
}
