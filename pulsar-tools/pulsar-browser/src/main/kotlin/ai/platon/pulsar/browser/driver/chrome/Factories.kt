package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer
import javax.websocket.WebSocketContainer

@FunctionalInterface
interface WebSocketServiceFactory {
    @Throws(WebSocketServiceException::class)
    fun createWebSocketService(wsUrl: String): WebSocketClient
}

@FunctionalInterface
interface WebSocketContainerFactory {
    val wsContainer: WebSocketContainer
}

class DefaultWebSocketContainerFactory : WebSocketContainerFactory {
    override val wsContainer get() = ClientManager.createClient(GrizzlyClientContainer::class.java.name)
    // override val wsContainer get() = ClientManager.createClient()
}
