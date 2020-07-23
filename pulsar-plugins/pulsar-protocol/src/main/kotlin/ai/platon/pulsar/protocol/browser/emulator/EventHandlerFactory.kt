package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager

class EventHandlerFactory(
        private val driverPoolManager: WebDriverPoolManager,
        private val messageWriter: MiscMessageWriter,
        private val immutableConfig: ImmutableConfig
) {
    val eventHandler by lazy {
        val clazz = immutableConfig.getClass(
                CapabilityTypes.BROWSER_EMULATOR_EVENT_HANDLER, EventHandler::class.java)
        clazz.constructors.first { it.parameters.size == 3 }
                .newInstance(driverPoolManager, messageWriter, immutableConfig) as EventHandler
    }
}
