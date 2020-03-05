package ai.platon.pulsar.browser.driver.chrome

import com.github.kklisura.cdt.protocol.ChromeDevTools
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener
import java.net.URI
import java.time.Duration
import java.util.concurrent.Executors
import java.util.function.Consumer

interface WebSocketService: AutoCloseable {
    @Throws(WebSocketServiceException::class)
    fun connect(uri: URI)
    fun send(message: String)
    fun addMessageHandler(consumer: Consumer<String>)
    fun isClosed(): Boolean
}

interface EventExecutorService: AutoCloseable {
    fun execute(runnable: Runnable)
}

class DevToolsServiceConfig {
    var readTimeout = Duration.ofMinutes(READ_TIMEOUT_MINUTES)
    var eventExecutorService = DefaultEventExecutorService()

    companion object {
        private const val READ_TIMEOUT_PROPERTY = "chrome.browser.services.config.readTimeout"
        private val READ_TIMEOUT_MINUTES = System.getProperty(READ_TIMEOUT_PROPERTY, "0").toLong()
    }
}

interface ChromeService: AutoCloseable {
    fun getTabs(): List<ChromeTab>

    fun createTab(): ChromeTab

    fun createTab(url: String): ChromeTab

    fun activateTab(tab: ChromeTab)

    fun closeTab(tab: ChromeTab)

    fun getVersion(): ChromeVersion

    fun createDevToolsService(tab: ChromeTab, config: DevToolsServiceConfig): ChromeDevToolsService

    fun createDevToolsService(tab: ChromeTab): ChromeDevToolsService
}

interface ChromeDevToolsService: ChromeDevTools, AutoCloseable {

    val isOpen: Boolean

    operator fun <T> invoke(
            returnProperty: String?,
            clazz: Class<T>,
            returnTypeClasses: Array<Class<out Any>>?,
            methodInvocation: MethodInvocation
    ): T?

    fun waitUntilClosed()

    fun addEventListener(domainName: String, eventName: String, eventHandler: EventHandler<Any>, eventType: Class<*>): EventListener

    fun removeEventListener(eventListener: EventListener)
}

class DefaultEventExecutorService : EventExecutorService {
    private val executorService = Executors.newWorkStealingPool()

    override fun execute(runnable: Runnable) {
        executorService.execute(runnable)
    }

    override fun close() {
        executorService.shutdown()
    }
}
