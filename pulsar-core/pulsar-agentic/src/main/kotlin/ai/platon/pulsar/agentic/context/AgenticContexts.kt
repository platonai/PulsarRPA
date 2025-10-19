package ai.platon.pulsar.agentic.context

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

object AgenticContexts {
    @Synchronized
    fun create(): AgenticContext = (PulsarContexts.activeContext as? AgenticContext)
        ?: create(DefaultClassPathXmlAgenticContext())

    @Synchronized
    fun create(context: AgenticContext): AgenticContext = context.also { PulsarContexts.create(it) }

    @Synchronized
    fun create(applicationContext: ApplicationContext): AgenticContext {
        val context = PulsarContexts.activeContext
        if (context is QLAgenticContext && context.applicationContext == applicationContext) {
            return PulsarContexts.activeContext as AgenticContext
        }

        return create(QLAgenticContext(applicationContext as AbstractApplicationContext))
    }

    @Synchronized
    fun create(contextLocation: String): AgenticContext = create(ClassPathXmlAgenticContext(contextLocation))

    @Synchronized
    fun createSession(settings: PulsarSettings = PulsarSettings()): AgenticSession = create().createSession(settings)

    @Synchronized
    fun getOrCreateSession(settings: PulsarSettings = PulsarSettings()): AgenticSession = create().getOrCreateSession(settings)

    @Throws(InterruptedException::class)
    fun await() = PulsarContexts.await()

    @Synchronized
    fun shutdown() = PulsarContexts.shutdown()
}
