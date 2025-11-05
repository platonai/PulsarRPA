package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.InternalAgentExecutor
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.context.AbstractAgenticContext
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.h2.AbstractH2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.ToolCallResult
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext
import ai.platon.pulsar.skeleton.session.AbstractPulsarSession
import ai.platon.pulsar.skeleton.session.PulsarSession

interface AgenticSession : PulsarSession {

    val companionAgent: PerceptiveAgent

    /**
     * Perform an action described by [action].
     *
     * @param action The action description that describes the action to be performed by the webdriver.
     * @return The response from the model, though in this implementation, the return value is not explicitly used.
     */
    suspend fun performAct(action: ActionDescription): ToolCallResult

    /**
     * Instructs the webdriver to perform a series of actions based on the given prompt.
     * This function converts the prompt into a sequence of webdriver actions, which are then executed.
     *
     * @param actionDescriptions The textual prompt that describes the actions to be performed by the webdriver.
     * @return The response from the model, though in this implementation, the return value is not explicitly used.
     */
    suspend fun plainActs(actionDescriptions: String): List<ToolCallResult>
}

abstract class AbstractAgenticSession(
    context: AbstractPulsarContext,
    sessionConfig: VolatileConfig,
    id: Long = generateNextInProcessId()
) : AbstractPulsarSession(context, sessionConfig, id = id), AgenticSession

open class BasicAgenticSession(
    context: AbstractAgenticContext,
    sessionConfig: VolatileConfig,
    id: Long = generateNextInProcessId()
) : AbstractAgenticSession(context, sessionConfig, id) {

    override val companionAgent: PerceptiveAgent by lazy { BrowserPerceptiveAgent(this) }

    private val executor by lazy { InternalAgentExecutor(this) }

    override suspend fun performAct(action: ActionDescription) = executor.performAct(action)

    override suspend fun plainActs(actionDescriptions: String) = executor.performActs(actionDescriptions)
}

open class AbstractAgenticQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractH2SQLSession(context, sessionDelegate, config), AgenticSession {

    override val companionAgent: PerceptiveAgent by lazy { BrowserPerceptiveAgent(this) }

    private val executor by lazy { InternalAgentExecutor(this) }

    override suspend fun performAct(action: ActionDescription) = executor.performAct(action)

    override suspend fun plainActs(actionDescriptions: String) = executor.performActs(actionDescriptions)
}

open class AgenticQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractAgenticQLSession(context, sessionDelegate, config)
