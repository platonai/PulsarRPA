package ai.platon.pulsar.context

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.context.support.BasicPulsarContext
import ai.platon.pulsar.context.support.ClassPathXmlPulsarContext
import ai.platon.pulsar.context.support.GenericPulsarContext
import org.springframework.context.ApplicationContext

object PulsarContexts {
    private val contexts = mutableSetOf<PulsarContext>()
    var activeContext: PulsarContext? = null
        private set

    @Synchronized
    fun activate() = activate(BasicPulsarContext())

    @Synchronized
    fun activate(context: PulsarContext): PulsarContext {
        contexts.add(context)
        activeContext = context
        return context
    }

    @Synchronized
    fun createSession(): PulsarSession {
        val context = activeContext?: activate(BasicPulsarContext())
        return context.createSession()
    }

    @Synchronized
    fun shutdown() {
        contexts.forEach { it.close() }
        activeContext = null
    }
}

fun withContext(block: (context: PulsarContext) -> Unit) {
    PulsarContexts.activate(BasicPulsarContext()).use {
        block(it)
    }
}

fun withContext(contextLocation: String, block: (context: PulsarContext) -> Unit) {
    PulsarContexts.activate(ClassPathXmlPulsarContext(contextLocation)).use {
        block(it)
    }
}

fun withContext(applicationContext: ApplicationContext, block: (context: PulsarContext) -> Unit) {
    PulsarContexts.activate(GenericPulsarContext(applicationContext)).use {
        block(it)
    }
}
