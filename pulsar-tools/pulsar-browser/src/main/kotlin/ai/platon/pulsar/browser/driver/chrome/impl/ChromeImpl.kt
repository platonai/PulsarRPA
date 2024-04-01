package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import ai.platon.pulsar.browser.driver.chrome.util.ProxyClasses
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.warnForClose
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ChromeImpl(
        var host: String = LOCALHOST,
        var port: Int = 0,
        var wss: WebSocketServiceFactory
): RemoteChrome {
    companion object {
        const val ABOUT_BLANK_PAGE = "about:blank"
        const val LOCALHOST = "localhost"

        const val LIST_TABS = "json/list"
        const val CREATE_TAB = "json/new"
        const val ACTIVATE_TAB = "json/activate"
        const val CLOSE_TAB = "json/close"
        const val VERSION = "json/version"
    }

    enum class HttpMethod {
        CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE
    }

    private val logger = getLogger(this)
    private val objectMapper = ObjectMapper()
    /**
     * DevTools map, the key is the Chrome tab id.
     * */
    private val remoteDevTools = ConcurrentHashMap<String, RemoteDevTools>()
    private val closed = AtomicBoolean()
    
    override val isActive get() = !closed.get()

    private val _version: Lazy<ChromeVersion> = lazy { refreshVersion() }
    /**
     * The Chrome version.
     * */
    override val version get() = _version.value

    constructor(host: String, port: Int): this(host, port, object: WebSocketServiceFactory {
        override fun createWebSocketService(wsUrl: String): Transport {
            return TransportImpl.create(URI.create(wsUrl))
        }
    })

    constructor(port: Int): this(LOCALHOST, port)

    @Throws(ChromeServiceException::class)
    override fun listTabs(): Array<ChromeTab> {
        return try {
            request(Array<ChromeTab>::class.java, HttpMethod.GET, "http://%s:%d/%s", host, port, LIST_TABS)
                ?: throw ChromeServiceException("Failed to list tabs, unexpected null response")
        } catch (e: WebSocketServiceException) {
            if (isActive) {
                throw ChromeServiceException("Failed to list tabs", e)
            } else {
                arrayOf()
            }
        }
    }

    @Throws(ChromeServiceException::class)
    override fun createTab(): ChromeTab {
        return createTab(ABOUT_BLANK_PAGE)
    }

    @Throws(ChromeServiceException::class)
    override fun createTab(url: String): ChromeTab {
        try {
            val chromeTab = request(ChromeTab::class.java, HttpMethod.PUT, "http://%s:%d/%s?%s", host, port, CREATE_TAB, url)
                ?: throw ChromeServiceException("Failed to create tab, unexpected null response | $url")
            return chromeTab
        } catch (e: WebSocketServiceException) {
            throw ChromeServiceException("Failed to create tab | $url", e)
        }
    }

    @Throws(ChromeServiceException::class)
    override fun activateTab(tab: ChromeTab) {
        try {
            request(Void::class.java, HttpMethod.PUT, "http://%s:%d/%s/%s", host, port, ACTIVATE_TAB, tab.id)
        } catch (e: WebSocketServiceException) {
            throw ChromeServiceException("Failed to activate tab", e)
        }
    }

    @Throws(ChromeServiceException::class)
    override fun closeTab(tab: ChromeTab) {
        try {
            if (!isActive || !canConnect()) {
                return
            }
            request(Void::class.java, HttpMethod.PUT, "http://%s:%d/%s/%s", host, port, CLOSE_TAB, tab.id)
        } catch (e: WebSocketServiceException) {
            throw ChromeServiceException("Failed to close tab", e)
        }
    }

    @Throws(ChromeServiceException::class)
    @Synchronized
    override fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        return try {
            remoteDevTools.computeIfAbsent(tab.id) { createDevTools0(version, tab, config) }
        } catch (e: WebSocketServiceException) {
            throw ChromeServiceException("Failed connecting to tab web socket.", e)
        }
    }
    
    override fun canConnect(): Boolean {
        val url = URL("http://$host:$port")
        return NetUtil.testHttpNetwork(url)
    }
    
    @Throws(ChromeServiceException::class)
    private fun refreshVersion(): ChromeVersion {
        return request(ChromeVersion::class.java, HttpMethod.GET, "http://%s:%d/%s", host, port, VERSION)
            ?: throw ChromeServiceException("Failed to get version")
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (!canConnect()) {
                return
            }

            val devTools = remoteDevTools.values
            devTools.forEach { it.runCatching { close() }.onFailure { warnForClose(this, it) } }
            remoteDevTools.clear()
        }
    }
    
    @Throws(WebSocketServiceException::class)
    private fun createDevTools0(version: ChromeVersion, tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        // Create invocation handler
        val commandHandler = DevToolsInvocationHandler()
        val commands: MutableMap<Method, Any> = ConcurrentHashMap()
        val invocationHandler = InvocationHandler { _, method, _ ->
            commands.computeIfAbsent(method) { ProxyClasses.createProxy(method.returnType, commandHandler) }
        }

        val browserUrl = version.webSocketDebuggerUrl
            ?: throw WebSocketServiceException("Invalid web socket url to browser")
        val browserTransport = wss.createWebSocketService(browserUrl)

        // Connect to a tab via web socket
        val debuggerUrl = tab.webSocketDebuggerUrl
                ?: throw WebSocketServiceException("Invalid web socket url to page")
        val pageTransport = wss.createWebSocketService(debuggerUrl)

        // Create concrete dev tools instance from interface
        return ProxyClasses.createProxyFromAbstract(
                DevToolsImpl::class.java,
                arrayOf(Transport::class.java, Transport::class.java, DevToolsConfig::class.java),
                arrayOf(browserTransport, pageTransport, config),
                invocationHandler
        ).also { commandHandler.devTools = it }
    }

    /**
     * Sends a request and parses json response as type T.
     *
     * @param responseType Resulting class type.
     * @param path Path with optional params similar to String.format() params.
     * @param params Path params.
     * @param <T> Type of response type.
     * @return Response object.
     * @throws WebSocketServiceException If sending request fails due to any reason.
    */
    @Throws(WebSocketServiceException::class)
    private fun <T> request(
        responseType: Class<T>, method: HttpMethod, path: String, vararg params: Any
    ): T? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null

        try {
            val uri = URL(String.format(path, *params))
            
            connection = uri.openConnection() as HttpURLConnection
            
            /**
             * Chrome 111 no longer accepts HTTP GET to create tabs, PUT is the correct verb.
             *
             * Issue #14: Using unsafe HTTP verb GET to invoke /json/new. This action supports only PUT verb.
             * @see [#14](https://github.com/platonai/exotic-amazon/issues/14)
             *
             * Chrome-devtools-java-client issue #87: Doesn't work with chrome v111 #87
             * @see [#87](https://github.com/kklisura/chrome-devtools-java-client/issues/87)
             * */
            connection.requestMethod = method.toString()
            
            val responseCode = connection.responseCode
            if (HttpURLConnection.HTTP_OK == responseCode) {
                if (Void::class.java == responseType) {
                    return null
                }
                inputStream = connection.inputStream
                return objectMapper.readerFor(responseType).readValue(inputStream)
            } else {
                inputStream = connection.errorStream
                val responseBody = readString(inputStream)
                val message = "Received error ($responseCode) - ${connection.responseMessage}\n$responseBody"

                throw WebSocketServiceException(message)
            }
        } catch (ex: IOException) {
            throw ChromeServiceException("Failed sending HTTP request", ex)
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }

    /**
     * Converts input stream to string. If input string is null, it returns empty string.
     *
     * @param inputStream Input stream.
     * @return String
     * @throws IOException If conversion fails.
     */
    @Throws(IOException::class)
    private fun readString(inputStream: InputStream): String {
        var length: Int
        val buffer = ByteArray(1024)
        val result = ByteArrayOutputStream()
        while (inputStream.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString("UTF-8")
    }
}
