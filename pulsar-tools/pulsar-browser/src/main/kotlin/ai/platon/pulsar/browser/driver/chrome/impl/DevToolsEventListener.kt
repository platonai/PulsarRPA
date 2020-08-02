package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.function.Consumer

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
