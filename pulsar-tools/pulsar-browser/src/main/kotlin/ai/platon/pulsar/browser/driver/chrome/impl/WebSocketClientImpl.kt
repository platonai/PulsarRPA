package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.DefaultWebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.WebSocketClient
import ai.platon.pulsar.browser.driver.chrome.WebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.simplify
import com.codahale.metrics.SharedMetricRegistries
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import javax.websocket.*

class WebSocketClientImpl : WebSocketClient {
    val id = instanceSequencer.incrementAndGet()

    private val logger = LoggerFactory.getLogger(WebSocketClientImpl::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    private lateinit var session: Session
    private val metricsPrefix = "c.i.WebSocketClient"
    private val metrics = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
    private val meterRequests = metrics.meter("$metricsPrefix.requests")

    class WSMessageHandler(val consumer: Consumer<String>): MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            consumer.accept(message)
        }
    }

    override fun isClosed(): Boolean {
        return !session.isOpen || closed.get()
    }

    @Throws(WebSocketServiceException::class)
    override fun connect(uri: URI) {

        val webSocketService = this

        val endpoint = object : Endpoint() {
            override fun onOpen(session: Session, config: EndpointConfig) {
                webSocketService.onOpen(session, config)
                logger.info("Connected to ws server {}", uri)
            }

            override fun onClose(session: Session, closeReason: CloseReason) {
                super.onClose(session, closeReason)
                webSocketService.onClose(session, closeReason)
                logger.info("Closing ws server {}", uri)
            }

            override fun onError(session: Session, e: Throwable?) {
                super.onError(session, e)
                e?.printStackTrace()
                webSocketService.onError(session, e)
            }
        }

        session = try {
            WEB_SOCKET_CONTAINER.connectToServer(endpoint, uri)
        } catch (e: DeploymentException) {
            logger.warn("Failed connecting to ws server | {}", uri, e)
            throw WebSocketServiceException("Failed connecting to ws server {}", e)
        } catch (e: IOException) {
            logger.warn("Failed connecting to ws server | {}", uri, e)
            throw WebSocketServiceException("Failed connecting to ws server {}", e)
        }
    }

    @Throws(WebSocketServiceException::class)
    override fun send(message: String) {
        meterRequests.mark()

        try {
            // TODO: use session.asyncRemote?
            // logger.info(message)
            session.basicRemote.sendText(message)
        } catch (e: IOException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: java.lang.IllegalStateException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: Exception) {
            logger.error("Unexpected exception | ${session.requestURI}", e)
        }
    }

    @Throws(WebSocketServiceException::class)
    override fun asyncSend(message: String): Future<Void> {
        meterRequests.mark()

        return try {
            session.asyncRemote.sendText(message)
        } catch (e: IOException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: java.lang.IllegalStateException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: Exception) {
            logger.error("Unexpected exception | ${session.requestURI}", e)
            throw e
        }
    }

    @Throws(WebSocketServiceException::class)
    override fun addMessageHandler(consumer: Consumer<String>) {
        if (session.messageHandlers.isNotEmpty()) {
            throw WebSocketServiceException("You are already subscribed to this web socket service.")
        }

        session.addMessageHandler(WSMessageHandler(consumer))
    }

    private fun onOpen(session: Session, config: EndpointConfig) {
        logger.info("Connected to ws {}", session.requestURI)
    }

    private fun onClose(session: Session, closeReason: CloseReason) {
        logger.info(
            "Web socket connection closed {}, {}",
            closeReason.closeCode,
            closeReason.reasonPhrase
        )

        if (WebSocketUtils.isTyrusBufferOverflowCloseReason(closeReason)) {
            logger.error(
                "Web socket connection closed due to BufferOverflow raised by Tyrus client. This indicates the message "
                        + "about to be received is larger than the incoming buffer in Tyrus client. "
                        + "See DefaultWebSocketContainerFactory class source on how to increase the incoming buffer size in Tyrus or visit https://github.com/kklisura/chrome-devtools-java-client/blob/master/cdt-examples/src/main/java/com/github/kklisura/cdt/examples/IncreasedIncomingBufferInTyrusExample.java"
            )
        }
    }

    private fun onError(session: Session, e: Throwable?) {
        logger.error("WS session error | {}\n>>>{}<<<", session.requestURI, e?.simplify())
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                session.close()
            } catch (e: IOException) {
                logger.error("Failed closing ws session", e)
            }
        }
    }

    override fun toString(): String {
        return session.requestURI.toString()
    }

    companion object {
        val instanceSequencer = AtomicInteger()
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
                return (Class.forName(className) as Class<WebSocketContainerFactory>).newInstance().wsContainer
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Could not create instance of $className class")
            } catch (e: InstantiationException) {
                throw RuntimeException("Could not create instance of $className class")
            }
        }
    }
}
