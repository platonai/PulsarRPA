package ai.platon.pulsar.context

import ai.platon.pulsar.context.support.BasicPulsarContext
import ai.platon.pulsar.context.support.ClassPathXmlPulsarContext
import ai.platon.pulsar.context.support.GenericPulsarContext
import org.springframework.context.ApplicationContext

object PulsarContexts {
    private val contexts = mutableSetOf<PulsarContext>()
    var activeContext: PulsarContext? = null
        private set

    @Synchronized
    fun activate() = activeContext ?: activate(BasicPulsarContext())

    @Synchronized
    fun activate(context: PulsarContext): PulsarContext {
        contexts.add(context)
        activeContext = context
        context.registerShutdownHook()
        return context
    }

    @Synchronized
    fun activate(applicationContext: ApplicationContext) = activate(GenericPulsarContext(applicationContext))

    @Synchronized
    fun createSession() = activate().createSession()

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
    PulsarContexts.activate(applicationContext).use {
        block(it)
    }
}
