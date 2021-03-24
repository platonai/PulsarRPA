package ai.platon.pulsar.context.support

import ai.platon.pulsar.BasicPulsarSession
import org.springframework.context.support.AbstractApplicationContext

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
open class BasicPulsarContext(
    applicationContext: AbstractApplicationContext
) : AbstractPulsarContext(applicationContext) {

    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}
