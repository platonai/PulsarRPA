package ai.platon.pulsar.browser.driver.ws.client

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.consumeAsFlow
import java.net.URI
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger

class Client2: AutoCloseable {
    private val client = HttpClient {
        install(WebSockets)
    }
    
    private lateinit var session: DefaultClientWebSocketSession
    
    suspend fun connect(uri: URI) {
        session = client.webSocketSession (method = HttpMethod.Get, host = uri.host, port = uri.port, path = uri.path)
    }
    
    suspend fun sendDeferred(message: String): String? {
        session.send(Frame.Text(message))
        
        val frame = session.incoming.receive()
        val threadId = Thread.currentThread().id
        return if (frame is Frame.Text) {
            " <$threadId> " + frame.readText()
        } else null
    }
    
    override fun close() {
        client.close()
    }
}

fun main() = runBlocking {
    Client2().use { client ->
        val uri = URI("ws://localhost:8080/chat")
        client.connect(uri)
        val i = AtomicInteger()
        while (true) {
            repeat(10) {
                i.incrementAndGet()
                val threadId = Thread.currentThread().id
                val messageSend = "[$threadId] Hello, world $i!"

                val deferred = async { client.sendDeferred(messageSend) }
                // Awaits for completion of this value ** WITHOUT BLOCKING ** a thread and resumes when deferred computation
                // is complete, returning the resulting value or throwing the corresponding exception if the deferred
                // was cancelled.
                val response = deferred.await()

                val messageReceived = "[$threadId] Received: $response ||| $messageSend"
                println(messageReceived)
            }
            delay(2000)
        }
    }
}
