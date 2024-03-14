package ai.platon.pulsar.context

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.context.support.AbstractPulsarContext
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
    @JvmStatic
    fun create(): PulsarContext {
        if (activeContext == null) {
            activeContext = create(StaticPulsarContext())
        }
        return activeContext!!
    }

    @Synchronized
    @JvmStatic
    fun create(context: PulsarContext): PulsarContext {
        val activated = activeContext
        if (activated != null && activated::class == context::class) {
            logger.info("Context is already activated | {}", activated::class)
            return activated
        }

        contexts.add(context)
        activeContext = context

        // TODO: do not call getBean in close() function, it's better to close pulsar context before application context.
        // TODO: The order of registered shutdown hooks is not guaranteed.
        (context as? AbstractPulsarContext)?.applicationContext?.registerShutdownHook()
        context.registerShutdownHook()
        logger.info("Active context | {}", contexts.joinToString { it::class.qualifiedName + "#" + it.id })

        return context
    }

    @Synchronized
    @JvmStatic
    fun create(contextLocation: String) = create(ClassPathXmlPulsarContext(contextLocation))

    @Synchronized
    @JvmStatic
    fun create(applicationContext: ApplicationContext) =
        create(BasicPulsarContext(applicationContext as AbstractApplicationContext))

    @Synchronized
    @JvmStatic
    fun createSession() = create().createSession()

    @JvmStatic
    @Throws(InterruptedException::class)
    fun await() {
        activeContext?.await()
    }

    @Synchronized
    @JvmStatic
    fun shutdown() {
        contexts.forEach { cx -> cx.runCatching { close() }.onFailure { warnForClose(this, it) } }
        contexts.clear()
        activeContext = null
    }
}
