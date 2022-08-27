package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import com.github.kklisura.cdt.protocol.ChromeDevTools
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener
import java.net.URI
import java.util.concurrent.Future
import java.util.function.Consumer
import kotlin.jvm.Throws

interface Transport: AutoCloseable {
    fun connect(uri: URI)
    fun send(message: String)
    fun asyncSend(message: String): Future<Void>
    fun addMessageHandler(consumer: Consumer<String>)
    fun isClosed(): Boolean
}

interface RemoteChrome: AutoCloseable {

    val version: ChromeVersion

    @Throws(ChromeServiceException::class)
    fun getTabs(): Array<ChromeTab>

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

    operator fun <T> invoke(
            returnProperty: String?,
            clazz: Class<T>,
            returnTypeClasses: Array<Class<out Any>>?,
            method: MethodInvocation
    ): T?

    fun waitUntilClosed()

    fun addEventListener(domainName: String, eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>): EventListener

    fun removeEventListener(eventListener: EventListener)
}
