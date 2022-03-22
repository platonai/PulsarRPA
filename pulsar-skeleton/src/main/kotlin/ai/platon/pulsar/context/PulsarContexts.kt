package ai.platon.pulsar.context

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.context.support.BasicPulsarContext
import ai.platon.pulsar.context.support.ClassPathXmlPulsarContext
import ai.platon.pulsar.context.support.StaticPulsarContext
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

object PulsarContexts {
    private val logger = getLogger(this)
    private val contexts = mutableSetOf<PulsarContext>()
    var activeContext: PulsarContext? = null
        private set

    @Synchronized
    fun activate() = activeContext ?: activate(StaticPulsarContext.create())

    @Synchronized
    fun activate(context: PulsarContext): PulsarContext {
        val activated = activeContext
        if (activated != null && activated::class == context::class) {
            logger.info("Context is already activated | {}", activated::class)
            return activated
        }

        contexts.add(context)
        activeContext = context
        context.registerShutdownHook()
        logger.info("Active context | {}", contexts.joinToString { it::class.qualifiedName + "#" + it.id })
        return context
    }

    @Synchronized
    fun activate(contextLocation: String) = activate(ClassPathXmlPulsarContext(contextLocation))

    @Synchronized
    fun activate(applicationContext: ApplicationContext) =
        activate(BasicPulsarContext(applicationContext as AbstractApplicationContext))

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
