package ai.platon.pulsar.browser.driver.chrome.impl.coroutine

import ai.platon.pulsar.browser.driver.chrome.Transport
import ai.platon.pulsar.browser.driver.chrome.impl.TransportImpl.Companion.instanceSequencer
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.warnForClose
import com.codahale.metrics.SharedMetricRegistries
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

class TransportKtorImpl() : Transport {
    private val logger = LoggerFactory.getLogger(TransportKtorImpl::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    val id = instanceSequencer.incrementAndGet()
    private val metricsPrefix = "c.i.WebSocketClient"
    private val metrics = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
    private val meterRequests = metrics.meter("$metricsPrefix.requests")
    
    private val client = HttpClient {
        install(WebSockets)
    }
    private lateinit var session: DefaultClientWebSocketSession
    
    suspend fun connectDeferred(uri: URI) {
        session = client.webSocketSession (method = HttpMethod.Get, host = uri.host, port = uri.port, path = uri.path)
    }
    
    suspend fun sendDeferred(message: String): String? {
        session.send(Frame.Text(message))
        
        // Retrieves and removes an element from this channel if it's not empty,
        // or suspends the caller while the channel is empty, or throws exception if the channel is closed.
        val frame = session.incoming.receive()
        
        // TODO: handle timeout
        
        return if (frame is Frame.Text) {
            frame.readText()
        } else null
    }
    
    override fun connect(uri: URI) {
        TODO("Not supported")
    }
    
    override fun send(message: String) {
        TODO("Not supported")
    }
    
    override fun sendAsync(message: String): Future<Void> {
        TODO("Not supported")
    }

    override fun addMessageHandler(consumer: Consumer<String>) {
        TODO("Not supported")
    }
    
    override fun isClosed(): Boolean {
        return closed.get()
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { client.close() }.onFailure { warnForClose(this, it) }
        }
    }
}
