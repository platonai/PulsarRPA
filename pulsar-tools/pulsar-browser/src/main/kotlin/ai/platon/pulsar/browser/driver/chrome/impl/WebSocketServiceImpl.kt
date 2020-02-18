package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.DefaultWebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.WebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.WebSocketService
import ai.platon.pulsar.browser.driver.chrome.WebSocketServiceException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.websocket.*

class WebSocketServiceImpl : WebSocketService {
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

        // log.debug("Connecting to ws server {}", uri)
        session = try {
            WEB_SOCKET_CONTAINER.connectToServer(endpoint, uri)
        } catch (e: DeploymentException) {
            log.warn("Failed connecting to ws server {}...", uri, e)
            throw WebSocketServiceException("Failed connecting to ws server {}", e)
        } catch (e: IOException) {
            log.warn("Failed connecting to ws server {}...", uri, e)
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
        } catch (e: IllegalStateException) {
            // log.error("The connection has been closed | {}", session.requestURI)
            throw WebSocketServiceException("The connection has been closed", e)
        } catch (e: Exception) {
            log.error("Unexpected exception {}", session.requestURI)
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
                // log.debug("Closing web socket service session | {}", session.requestURI)
                session.close()
            } catch (e: IOException) {
                log.error("Failed closing ws session on ${session.requestURI} ...", e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WebSocketServiceImpl::class.java)

        private const val WEB_SOCKET_CONTAINER_FACTORY_PROPERTY = "services.config.webSocketContainerFactory"
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
        fun create(uri: URI): WebSocketService {
            return WebSocketServiceImpl().also { it.connect(uri) }
        }

        /**
         * Returns a WebSocketContainer retrieved from class defined in system property
         * com.github.kklisura.cdt.services.config.webSocketContainerProvider. The default value for this
         * property is GrizzlyContainerProvider class FQN.
         *
         * @return WebSocketContainer.
         */
        private fun createWebSocketContainer(): WebSocketContainer {
            val containerFactoryClassName = System.getProperty(WEB_SOCKET_CONTAINER_FACTORY_PROPERTY, DEFAULT_WEB_SOCKET_CONTAINER_FACTORY)

            try {
                val containerFactoryClass: Class<WebSocketContainerFactory> = Class.forName(containerFactoryClassName) as Class<WebSocketContainerFactory>
                if (WebSocketContainerFactory::class.java.isAssignableFrom(containerFactoryClass)) {
                    val containerFactory: WebSocketContainerFactory = containerFactoryClass.newInstance()
                    return containerFactory.webSocketContainer
                }
                throw RuntimeException("$containerFactoryClassName is not a WebSocketContainerFactory")
            } catch (e: ClassNotFoundException) {
                throw RuntimeException("$containerFactoryClassName class not found.", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Could not create instance of $containerFactoryClassName class")
            } catch (e: InstantiationException) {
                throw RuntimeException("Could not create instance of $containerFactoryClassName class")
            }
        }
    }
}
