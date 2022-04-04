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
    fun create(): PulsarContext {
        if (activeContext == null) {
            activeContext = create(StaticPulsarContext())
        }
        return activeContext!!
    }

    @Synchronized
    fun create(context: PulsarContext): PulsarContext {
        val activated = activeContext
        if (activated != null && activated::class == context::class) {
            logger.info("Context is already activated | {}", activated::class)
            return activated
        }

        contexts.add(context)
        activeContext = context
        context.registerShutdownHook()
        logger.info("Active context | {}", contexts.joinToString { it::class.qualifiedName + "#" + it.id })

        if (!context.crawlLoops.isStarted) {
            context.crawlLoops.start()
        }

        return context
    }

    @Synchronized
    fun create(contextLocation: String) = create(ClassPathXmlPulsarContext(contextLocation))

    @Synchronized
    fun create(applicationContext: ApplicationContext) =
        create(BasicPulsarContext(applicationContext as AbstractApplicationContext))

    @Synchronized
    fun createSession() = create().createSession()

    @Synchronized
    fun shutdown() {
        contexts.forEach { it.close() }
        activeContext = null
    }
}

fun withContext(block: (context: PulsarContext) -> Unit) {
    PulsarContexts.create(StaticPulsarContext()).use {
        block(it)
    }
}

fun withContext(contextLocation: String, block: (context: PulsarContext) -> Unit) {
    PulsarContexts.create(ClassPathXmlPulsarContext(contextLocation)).use {
        block(it)
    }
}

fun withContext(applicationContext: ApplicationContext, block: (context: PulsarContext) -> Unit) {
    PulsarContexts.create(applicationContext).use {
        block(it)
    }
}
