package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.DevToolsConfig
import ai.platon.pulsar.browser.driver.chrome.MethodInvocation
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.Transport
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.readable
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

class InvocationFuture(val returnProperty: String? = null) {
    var result: JsonNode? = null
    var isSuccess = false
    private val countDownLatch = CountDownLatch(1)

    fun signal(isSuccess: Boolean, result: JsonNode?) {
        this.isSuccess = isSuccess
        this.result = result
        countDownLatch.countDown()
    }

    @Throws(InterruptedException::class)
    fun await(timeout: Duration) = await(timeout.toMillis(), TimeUnit.MILLISECONDS)

    @Throws(InterruptedException::class)
    fun await(timeout: Long, timeUnit: TimeUnit): Boolean {
        return if (timeout == 0L) {
            countDownLatch.await()
            true
        } else countDownLatch.await(timeout, timeUnit)
    }
}

/** Error object returned from dev tools.  */
internal class ErrorObject {
    var code: Long = 0
    var message: String = ""
    var data: String? = null
}

class EventDispatcher : Consumer<String> {
    companion object {
        private const val ID_PROPERTY = "id"
        private const val ERROR_PROPERTY = "error"
        private const val RESULT_PROPERTY = "result"
        private const val METHOD_PROPERTY = "method"
        private const val PARAMS_PROPERTY = "params"

        private val OBJECT_MAPPER = ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    }

    private val logger = LoggerFactory.getLogger(EventDispatcher::class.java)

    private val invocationFutures: MutableMap<Long, InvocationFuture> = ConcurrentHashMap()
    private val eventListeners: MutableMap<String, MutableSet<DevToolsEventListener>> = mutableMapOf()

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

    @Throws(IOException::class)
    fun <T> deserialize(clazz: Class<T>, jsonNode: JsonNode?): T {
        if (jsonNode == null) {
            throw ChromeRPCException("Failed converting null response to clazz " + clazz.name)
        }
        return OBJECT_MAPPER.readerFor(clazz).readValue(jsonNode)
    }

    fun hasFutures() = invocationFutures.isNotEmpty()

    fun subscribe(id: Long, returnProperty: String?): InvocationFuture {
        return invocationFutures.computeIfAbsent(id) { InvocationFuture(returnProperty) }
    }

    fun unsubscribe(id: Long) {
        invocationFutures.remove(id)
    }

    fun registerListener(key: String, listener: DevToolsEventListener) {
        eventListeners.computeIfAbsent(key) { ConcurrentSkipListSet<DevToolsEventListener>() }.add(listener)
    }

    fun unregisterListener(key: String, listener: DevToolsEventListener) {
        eventListeners[key]?.removeIf { listener.handler == it.handler }
    }

    override fun accept(message: String) {
        logger.takeIf { it.isTraceEnabled }?.trace("Accept {}", StringUtils.abbreviateMiddle(message, "...", 500))

        BasicDevTools.numAccepts.inc()
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
        } catch (ex: IOException) {
            logger.error("Failed reading web socket message", ex)
        } catch (ex: java.lang.Exception) {
            logger.error("Failed receiving web socket message", ex)
        }
    }

    private fun handleEvent(name: String, params: JsonNode) {
        val listeners = eventListeners[name] ?: return

        // make a copy
        val unmodifiedListeners = mutableSetOf<DevToolsEventListener>()
        synchronized(listeners) { listeners.toCollection(unmodifiedListeners) }
        if (unmodifiedListeners.isEmpty()) return

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            handleEvent0(params, unmodifiedListeners)
        }
    }

    private fun handleEvent0(params: JsonNode, unmodifiedListeners: Iterable<DevToolsEventListener>) {
        var event: Any? = null
        for (listener in unmodifiedListeners) {
            if (event == null) {
                event = deserialize(listener.paramType, params)
            }

            if (event != null) {
                try {
                    listener.handler.onEvent(event)
                } catch (t: Throwable) {
                    logger.warn("Unexpected exception", t)
                }
            }
        }
    }
}

abstract class BasicDevTools(
    private val client: Transport,
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

    private val logger = LoggerFactory.getLogger(BasicDevTools::class.java)
    private val id = instanceSequencer.incrementAndGet()

    private val lock = ReentrantLock() // lock for containers
    private val notBusy = lock.newCondition()
    private val closeLatch = CountDownLatch(1)
    private val closed = AtomicBoolean()
    override val isOpen get() = !closed.get() && !client.isClosed()

    private val dispatcher = EventDispatcher()

    init {
        client.addMessageHandler(dispatcher)
    }

    open operator fun <T> invoke(returnProperty: String, clazz: Class<T>, methodInvocation: MethodInvocation): T? {
        return invoke(returnProperty, clazz, null, methodInvocation)
    }

    override operator fun <T> invoke(
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

        val future = dispatcher.subscribe(method.id, returnProperty)
        val message = dispatcher.serialize(method)

        try {
            client.send(message)

            val responded = future.await(config.readTimeout)
            dispatcher.unsubscribe(method.id)
            lastActiveTime = Instant.now()

            lock.withLock {
                if (!dispatcher.hasFutures()) {
                    notBusy.signalAll()
                }
            }

            if (!responded) {
                logger.warn("Timeout to wait for ws response #{}", numInvokes.count)
                throw ChromeRPCException("Timeout to wait for ws response #${numInvokes.count}")
            }

            if (future.isSuccess) {
                return when {
                    Void.TYPE == clazz -> null
                    returnTypeClasses != null -> dispatcher.deserialize(returnTypeClasses, clazz, future.result)
                    else -> dispatcher.deserialize(clazz, future.result)
                }
            }

            // Received an error
            val error = dispatcher.deserialize(ErrorObject::class.java, future.result)
            val sb = StringBuilder(error.message)
            if (error.data != null) {
                sb.append(": ")
                sb.append(error.data)
            }

            throw ChromeRPCException(error.code, sb.toString())
        } catch (e: WebSocketServiceException) {
            throw ChromeRPCException("Web socket connection lost", e)
        } catch (e: InterruptedException) {
            logger.warn("Interrupted while invoke ${method.method}")
            Thread.currentThread().interrupt()
            return null
        } catch (e: IOException) {
            throw ChromeRPCException("Failed reading response message", e)
        }
    }

    override fun addEventListener(
        domainName: String,
        eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>
    ): EventListener {
        val key = "$domainName.$eventName"
        val listener = DevToolsEventListener(key, eventHandler, eventType, this)
        dispatcher.registerListener(key, listener)
        return listener
    }

    override fun removeEventListener(eventListener: EventListener) {
        val listener = eventListener as DevToolsEventListener
        dispatcher.unregisterListener(listener.key, listener)
    }

    override fun waitUntilClosed() {
        runCatching { closeLatch.await() }.onFailure {
            if (it is InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { doClose() }.onFailure { logger.warn(it.message) }
            closeLatch.countDown()
        }
    }

    @Throws(Exception::class)
    private fun doClose() {
        lock.withLock {
            try {
                // TODO: no need to wait for all futures, just ignore them
                var i = 0
                while (i++ < 5 && dispatcher.hasFutures()) {
                    notBusy.await(1, TimeUnit.SECONDS)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        logger.trace("Closing ws client ... | {}", client)

        client.close()
    }
}
