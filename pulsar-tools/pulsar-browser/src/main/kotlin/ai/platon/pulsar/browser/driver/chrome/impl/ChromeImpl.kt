package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import ai.platon.pulsar.browser.driver.chrome.util.ProxyClasses
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
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

    private val logger = getLogger(this)
    private val objectMapper = ObjectMapper()
    private val remoteDevTools: MutableMap<String, RemoteDevTools> = ConcurrentHashMap()
    private val closed = AtomicBoolean()

    override val version: ChromeVersion by lazy { refreshVersion() }

    constructor(host: String, port: Int): this(host, port, object: WebSocketServiceFactory {
        override fun createWebSocketService(wsUrl: String): Transport {
            return TransportImpl.create(URI.create(wsUrl))
        }
    })

    constructor(port: Int): this(LOCALHOST, port)

    @Throws(ChromeServiceException::class)
    override fun listTabs(): Array<ChromeTab> {
        return request(Array<ChromeTab>::class.java, "http://%s:%d/%s", host, port, LIST_TABS)
            ?: throw ChromeServiceException("Failed to list tabs")
    }

    @Throws(ChromeServiceException::class)
    override fun createTab(): ChromeTab {
        return createTab(ABOUT_BLANK_PAGE)
    }

    @Throws(ChromeServiceException::class)
    override fun createTab(url: String): ChromeTab {
        return request(ChromeTab::class.java, "http://%s:%d/%s?%s", host, port, CREATE_TAB, url)
                ?: throw ChromeServiceException("Failed to create tab | $url")
    }

    @Throws(ChromeServiceException::class)
    override fun activateTab(tab: ChromeTab) {
        request(Void::class.java, "http://%s:%d/%s/%s", host, port, ACTIVATE_TAB, tab.id)
    }

    @Throws(ChromeServiceException::class)
    override fun closeTab(tab: ChromeTab) {
        request(Void::class.java, "http://%s:%d/%s/%s", host, port, CLOSE_TAB, tab.id)
        clearDevTools(tab)
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

    @Throws(ChromeServiceException::class)
    private fun refreshVersion(): ChromeVersion {
        return request(ChromeVersion::class.java, "http://%s:%d/%s", host, port, VERSION)
            ?: throw ChromeServiceException("Failed to get version")
    }

    private fun clearDevTools(tab: ChromeTab) {
        remoteDevTools.remove(tab.id)?.close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            remoteDevTools.values.forEach { kotlin.runCatching { it.close() }.onFailure { logger.warn(it.stringify()) } }
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
        val browserClient = wss.createWebSocketService(browserUrl)

        // Connect to a tab via web socket
        val debuggerUrl = tab.webSocketDebuggerUrl
                ?: throw WebSocketServiceException("Invalid web socket url to page")
        val pageClient = wss.createWebSocketService(debuggerUrl)

        // Create concrete dev tools instance from interface
        return ProxyClasses.createProxyFromAbstract(
                DevToolsImpl::class.java,
                arrayOf(Transport::class.java, Transport::class.java, DevToolsConfig::class.java),
                arrayOf(browserClient, pageClient, config),
                invocationHandler
        ).also { commandHandler.devTools = it }
    }

    /**
     * Sends a request and parses json response as type T.
     *
     * @param responseType Resulting class type.
     * @param path Path with optional params similar to String.formats params.
     * @param params Path params.
     * @param <T> Type of response type.
     * @return Response object.
     * @throws ChromeServiceException If sending request fails due to any reason.
    */
    @Throws(ChromeServiceException::class)
    private fun <T> request(responseType: Class<T>, path: String, vararg params: Any): T? {
        if (closed.get()) return null

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null

        try {
            val uri = URL(String.format(path, *params))
            connection = uri.openConnection() as HttpURLConnection
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
