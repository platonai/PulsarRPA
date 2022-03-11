package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.Systems
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer
import javax.websocket.WebSocketContainer

@FunctionalInterface
interface WebSocketServiceFactory {
    @Throws(WebSocketServiceException::class)
    fun createWebSocketService(wsUrl: String): Transport
}

@FunctionalInterface
interface WebSocketContainerFactory {
    val wsContainer: WebSocketContainer
}

class DefaultWebSocketContainerFactory : WebSocketContainerFactory {
    override val wsContainer: WebSocketContainer
        get() {
            val client = ClientManager.createClient(GrizzlyClientContainer::class.java.name)
            client.properties[INCOMING_BUFFER_SIZE_PROPERTY] = INCOMING_BUFFER_SIZE
            return client
        }

    companion object {
        const val WEBSOCKET_INCOMING_BUFFER_PROPERTY = "ai.platon.pulsar.browser.driver.chrome.incomingBuffer"
        const val KB = 1024
        const val MB = 1024 * KB
        private const val DEFAULT_INCOMING_BUFFER_SIZE = 8 * MB
        private val INCOMING_BUFFER_SIZE: Long =
            Systems.getProperty(WEBSOCKET_INCOMING_BUFFER_PROPERTY, DEFAULT_INCOMING_BUFFER_SIZE).toLong()
        const val INCOMING_BUFFER_SIZE_PROPERTY = "org.glassfish.tyrus.incomingBufferSize"
    }
}
