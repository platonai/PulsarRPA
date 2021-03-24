package ai.platon.pulsar.context

import ai.platon.pulsar.context.support.BasicPulsarContext
import ai.platon.pulsar.context.support.ClassPathXmlPulsarContext
import ai.platon.pulsar.context.support.StaticPulsarContext
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

object PulsarContexts {
    private val contexts = mutableSetOf<PulsarContext>()
    private var activeContext: PulsarContext? = null

    @Synchronized
    fun activate() = activeContext ?: activate(StaticPulsarContext())

    @Synchronized
    fun activate(context: PulsarContext): PulsarContext {
        contexts.add(context)
        activeContext = context
        context.registerShutdownHook()
        return context
    }

    @Synchronized
    fun activate(applicationContext: ApplicationContext)
            = activate(BasicPulsarContext(applicationContext as AbstractApplicationContext))

    @Synchronized
    fun createSession() = activate().createSession()

    @Synchronized
    fun shutdown() {
        contexts.forEach { it.close() }
        activeContext = null
    }
}

fun withContext(block: (context: PulsarContext) -> Unit) {
    PulsarContexts.activate(StaticPulsarContext()).use {
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
