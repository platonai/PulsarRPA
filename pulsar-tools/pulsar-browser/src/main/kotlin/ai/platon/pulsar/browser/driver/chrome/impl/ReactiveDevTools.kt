package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.DevToolsConfig
import ai.platon.pulsar.browser.driver.chrome.MethodInvocation
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.WebSocketClient
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDevToolsInvocationException
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
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
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

internal class RpcFuture(
        val returnProperty: String?,
        val clazz: Class<*>,
        val returnTypeClasses: Array<Class<out Any>>?
) {
    var result: JsonNode? = null
    var isDone = false
    var isSuccess = false

    fun setDone(isSuccess: Boolean, result: JsonNode?) {
        this.isSuccess = isSuccess
        this.result = result
    }
}

abstract class ReactiveDevTools(
        private val wsClient: WebSocketClient,
        private val devToolsConfig: DevToolsConfig
): RemoteDevTools, Consumer<String>, AutoCloseable {

    companion object {
        private val LOG = LoggerFactory.getLogger(BasicDevTools::class.java)

        private const val ID_PROPERTY = "id"
        private const val ERROR_PROPERTY = "error"
        private const val RESULT_PROPERTY = "result"
        private const val METHOD_PROPERTY = "method"
        private const val PARAMS_PROPERTY = "params"

        private val OBJECT_MAPPER = ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val workerGroup = devToolsConfig.workerGroup
    private val rpcResults: MutableMap<Long, RpcFuture> = ConcurrentHashMap()
    private val eventHandlers: MutableMap<String, MutableSet<DevToolsEventListener>> = mutableMapOf()
    private val lock = ReentrantLock() // lock for containers
    private val notBusy = lock.newCondition()
    private val closeLatch = CountDownLatch(1)
    private val closed = AtomicBoolean()
    override val isOpen get() = !closed.get() && !wsClient.isClosed()

    init {
        wsClient.addMessageHandler(this)
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
        if (!isOpen) {
            return null
        }

        try {
            val future = RpcFuture(returnProperty, clazz, returnTypeClasses)
            rpcResults[methodInvocation.id] = future
            wsClient.send(OBJECT_MAPPER.writeValueAsString(methodInvocation))
        } catch (e: WebSocketServiceException) {
            throw ChromeDevToolsInvocationException("Web socket connection lost", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: IOException) {
            throw ChromeDevToolsInvocationException("Failed reading response message", e)
        }

        return null
    }

    fun watch() {
        while (true) {
            rpcResults.forEach {
                val result = it.value
                if (result.isDone) {
                    if (result.isSuccess) {
                        when {
                            Void.TYPE == result.clazz -> null
                            result.returnTypeClasses != null -> readJsonObject(result.returnTypeClasses, result.clazz, result.result)
                            else -> readJsonObject(result.clazz, result.result)
                        }
                    } else {
                        val error = readJsonObject(ErrorObject::class.java, result.result)
                        val sb = StringBuilder(error.message)
                        if (error.data != null) {
                            sb.append(": ")
                            sb.append(error.data)
                        }
                    }
                }
            }
        }
    }

    override fun addEventListener(domainName: String,
                                  eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>): EventListener {
        val name = "$domainName.$eventName"
        val listener = DevToolsEventListener(name, eventHandler, eventType, this)
        eventHandlers.computeIfAbsent(name) { createEventHandlerSet() }.add(listener)
        return listener
    }

    private fun createEventHandlerSet(): MutableSet<DevToolsEventListener> {
        return Collections.synchronizedSet<DevToolsEventListener>(HashSet())
    }

    override fun removeEventListener(eventListener: EventListener) {
        val listener = eventListener as DevToolsEventListener
        eventHandlers[listener.key]?.removeIf { listener.handler == it.handler }
    }

    override fun accept(message: String) {
        LOG.trace(message)

        try {
            val jsonNode = OBJECT_MAPPER.readTree(message)
            val idNode = jsonNode.get(ID_PROPERTY)
            if (idNode != null) {
                val id = idNode.asLong()
                val result = rpcResults[id]
                if (result != null) {
                    var resultNode = jsonNode.get(RESULT_PROPERTY)
                    val errorNode = jsonNode.get(ERROR_PROPERTY)
                    if (errorNode != null) {
                        result.setDone(false, errorNode)
                    } else {
                        if (result.returnProperty != null) {
                            if (resultNode != null) {
                                resultNode = resultNode.get(result.returnProperty)
                            }
                        }
                        if (resultNode != null) {
                            result.setDone(true, resultNode)
                        } else {
                            result.setDone(true, null)
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
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            lock.withLock {
                if (rpcResults.isNotEmpty()) {
                    notBusy.await(5, TimeUnit.SECONDS)
                }
            }

            wsClient.close()
            workerGroup.shutdownGracefully()

            closeLatch.countDown()
        }
    }

    private fun handleEvent(name: String, params: JsonNode) {
        if (!isOpen) return

        val listeners = eventHandlers[name] ?:return

        var unmodifiedListeners: Set<DevToolsEventListener>
        // make a copy
        synchronized(listeners) { unmodifiedListeners = HashSet<DevToolsEventListener>(listeners) }
        if (unmodifiedListeners.isEmpty()) return

//        GlobalScope.launch {
//            var event: Any? = null
//            for (listener in unmodifiedListeners) {
//                try {
//                    if (event == null) {
//                        // event = readJsonObject(listener.paramType, params)
//                        event = OBJECT_MAPPER.readerFor(listener.paramType).readValue(params)
//                    }
//
//                    if (event != null) {
//                        try {
//                            listener.handler.onEvent(event)
//                        } catch (t: Throwable) {
//                            LOG.warn("Unexpected exception", t)
//                        }
//                    }
//                } catch (e: Exception) {
//                    LOG.error("Error while processing event {}", name, e)
//                }
//            }
//        }

        workerGroup.execute {
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
        }
    }

    @Throws(IOException::class)
    private fun <T> readJsonObject(classParameters: Array<Class<*>>, parameterizedClazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeDevToolsInvocationException("Failed converting null response to clazz $parameterizedClazz")
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

    @Throws(IOException::class)
    private fun <T> readJsonObject(clazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeDevToolsInvocationException("Failed converting null response to clazz " + clazz.name)
        }
        return OBJECT_MAPPER.readerFor(clazz).readValue(jsonNode)
    }
}
