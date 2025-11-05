package ai.platon.pulsar.agentic.context

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.browser.common.DisplayMode
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

/**
 * Manage creation and lifecycle of Pulsar agentic contexts and sessions.
 *
 * An agentic session provides:
 * - agentic/browser-based agents
 * - Full-featured WebDriver
 * - Capture of live web pages into a local `WebPage`
 * - Parsing a `WebPage` into a lightweight `Document`
 * - A full crawler (fetching, parsing, scheduling, priorities, crawl pool)
 * - Basic LLM support for interacting with pages or documents
 * - X-SQL for advanced web page modeling
 *
 * This object coordinates the active context, shutdown hooks and session creation.
 */
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
    fun createSession(settings: PulsarSettings): AgenticSession = create().createSession(settings)

    @Synchronized
    fun getOrCreateSession(settings: PulsarSettings): AgenticSession = create().getOrCreateSession(settings)

    @Synchronized
    fun createSession(
        spa: Boolean? = true,
        headless: Boolean = false,
        maxBrowsers: Int? = null,
        maxOpenTabs: Int? = null,
        interactSettings: InteractSettings? = null,
    ): AgenticSession {
        val displayMode = if (headless) DisplayMode.HEADLESS else null
        val settings = PulsarSettings(spa, displayMode, maxBrowsers, maxOpenTabs, interactSettings)
        return createSession(settings)
    }

    @Synchronized
    fun getOrCreateSession(
        spa: Boolean? = true,
        headless: Boolean = false,
        maxBrowsers: Int? = null,
        maxOpenTabs: Int? = null,
        interactSettings: InteractSettings? = null,
    ): AgenticSession {
        val displayMode = if (headless) DisplayMode.HEADLESS else null
        val settings = PulsarSettings(spa, displayMode, maxBrowsers, maxOpenTabs, interactSettings)
        return getOrCreateSession(settings)
    }

    @Throws(InterruptedException::class)
    fun await() = PulsarContexts.await()

    @Synchronized
    fun shutdown() = PulsarContexts.shutdown()

    fun close() = shutdown()
}
