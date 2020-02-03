package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.ChromeDevToolsService
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener

class EventListenerImpl(
        val key: String,
        val handler: EventHandler<Any>,
        val paramType: Class<*>,
        private val service: ChromeDevToolsService
) : EventListener {
    override fun off() {
        unsubscribe()
    }

    override fun unsubscribe() {
        service.removeEventListener(this)
    }
}
