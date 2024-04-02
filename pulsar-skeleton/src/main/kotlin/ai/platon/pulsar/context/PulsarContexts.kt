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
    
    /**
     * The active context, it's the last created context.
     */
    var activeContext: PulsarContext? = null
        private set

    /**
     * Create a new context, if there is already an active context, return it.
     *
     * @return the created context
     */
    @Synchronized
    @JvmStatic
    fun create(): PulsarContext {
        if (activeContext == null) {
            activeContext = create(StaticPulsarContext())
        }
        return activeContext!!
    }

    /**
     * Create a new context, if there is already an active context, return it.
     *
     * @param context the context to be created
     * @return the created context
     */
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
        // NOTE: The order of registered shutdown hooks is not guaranteed.
        (context as? AbstractPulsarContext)?.applicationContext?.registerShutdownHook()
        context.registerShutdownHook()
        logger.info("Active context | {}", contexts.joinToString(" | ") { it::class.qualifiedName + " #" + it.id })

        return context
    }

    /**
     * Create a new context, if there is already an active context, return it.
     *
     * @param contextLocation the location of the context
     * @return the created context
     */
    @Synchronized
    @JvmStatic
    fun create(contextLocation: String) = create(ClassPathXmlPulsarContext(contextLocation))

    /**
     * Create a new context, if there is already an active context, return it.
     *
     * @param applicationContext the Spring application context
     * @return the created context
     */
    @Synchronized
    @JvmStatic
    fun create(applicationContext: ApplicationContext) =
        create(BasicPulsarContext(applicationContext as AbstractApplicationContext))
    
    /**
     * Create a PulsarSession with the active context.
     *
     * @return the created session
     */
    @Synchronized
    @JvmStatic
    @Throws(Exception::class)
    fun createSession() = create().createSession()

    /**
     * Wait for all submitted urls to be processed.
     */
    @JvmStatic
    @Throws(InterruptedException::class)
    fun await() {
        activeContext?.await()
    }
    
    /**
     * Register a closable object to the active context.
     * TODO: might fail before the context is created
     *
     * @param closable the closable object
     * @param priority the priority of the closable object
     * @see AutoCloseable
     * @see AbstractPulsarContext.registerClosable
     */
    @JvmStatic
    fun registerClosable(closable: AutoCloseable, priority: Int = 0) {
        activeContext?.registerClosable(closable, priority)
    }
    
    /**
     * Close the all the created context.
     */
    @Synchronized
    @JvmStatic
    fun shutdown() {
        contexts.forEach { cx -> cx.runCatching { close() }.onFailure { warnForClose(this, it) } }
        contexts.clear()
        activeContext = null
    }
}
