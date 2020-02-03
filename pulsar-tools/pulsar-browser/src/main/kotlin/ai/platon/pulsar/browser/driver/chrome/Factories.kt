package ai.platon.pulsar.browser.driver.chrome

import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer
import javax.websocket.WebSocketContainer

@FunctionalInterface
interface WebSocketServiceFactory {
    @Throws(WebSocketServiceException::class)
    fun createWebSocketService(wsUrl: String): WebSocketService
}

@FunctionalInterface
interface WebSocketContainerFactory {
    val webSocketContainer: WebSocketContainer
}

class DefaultWebSocketContainerFactory : WebSocketContainerFactory {
    override val webSocketContainer
        get() = ClientManager.createClient(GrizzlyClientContainer::class.java.name)
}
