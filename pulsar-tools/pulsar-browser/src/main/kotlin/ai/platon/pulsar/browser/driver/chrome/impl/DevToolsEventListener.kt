package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener

class DevToolsEventListener(
        val key: String,
        val handler: EventHandler<Any>,
        val paramType: Class<*>,
        private val devTools: RemoteDevTools
): EventListener, Comparable<DevToolsEventListener> {
    override fun off() {
        unsubscribe()
    }

    override fun unsubscribe() {
        devTools.removeEventListener(this)
    }

    override fun compareTo(other: DevToolsEventListener): Int {
        return this.key.compareTo(other.key)
    }
}
