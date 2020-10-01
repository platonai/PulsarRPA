package ai.platon.pulsar.context.support

import org.springframework.context.support.AbstractApplicationContext

/**
 * Main entry point for Pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
open class GenericPulsarContext(
        override val applicationContext: AbstractApplicationContext
): AbstractPulsarContext(applicationContext)
