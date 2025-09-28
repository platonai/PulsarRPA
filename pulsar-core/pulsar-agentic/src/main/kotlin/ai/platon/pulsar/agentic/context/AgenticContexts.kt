package ai.platon.pulsar.agentic.context

import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

object AgenticContexts {
    @Synchronized
    fun create(): AgenticContext = (PulsarContexts.activeContext as? AgenticContext)
        ?: create(DefaultClassPathXmlAgenticQLContext())

    @Synchronized
    fun create(context: AgenticContext): AgenticContext = context.also { PulsarContexts.create(it) }

    @Synchronized
    fun create(applicationContext: ApplicationContext): AgenticContext {
        val context = PulsarContexts.activeContext
        if (context is AgenticQLContext && context.applicationContext == applicationContext) {
            return PulsarContexts.activeContext as AgenticContext
        }

        return create(AgenticQLContext(applicationContext as AbstractApplicationContext))
    }

    @Synchronized
    fun create(contextLocation: String): AgenticContext = create(ClassPathXmlAgenticQLContext(contextLocation))

    @Synchronized
    fun createSession() = create().createSession()

    @Synchronized
    fun getOrCreateSession() = create().getOrCreateSession()

    @Throws(InterruptedException::class)
    fun await() = PulsarContexts.await()

    @Synchronized
    fun shutdown() = PulsarContexts.shutdown()
}
