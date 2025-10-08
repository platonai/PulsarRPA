package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.ChromeIOException
import ai.platon.pulsar.common.Systems
import org.apache.commons.io.FileUtils
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer
import javax.websocket.WebSocketContainer

@FunctionalInterface
interface WebSocketServiceFactory {
    @Throws(ChromeIOException::class)
    fun createWebSocketService(wsUrl: String): Transport
}

interface WebSocketContainerFactory {
    val wsContainer: WebSocketContainer
}

class DefaultWebSocketContainerFactory : WebSocketContainerFactory {
    
    companion object {
        const val WEBSOCKET_INCOMING_BUFFER_PROPERTY = "ai.platon.pulsar.browser.driver.chrome.incomingBuffer"
        private const val DEFAULT_INCOMING_BUFFER_SIZE = 8 * FileUtils.ONE_MB
        private val INCOMING_BUFFER_SIZE = Systems.getProperty(WEBSOCKET_INCOMING_BUFFER_PROPERTY, DEFAULT_INCOMING_BUFFER_SIZE)
        const val INCOMING_BUFFER_SIZE_PROPERTY = "org.glassfish.tyrus.incomingBufferSize"
    }
    
    override val wsContainer: WebSocketContainer = createWebSocketContainer()

    private fun createWebSocketContainer(): ClientManager {
        val client = ClientManager.createClient(GrizzlyClientContainer::class.java.name)
        client.properties[INCOMING_BUFFER_SIZE_PROPERTY] = INCOMING_BUFFER_SIZE
        return client
    }
}
