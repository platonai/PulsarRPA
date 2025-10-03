package ai.platon.pulsar.skeleton.context

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext
import ai.platon.pulsar.skeleton.context.support.BasicPulsarContext
import ai.platon.pulsar.skeleton.context.support.ClassPathXmlPulsarContext
import ai.platon.pulsar.skeleton.context.support.StaticPulsarContext
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
        val activated = activeContext
        if (activated != null && activated.isActive) {
            logger.debug("Context is already activated | {}#{}", activated::class, activated.id)
            return activated
        }

        activeContext = create(StaticPulsarContext())
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
        if (activated != null && activated::class == context::class && activated.isActive) {
            logger.info("Context is already activated | {}", activated::class)
            return activated
        }

        contexts.add(context)
        activeContext = context

        // NOTE: The order of registered shutdown hooks is not guaranteed.
        (context as? AbstractPulsarContext)?.applicationContext?.registerShutdownHook()
        context.registerShutdownHook()
        val count = contexts.count()
        val message = contexts.joinToString(" | ") { it::class.qualifiedName + " #" + it.id }
        logger.info("Total {} active contexts: {}", count, message)

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
     * Create a PulsarSession with the active context.
     *
     * @return the created session
     */
    @Synchronized
    @JvmStatic
    @Throws(Exception::class)
    fun getOrCreateSession() = create().getOrCreateSession()

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
     * @see PulsarContext.registerClosable
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
