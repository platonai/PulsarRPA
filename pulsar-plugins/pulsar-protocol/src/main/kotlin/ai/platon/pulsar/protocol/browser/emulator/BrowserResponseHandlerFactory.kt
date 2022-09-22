package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.emulator.impl.BrowserResponseHandlerImpl

class BrowserResponseHandlerFactory(
        private val immutableConfig: ImmutableConfig
) {
    private val reflectedHandler by lazy {
        val clazz = immutableConfig.getClass(
                CapabilityTypes.BROWSER_RESPONSE_HANDLER, BrowserResponseHandlerImpl::class.java)
        clazz.constructors.first { it.parameters.size == 1 }
                .newInstance(immutableConfig) as BrowserResponseHandler
    }

    var specifiedHandler: BrowserResponseHandler? = null

    val eventHandler get() = specifiedHandler ?: reflectedHandler
}
