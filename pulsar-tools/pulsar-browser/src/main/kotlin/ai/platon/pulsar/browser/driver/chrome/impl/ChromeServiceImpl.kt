package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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

class ChromeServiceImpl(
        var host: String = LOCALHOST,
        var port: Int = 0,
        var wss: WebSocketServiceFactory
): ChromeService {
    companion object {
        const val ABOUT_BLANK_PAGE = "about:blank"
        const val LOCALHOST = "localhost"

        const val LIST_TABS = "json/list"
        const val CREATE_TAB = "json/new"
        const val ACTIVATE_TAB = "json/activate"
        const val CLOSE_TAB = "json/close"
        const val VERSION = "json/version"
    }

    private val LOG = LoggerFactory.getLogger(ChromeServiceImpl::class.java)

    private val OBJECT_MAPPER = ObjectMapper()

    private val devToolsServices: MutableMap<String, ChromeDevToolsService> = ConcurrentHashMap()
    private val closed = AtomicBoolean()

    constructor(host: String, port: Int): this(host, port, object: WebSocketServiceFactory {
        override fun createWebSocketService(wsUrl: String): WebSocketService {
            return WebSocketServiceImpl.create(URI.create(wsUrl))
        }
    })

    constructor(port: Int): this(LOCALHOST, port)

    override fun getTabs(): List<ChromeTab> {
        return request(Array<ChromeTab>::class.java, "http://%s:%d/%s", host, port, LIST_TABS)?.toList() ?: listOf()
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
        clearDevToolsServices(tab)
    }

    @Throws(ChromeServiceException::class)
    override fun getVersion(): ChromeVersion {
        return request(ChromeVersion::class.java, "http://%s:%d/%s", host, port, VERSION)
                ?: throw ChromeServiceException("Failed to get version")
    }

    @Throws(ChromeServiceException::class)
    @Synchronized
    override fun createDevToolsService(tab: ChromeTab, config: DevToolsServiceConfig): ChromeDevToolsService {
        return try {
            devToolsServices.computeIfAbsent(tab.id) { createDevToolsServiceInternal(tab, config) }
        } catch (e: WebSocketServiceException) {
            throw ChromeServiceException("Failed connecting to tab web socket.", e)
        }
    }

    @Synchronized
    override fun createDevToolsService(tab: ChromeTab): ChromeDevToolsService {
        return createDevToolsService(tab, DevToolsServiceConfig())
    }

    private fun clearDevToolsServices(tab: ChromeTab) {
        devToolsServices.remove(tab.id)?.close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    @Throws(WebSocketServiceException::class)
    private fun createDevToolsServiceInternal(tab: ChromeTab, config: DevToolsServiceConfig): ChromeDevToolsService {
        // Create invocation handler
        val commandInvocationHandler = CommandInvocationHandler()
        val commandsCache: MutableMap<Method, Any> = ConcurrentHashMap()
        val invocationHandler = InvocationHandler { _, method, _ ->
            commandsCache.computeIfAbsent(method) {
                ProxyClasses.createProxy(method.returnType, commandInvocationHandler)
            }
        }

        // Connect to a tab via web socket
        val webSocketDebuggerUrl: String = tab.webSocketDebuggerUrl
                ?:throw WebSocketServiceException("Invalid web socket debugger url")
        val wsService = wss.createWebSocketService(webSocketDebuggerUrl)

        // Create dev tools service.
        return ProxyClasses.createProxyFromAbstract(
                ChromeDevToolsServiceImpl::class.java,
                arrayOf(WebSocketService::class.java, DevToolsServiceConfig::class.java),
                arrayOf(wsService, config),
                invocationHandler
        ).also { commandInvocationHandler.chromeDevToolsService = it }
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
                return OBJECT_MAPPER.readerFor(responseType).readValue(inputStream)
            } else {
                inputStream = connection.errorStream
                val responseBody = readString(inputStream)
                val message = "Responded error $responseCode - ${connection.responseMessage} | $responseBody"
                throw ChromeServiceException(message)
            }
        } catch (ex: IOException) {
            throw ChromeServiceException("Failed sending HTTP request", ex)
        } finally {
            inputStream?.use { it.close() }
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