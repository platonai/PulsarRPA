package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

private class InvocationResult(val returnProperty: String? = null) {
    var result: JsonNode? = null
    var isSuccess = false
    private val countDownLatch = CountDownLatch(1)

    fun signalResultReady(isSuccess: Boolean, result: JsonNode?) {
        this.isSuccess = isSuccess
        this.result = result
        countDownLatch.countDown()
    }

    @Throws(InterruptedException::class)
    fun waitForResult(timeout: Long, timeUnit: TimeUnit): Boolean {
        if (timeout == 0L) {
            countDownLatch.await()
            return true
        }
        return countDownLatch.await(timeout, timeUnit)
    }
}

/** Error object returned from dev tools.  */
private class ErrorObject {
    var code: Long = 0
    var message: String = ""
    var data: String? = null
}

abstract class ChromeDevToolsServiceImpl(
        val wsService: WebSocketService,
        val configuration: ChromeDevToolsServiceConfiguration
): ChromeDevToolsService, Consumer<String>, AutoCloseable {

    companion object {
        private val LOG = LoggerFactory.getLogger(ChromeDevToolsServiceImpl::class.java)

        private const val ID_PROPERTY = "id"
        private const val ERROR_PROPERTY = "error"
        private const val RESULT_PROPERTY = "result"
        private const val METHOD_PROPERTY = "method"
        private const val PARAMS_PROPERTY = "params"

        private val OBJECT_MAPPER = ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val eventExecutor = configuration.eventExecutorService
    private val invocationResults: MutableMap<Long, InvocationResult> = ConcurrentHashMap()
    private val eventHandlers: MutableMap<String, MutableSet<EventListenerImpl>> = mutableMapOf()
    private val lock = ReentrantLock() // lock for containers
    private val notBusy = lock.newCondition()
    private val closeLatch = CountDownLatch(1)
    private val closed = AtomicBoolean()
    private val isClosed get() = closed.get()

    init {
        wsService.addMessageHandler(this)
    }

    open operator fun <T> invoke(returnProperty: String, clazz: Class<T>, methodInvocation: MethodInvocation): T? {
        return invoke(returnProperty, clazz, null, methodInvocation)
    }

    override operator fun <T> invoke(
            returnProperty: String?,
            clazz: Class<T>,
            returnTypeClasses: Array<Class<out Any>>?,
            methodInvocation: MethodInvocation
    ): T? {
        if (isClosed) {
            return null
        }

        try {
            val result = InvocationResult(returnProperty)

            invocationResults[methodInvocation.id] = result
            wsService.send(OBJECT_MAPPER.writeValueAsString(methodInvocation))
            val responded = result.waitForResult(configuration.readTimeout.seconds, TimeUnit.SECONDS)
            invocationResults.remove(methodInvocation.id)

            lock.withLock {
                if (invocationResults.isEmpty()) {
                    notBusy.signalAll()
                }
            }

            if (!responded) {
                throw ChromeDevToolsInvocationException("Timeout to wait for response")
            }

            if (result.isSuccess) {
                return when {
                    Void.TYPE == clazz -> null
                    returnTypeClasses != null -> readJsonObject(returnTypeClasses, clazz, result.result)
                    else -> readJsonObject(clazz, result.result)
                }
            }

            // Received a error
            val error = readJsonObject(ErrorObject::class.java, result.result)
            val sb = StringBuilder(error.message)
            if (error.data != null) {
                sb.append(": ")
                sb.append(error.data)
            }

            throw ChromeDevToolsInvocationException(error.code, sb.toString())
        } catch (e: WebSocketServiceException) {
            throw ChromeDevToolsInvocationException("Web socket connection lost", e)
        } catch (e: InterruptedException) {
            throw ChromeDevToolsInvocationException("Interrupted while waiting response", e)
        } catch (e: IOException) {
            throw ChromeDevToolsInvocationException("Failed reading response message", e)
        }
    }

    override fun addEventListener(domainName: String,
            eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>): EventListener {
        val name = "$domainName.$eventName"
        val listener = EventListenerImpl(name, eventHandler, eventType, this)
        eventHandlers.computeIfAbsent(name) { createEventHandlerSet() }.add(listener)
        return listener
    }

    private fun createEventHandlerSet(): MutableSet<EventListenerImpl> {
        return Collections.synchronizedSet<EventListenerImpl>(HashSet())
    }

    override fun removeEventListener(eventListener: EventListener) {
        val listener = eventListener as EventListenerImpl
        eventHandlers[listener.key]?.removeIf { listener.handler == it.handler }
    }

    override fun accept(message: String) {
        try {
            val jsonNode = OBJECT_MAPPER.readTree(message)
            val idNode = jsonNode.get(ID_PROPERTY)
            if (idNode != null) {
                val id = idNode.asLong()
                val result = invocationResults[id]
                if (result != null) {
                    var resultNode = jsonNode.get(RESULT_PROPERTY)
                    val errorNode = jsonNode.get(ERROR_PROPERTY)
                    if (errorNode != null) {
                        result.signalResultReady(false, errorNode)
                    } else {
                        if (result.returnProperty != null) {
                            if (resultNode != null) {
                                resultNode = resultNode.get(result.returnProperty)
                            }
                        }
                        if (resultNode != null) {
                            result.signalResultReady(true, resultNode)
                        } else {
                            result.signalResultReady(true, null)
                        }
                    }
                } else {
                    LOG.warn("Received response with unknown invocation #{} - {}", id, jsonNode.asText())
                }
            } else {
                val methodNode = jsonNode.get(METHOD_PROPERTY)
                val paramsNode = jsonNode.get(PARAMS_PROPERTY)
                if (methodNode != null) {
                    handleEvent(methodNode.asText(), paramsNode)
                }
            }
        } catch (ex: IOException) {
            LOG.error("Failed reading web socket message!", ex)
        } catch (ex: java.lang.Exception) {
            LOG.error("Failed receiving web socket message!", ex)
        }
    }

    override fun waitUntilClosed() {
        try {
            closeLatch.await()
        } catch (ignored: InterruptedException) {}
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            lock.withLock {
                if (invocationResults.isNotEmpty()) {
                    notBusy.await(5, TimeUnit.SECONDS)
                }
            }

            eventExecutor.use { it.close() }
            wsService.use { it.close() }

            closeLatch.countDown()
        }
    }

    private fun handleEvent(name: String, params: JsonNode) {
        if (isClosed) return

        val listeners = eventHandlers[name] ?:return

        var unmodifiedListeners: Set<EventListenerImpl>
        // make a copy
        synchronized(listeners) { unmodifiedListeners = HashSet<EventListenerImpl>(listeners) }
        if (unmodifiedListeners.isEmpty()) return

        eventExecutor.execute(Runnable {
            var event: Any? = null
            for (listener in unmodifiedListeners) {
                try {
                    if (event == null) {
                        event = readJsonObject(listener.paramType, params)
                    }

                    if (event != null) {
                        try {
                            listener.handler.onEvent(event)
                        } catch (t: Throwable) {
                            LOG.warn("Unexpected exception", t)
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("Error while processing event {}", name, e)
                }
            }
        })
    }

    @Throws(IOException::class)
    private fun <T> readJsonObject(classParameters: Array<Class<*>>, parameterizedClazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeDevToolsInvocationException("Failed converting null response to clazz $parameterizedClazz")
        }

        val typeFactory: TypeFactory = OBJECT_MAPPER.getTypeFactory()
        var javaType: JavaType? = null
        if (classParameters.size > 1) {
            for (i in classParameters.size - 2 downTo 0) {
                javaType = if (javaType == null) {
                    typeFactory.constructParametricType(classParameters[i], classParameters[i + 1])
                } else {
                    typeFactory.constructParametricType(classParameters[i], javaType)
                }
            }
            javaType = OBJECT_MAPPER.typeFactory.constructParametricType(parameterizedClazz, javaType)
        } else {
            javaType = OBJECT_MAPPER.typeFactory.constructParametricType(parameterizedClazz, classParameters[0])
        }

        return OBJECT_MAPPER.readerFor(javaType).readValue(jsonNode)
    }

    @Throws(IOException::class)
    private fun <T> readJsonObject(clazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeDevToolsInvocationException("Failed converting null response to clazz " + clazz.name)
        }
        return OBJECT_MAPPER.readerFor(clazz).readValue(jsonNode)
    }
}
