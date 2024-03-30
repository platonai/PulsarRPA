package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import ai.platon.pulsar.browser.driver.chrome.util.WebSocketServiceException
import com.github.kklisura.cdt.protocol.v2023.ChromeDevTools
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener
import java.net.URI
import java.util.concurrent.Future
import java.util.function.Consumer

interface Transport: AutoCloseable {
    val isClosed: Boolean
    
    @Throws(WebSocketServiceException::class)
    fun connect(uri: URI)
    
    @Throws(WebSocketServiceException::class)
    fun send(message: String)
    
    @Throws(WebSocketServiceException::class)
    fun sendAsync(message: String): Future<Void>
    
    fun addMessageHandler(consumer: Consumer<String>)
}

interface CoTransport: AutoCloseable {
    val isClosed: Boolean
    suspend fun connect(uri: URI)
    suspend fun send(message: String): String?
}

interface RemoteChrome: AutoCloseable {

    val isActive: Boolean
    
    val version: ChromeVersion

    @Throws(ChromeServiceException::class)
    fun listTabs(): Array<ChromeTab>

    @Throws(ChromeServiceException::class)
    fun createTab(): ChromeTab

    @Throws(ChromeServiceException::class)
    fun createTab(url: String): ChromeTab

    @Throws(ChromeServiceException::class)
    fun activateTab(tab: ChromeTab)

    @Throws(ChromeServiceException::class)
    fun closeTab(tab: ChromeTab)

    fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools
}

interface RemoteDevTools: ChromeDevTools, AutoCloseable {

    val isOpen: Boolean
    
    @Throws(InterruptedException::class)
    operator fun <T> invoke(
            returnProperty: String?,
            clazz: Class<T>,
            returnTypeClasses: Array<Class<out Any>>?,
            method: MethodInvocation
    ): T?
    
    @Throws(InterruptedException::class)
    fun awaitTermination()

    fun addEventListener(domainName: String, eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>): EventListener

    fun removeEventListener(eventListener: EventListener)
}

interface CoRemoteDevTools: ChromeDevTools, AutoCloseable {
    
    val isOpen: Boolean
    
    suspend operator fun <T> invoke(
        returnProperty: String?,
        clazz: Class<T>,
        returnTypeClasses: Array<Class<out Any>>?,
        method: MethodInvocation
    ): T?
    
    @Throws(InterruptedException::class)
    fun awaitTermination()
}
