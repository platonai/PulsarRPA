package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.Transport
import ai.platon.pulsar.browser.driver.chrome.impl.TransportImpl.Companion.instanceSequencer
import ai.platon.pulsar.common.config.AppConstants
import com.codahale.metrics.SharedMetricRegistries
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.websocket.MessageHandler
import javax.websocket.Session

suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        for (message in incoming) {
            message as? Frame.Text ?: continue
            println(message.readText())
        }
    } catch (e: Exception) {
        println("Error while receiving: " + e.localizedMessage)
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages() {
    while (true) {
        val message = readlnOrNull() ?: ""
        if (message.equals("exit", true)) return
        try {
            send(message)
        } catch (e: Exception) {
            println("Error while sending: " + e.localizedMessage)
            return
        }
    }
}

class TransportKtorImpl : Transport {
    private val logger = LoggerFactory.getLogger(TransportKtorImpl::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    val id = instanceSequencer.incrementAndGet()
    private lateinit var session: Session
    private val metricsPrefix = "c.i.WebSocketClient"
    private val metrics = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
    private val meterRequests = metrics.meter("$metricsPrefix.requests")
    
    private val client = HttpClient {
        install(WebSockets)
    }
    
    class DevToolsMessageHandler(val consumer: Consumer<String>) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            consumer.accept(message)
        }
    }
    
    override fun connect(uri: URI) {
        TODO("Not yet implemented")
    }

    suspend fun connectDeferred(uri: URI) {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }
            
            userInputRoutine.join() // Wait for completion; either "exit" or error
            messageOutputRoutine.cancelAndJoin()
        }
    }
    
    override fun send(message: String) {
        TODO("Not yet implemented")
    }
    
    override fun sendAsync(message: String): Future<Void> {
        TODO("Not yet implemented")
    }

    override fun addMessageHandler(consumer: Consumer<String>) {
        TODO("Not yet implemented")
    }
    
    override fun isClosed(): Boolean {
        return !session.isOpen || closed.get()
    }
    
    override fun close() {
        TODO("Not yet implemented")
    }
}
