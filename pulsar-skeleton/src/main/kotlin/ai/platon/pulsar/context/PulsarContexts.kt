package ai.platon.pulsar.context

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.context.support.StaticPulsarContext
import ai.platon.pulsar.context.support.ClassPathXmlPulsarContext
import ai.platon.pulsar.context.support.GenericPulsarContext
import org.springframework.context.ApplicationContext

object PulsarContexts {
    private val contexts = mutableSetOf<PulsarContext>()
    var activeContext: PulsarContext? = null
        private set

    @Synchronized
    fun active(context: PulsarContext): PulsarContext {
        contexts.add(context)
        activeContext = context
        return context
    }

    @Synchronized
    fun createSession(): PulsarSession {
        val context = activeContext?: active(StaticPulsarContext())
        return context.createSession()
    }
}

fun withContext(block: (context: PulsarContext) -> Unit) {
    PulsarContexts.active(StaticPulsarContext()).use {
        block(it)
    }
}

fun withContext(contextLocation: String, block: (context: PulsarContext) -> Unit) {
    PulsarContexts.active(ClassPathXmlPulsarContext(contextLocation)).use {
        block(it)
    }
}

fun withContext(applicationContext: ApplicationContext, block: (context: PulsarContext) -> Unit) {
    PulsarContexts.active(GenericPulsarContext(applicationContext)).use {
        block(it)
    }
}
