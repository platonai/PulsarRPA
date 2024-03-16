package ai.platon.pulsar.browser.driver.chrome.impl.coroutine

import ai.platon.pulsar.browser.driver.chrome.CoTransport
import ai.platon.pulsar.browser.driver.chrome.impl.TransportImpl.Companion.instanceSequencer
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.warnForClose
import com.codahale.metrics.SharedMetricRegistries
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalApi
class CoTransportImpl : CoTransport {
    private val logger = LoggerFactory.getLogger(CoTransportImpl::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    private val metricsPrefix = "c.i.WebSocketClient"
    private val metrics = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
    private val meterRequests = metrics.meter("$metricsPrefix.requests")
    
    val id = instanceSequencer.incrementAndGet()
    override val isClosed: Boolean get() = closed.get()
    
    private val client = HttpClient {
        install(WebSockets)
    }
    private lateinit var session: DefaultClientWebSocketSession
    
    override suspend fun connect(uri: URI) {
        session = client.webSocketSession (method = HttpMethod.Get, host = uri.host, port = uri.port, path = uri.path)
    }
    
    override suspend fun send(message: String): String? {
        session.send(Frame.Text(message))
        
        // Retrieves and removes an element from this channel if it's not empty,
        // or suspends the caller while the channel is empty, or throws exception if the channel is closed.
        val frame = session.incoming.receive()
        
        return if (frame is Frame.Text) {
            frame.readText()
        } else null
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { client.close() }.onFailure { warnForClose(this, it) }
        }
    }
}
