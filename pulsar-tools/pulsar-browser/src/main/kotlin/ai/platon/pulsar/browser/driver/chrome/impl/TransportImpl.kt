package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.DefaultWebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.Transport
import ai.platon.pulsar.browser.driver.chrome.WebSocketContainerFactory
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.warnForClose
import com.codahale.metrics.SharedMetricRegistries
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import javax.websocket.*

class TransportImpl : Transport {
    private val logger = LoggerFactory.getLogger(TransportImpl::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()
    
    private lateinit var session: Session
    private val metricsPrefix = "c.i.WebSocketClient"
    private val metrics = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
    private val meterRequests = metrics.meter("$metricsPrefix.requests")
    
    val id = instanceSequencer.incrementAndGet()
    override val isClosed: Boolean get() = !session.isOpen || closed.get()
    
    class DevToolsMessageHandler(val consumer: Consumer<String>) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            consumer.accept(message)
        }
    }
    
    /**
     * Connect the supplied annotated endpoint instance to its server. The supplied
     * object must be a class decorated with the class level.
     *
     * @throws WebSocketServiceException if the annotated endpoint instance is not valid,
     * or if there was a network or protocol problem that prevented the client endpoint
     * being connected to its server.
     * @throws IllegalStateException if called during the deployment phase of the containing
     * application.
     * */
    @Throws(WebSocketServiceException::class, IllegalStateException::class)
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
                webSocketService.onError(session, e)
            }
        }
        
        session = try {
            WEB_SOCKET_CONTAINER.connectToServer(endpoint, uri)
        } catch (e: DeploymentException) {
            logger.warn("Failed to connect to ws server | $uri", e)
            throw WebSocketServiceException("Failed connecting to ws server | $uri", e)
        } catch (e: IOException) {
            logger.warn("Failed to connect to ws server | $uri", e)
            throw WebSocketServiceException("Failed connecting to ws server | $uri", e)
        }
    }
    
    @Throws(WebSocketServiceException::class)
    override fun send(message: String) {
        meterRequests.mark()
        
        try {
            tracer?.trace("Send {}", StringUtils.abbreviateMiddle(message, "...", 500))
            session.basicRemote.sendText(message)
        } catch (e: IOException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: IllegalArgumentException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: java.lang.IllegalStateException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: Exception) {
            logger.error("[Unexpected] | ${session.requestURI}", e)
        }
    }
    
    @Throws(WebSocketServiceException::class)
    override fun sendAsync(message: String): Future<Void> {
        meterRequests.mark()
        
        return try {
            tracer?.trace("Send {}", StringUtils.abbreviateMiddle(message, "...", 500))
            session.asyncRemote.sendText(message)
        } catch (e: IOException) {
            throw WebSocketServiceException("The connection is closed", e)
        } catch (e: IllegalArgumentException) {
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
        
        session.addMessageHandler(DevToolsMessageHandler(consumer))
    }
    
    private fun onOpen(session: Session, config: EndpointConfig) {
        logger.info("Connected to ws {}", session.requestURI)
    }
    
    private fun onClose(session: Session, closeReason: CloseReason) {
        logger.info("Web socket connection closed {} {}", closeReason.closeCode, closeReason.reasonPhrase)
        
        if (WebSocketUtils.isTyrusBufferOverflowCloseReason(closeReason)) {
            logger.error(
                "Web socket connection closed due to BufferOverflow raised by Tyrus client. This indicates the message "
                    + "about to be received is larger than the incoming buffer in Tyrus client. "
                    + "See DefaultWebSocketContainerFactory class source on how to increase the incoming buffer size in Tyrus or visit https://github.com/kklisura/chrome-devtools-java-client/blob/master/cdt-examples/src/main/java/com/github/kklisura/cdt/examples/IncreasedIncomingBufferInTyrusExample.java"
            )
        }
    }
    
    private fun onError(session: Session, e: Throwable?) {
        logger.error("WS session error | {}\n>>>{}<<<", session.requestURI, e?.brief())
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            // Close the current conversation with a normal status code and no reason phrase.
            session.runCatching { close() }.onFailure { warnForClose(this, it) }
        }
    }
    
    override fun toString(): String {
        return session.requestURI.toString()
    }
    
    companion object {
        val instanceSequencer = AtomicInteger()
        private const val WEB_SOCKET_CONTAINER_FACTORY_PROPERTY =
            "ai.platon.pulsar.browser.driver.chrome.webSocketContainerFactory"
        private val DEFAULT_WEB_SOCKET_CONTAINER_FACTORY = DefaultWebSocketContainerFactory::class.java.name
        val WEB_SOCKET_CONTAINER = createWebSocketContainer()
        
        /**
         * Creates a WebSocketService and connects to a specified uri.
         *
         * @param uri URI to connect to.
         * @return WebSocketService implementation.
         * @throws WebSocketServiceException If it fails to connect.
         */
        @Throws(WebSocketServiceException::class)
        fun create(uri: URI): Transport {
            return TransportImpl().also { it.connect(uri) }
        }
        
        /**
         * Returns a WebSocketContainer retrieved from class defined in system property
         * ai.platon.pulsar.browser.driver.chrome.webSocketContainerFactory.
         * The default value is GrizzlyContainerProvider.
         *
         * @return WebSocketContainer.
         */
        private fun createWebSocketContainer(): WebSocketContainer {
            val className =
                System.getProperty(WEB_SOCKET_CONTAINER_FACTORY_PROPERTY, DEFAULT_WEB_SOCKET_CONTAINER_FACTORY)
            
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
