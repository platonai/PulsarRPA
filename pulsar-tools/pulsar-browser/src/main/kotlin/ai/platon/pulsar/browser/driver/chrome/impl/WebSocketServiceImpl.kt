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
        return closed.get()
    }

    @Throws(WebSocketServiceException::class)
    override fun connect(uri: URI) {
        val endpoint = object : Endpoint() {
            override fun onOpen(session: Session, config: EndpointConfig) {
                log.info("Connected to ws server {}", uri)
            }

            override fun onClose(session: Session, closeReason: CloseReason) {
                log.info("Close ws server {}", uri)
            }
        }

        log.debug("Connecting to ws server {}", uri)
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
            log.trace("Sending {} | {}", message, session.requestURI)
            session.basicRemote.sendText(message)
        } catch (e: Exception) {
            log.error("Failed sending data to {}...", session.requestURI, e)
            throw WebSocketServiceException("Failed sending data to ws server.", e)
        }
    }

    @Throws(WebSocketServiceException::class)
    override fun addMessageHandler(consumer: Consumer<String>) {
        if (session.messageHandlers.isNotEmpty()) {
            throw WebSocketServiceException("You are already subscribed to this web socket service.")
        }

        val messageHandle = MessageHandler.Whole<String> { message ->
            log.trace("Received {} | {}", message, session.requestURI)
            consumer.accept(message)
        }

        session.addMessageHandler(messageHandle)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                log.debug("Closing WebSocketService")

                session.close()
            } catch (e: IOException) {
                log.error("Failed closing ws session on {}...", session.getRequestURI(), e)
            }
        }
    }

    companion object {
        const val WEB_SOCKET_CONTAINER_FACTORY_PROPERTY = "services.config.webSocketContainerFactory"
        private val DEFAULT_WEB_SOCKET_CONTAINER_FACTORY: String = DefaultWebSocketContainerFactory::class.java.getName()
        private val log = LoggerFactory.getLogger(WebSocketServiceImpl::class.java)
        private val WEB_SOCKET_CONTAINER: WebSocketContainer = webSocketContainer
        /**
         * Creates a WebSocketService and connects to a specified uri.
         *
         * @param uri URI to connect to.
         * @return WebSocketService implementation.
         * @throws WebSocketServiceException If it fails to connect.
         */
        @Throws(WebSocketServiceException::class)
        fun create(uri: URI): WebSocketService {
            val webSocketService = WebSocketServiceImpl()
            webSocketService.connect(uri)
            return webSocketService
        }

        /**
         * Returns a WebSocketContainer retrieved from class defined in system property
         * com.github.kklisura.cdt.services.config.webSocketContainerProvider. The default value for this
         * property is GrizzlyContainerProvider class FQN.
         *
         * @return WebSocketContainer.
         */
        private val webSocketContainer: WebSocketContainer
            get() {
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
