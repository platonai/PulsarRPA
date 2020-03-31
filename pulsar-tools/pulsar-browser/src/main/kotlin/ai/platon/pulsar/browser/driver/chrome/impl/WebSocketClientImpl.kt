package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.DefaultWebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.WebSocketClient
import ai.platon.pulsar.browser.driver.chrome.WebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.websocket.*

class WebSocketClientImpl : WebSocketClient {
    private val log = LoggerFactory.getLogger(WebSocketClientImpl::class.java)
    private val closed = AtomicBoolean()
    private lateinit var session: Session

    override fun isClosed(): Boolean {
        return !session.isOpen || closed.get()
    }

    @Throws(WebSocketServiceException::class)
    override fun connect(uri: URI) {
        val endpoint = object : Endpoint() {
            override fun onOpen(session: Session, config: EndpointConfig) {
                log.info("Connected to ws server {}", uri)
            }

            override fun onClose(session: Session, closeReason: CloseReason) {
                // log.info("Closing ws server {}")
            }
        }

        // log.debug("Connecting to ws server | {}", uri)

        session = try {
            WEB_SOCKET_CONTAINER.connectToServer(endpoint, uri)
        } catch (e: DeploymentException) {
            log.warn("Failed connecting to ws server | {}", uri, e)
            throw WebSocketServiceException("Failed connecting to ws server {}", e)
        } catch (e: IOException) {
            log.warn("Failed connecting to ws server | {}", uri, e)
            throw WebSocketServiceException("Failed connecting to ws server {}", e)
        }
    }

    @Throws(WebSocketServiceException::class)
    override fun send(message: String) {
        try {
            if (log.isTraceEnabled) {
                log.trace("Sending {} | {}", message, session.requestURI)
            }

            session.basicRemote.sendText(message)
        } catch (e: IOException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: java.lang.IllegalStateException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: Exception) {
            log.error("Unexpected exception | ${session.requestURI}", e)
        }
    }

    @Throws(WebSocketServiceException::class)
    override fun addMessageHandler(consumer: Consumer<String>) {
        if (session.messageHandlers.isNotEmpty()) {
            throw WebSocketServiceException("You are already subscribed to this web socket service.")
        }

        val messageHandle = MessageHandler.Whole<String> { message ->
            if (log.isTraceEnabled) {
                log.trace("Received {} | {}", message, session.requestURI)
            }
            consumer.accept(message)
        }

        session.addMessageHandler(messageHandle)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                session.close()
            } catch (e: IOException) {
                log.error("Failed closing ws session on ${session.requestURI} ...", e)
            }
        }
    }

    companion object {
        private const val WEB_SOCKET_CONTAINER_FACTORY_PROPERTY = "ai.platon.pulsar.browser.driver.chrome.webSocketContainerFactory"
        private val DEFAULT_WEB_SOCKET_CONTAINER_FACTORY = DefaultWebSocketContainerFactory::class.java.getName()
        private val WEB_SOCKET_CONTAINER = createWebSocketContainer()
        /**
         * Creates a WebSocketService and connects to a specified uri.
         *
         * @param uri URI to connect to.
         * @return WebSocketService implementation.
         * @throws WebSocketServiceException If it fails to connect.
         */
        @Throws(WebSocketServiceException::class)
        fun create(uri: URI): WebSocketClient {
            return WebSocketClientImpl().also { it.connect(uri) }
        }

        /**
         * Returns a WebSocketContainer retrieved from class defined in system property
         * ai.platon.pulsar.browser.driver.chrome.webSocketContainerFactory.
         * The default value is GrizzlyContainerProvider.
         *
         * @return WebSocketContainer.
         */
        private fun createWebSocketContainer(): WebSocketContainer {
            val className = System.getProperty(WEB_SOCKET_CONTAINER_FACTORY_PROPERTY, DEFAULT_WEB_SOCKET_CONTAINER_FACTORY)

            try {
                return (Class.forName(className) as Class<WebSocketContainerFactory>).newInstance().webSocketContainer
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Could not create instance of $className class")
            } catch (e: InstantiationException) {
                throw RuntimeException("Could not create instance of $className class")
            }
        }
    }
}
