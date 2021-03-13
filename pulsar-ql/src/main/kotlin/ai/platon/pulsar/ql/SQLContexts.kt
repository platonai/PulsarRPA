package ai.platon.pulsar.ql

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.context.support.AbstractPulsarContext

object SQLContexts {

    private val contexts = mutableSetOf<SQLContext>()
    var activeContext: SQLContext? = null
        private set

    @Synchronized
    fun activate() = activeContext ?: activate(PulsarContexts.activate() as AbstractPulsarContext)

    @Synchronized
    fun activate(pulsarContext: AbstractPulsarContext): SQLContext {
        if (activeContext?.pulsarContext == pulsarContext) return activeContext!!

        val context = SQLContext(pulsarContext)
        contexts.add(context)
        activeContext = context
        pulsarContext.registerClosable(context)
        return context
    }

    @Synchronized
    fun shutdown() {
        activeContext?.close()
        activeContext = null
    }
}
