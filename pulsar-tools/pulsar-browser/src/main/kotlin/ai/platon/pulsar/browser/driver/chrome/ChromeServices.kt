package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.ChromeIOException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import com.github.kklisura.cdt.protocol.v2023.ChromeDevTools
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener
import java.net.URI
import java.time.Instant
import java.util.concurrent.Future
import java.util.function.Consumer

interface Transport: AutoCloseable {
    val isOpen: Boolean
    
    @Throws(ChromeIOException::class)
    fun connect(uri: URI)
    
    @Throws(ChromeIOException::class)
    fun send(message: String)
    
    @Throws(ChromeIOException::class)
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

    fun canConnect(): Boolean
    
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
    
    @Throws(ChromeServiceException::class)
    fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools
}

interface RemoteDevTools: ChromeDevTools, AutoCloseable {

    val isOpen: Boolean

    val lastSentTime: Instant?

    val lastReceivedTime: Instant?

    @Throws(ChromeIOException::class, ChromeRPCException::class)
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
