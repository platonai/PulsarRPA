package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.InternalAgentExecutor
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.ToolCallResult
import ai.platon.pulsar.agentic.ai.tta.ToolCallResults
import ai.platon.pulsar.agentic.context.AbstractAgenticContext
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.h2.AbstractH2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext
import ai.platon.pulsar.skeleton.session.AbstractPulsarSession
import ai.platon.pulsar.skeleton.session.PulsarSession
import com.google.common.annotations.Beta

interface AgenticSession: PulsarSession {

    /**
     * Executes an action described by the given string.
     * An agent will be created to analyze the action and generate a step-by-step plan to perform it.
     * Each step in the plan uses at most one tool.
     *
     * @param problem A string describing the problem to be resolved.
     * @return A [ai.platon.pulsar.skeleton.ai.PerceptiveAgent] instance that executes the action.
     * @throws Exception if the action cannot be performed or if an error occurs during execution.
     */
    suspend fun resolve(problem: String): PerceptiveAgent

    /**
     * Executes an action described by the given string.
     * An agent will be created to analyze the action and generate a step-by-step plan to perform it.
     * Each step in the plan uses at most one tool.
     *
     * @param action A string describing the action to be performed.
     * @return A [ai.platon.pulsar.skeleton.ai.PerceptiveAgent] instance that executes the action.
     * @throws Exception if the action cannot be performed or if an error occurs during execution.
     */
    suspend fun act(action: String): PerceptiveAgent

    /**
     * Executes an action described by the given [ai.platon.pulsar.skeleton.ai.ActionOptions].
     * An agent will be created to analyze the action and generate a step-by-step plan to perform it.
     * Each step in the plan uses at most one tool.
     *
     * @param action An [ai.platon.pulsar.skeleton.ai.ActionOptions] object describing the action to be performed.
     * @return A [ai.platon.pulsar.skeleton.ai.PerceptiveAgent] instance that executes the action.
     * @throws Exception if the action cannot be performed or if an error occurs during execution.
     */
    suspend fun act(action: ActionOptions): PerceptiveAgent

    /**
     * Perform an action described by [action].
     *
     * @param action The action description that describes the action to be performed by the webdriver.
     * @return The response from the model, though in this implementation, the return value is not explicitly used.
     */
    suspend fun performAct(action: ActionDescription): ToolCallResult

    @Beta
    suspend fun execute(action: ActionDescription): ToolCallResult

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
): AbstractPulsarSession(context, sessionConfig, id = id), AgenticSession

open class BasicAgenticSession(
    context: AbstractAgenticContext,
    sessionConfig: VolatileConfig,
    id: Long = generateNextInProcessId()
) : AbstractAgenticSession(context, sessionConfig, id) {

    private val executor by lazy { InternalAgentExecutor(this) }

    override suspend fun resolve(problem: String): PerceptiveAgent {
        executor.resolve(problem)
        return executor.agent
    }

    override suspend fun act(action: String): PerceptiveAgent {
        return act(ActionOptions(action = action))
    }

    override suspend fun act(action: ActionOptions): PerceptiveAgent {
        executor.act(action)
        return executor.agent
    }

    override suspend fun performAct(action: ActionDescription) = executor.performAct(action)

    override suspend fun execute(action: ActionDescription) = executor.execute(action)

    override suspend fun plainActs(actionDescriptions: String) = executor.plainActs(actionDescriptions)
}

open class AbstractAgenticQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractH2SQLSession(context, sessionDelegate, config), AgenticSession {

    private val executor by lazy { InternalAgentExecutor(this) }

    override suspend fun resolve(problem: String): PerceptiveAgent {
        executor.resolve(problem)
        return executor.agent
    }

    override suspend fun act(action: String): PerceptiveAgent {
        return act(ActionOptions(action = action))
    }

    override suspend fun act(action: ActionOptions): PerceptiveAgent {
        executor.act(action)
        return executor.agent
    }

    override suspend fun performAct(action: ActionDescription) = executor.performAct(action)

    override suspend fun execute(action: ActionDescription) = executor.execute(action)

    override suspend fun plainActs(actionDescriptions: String) = executor.plainActs(actionDescriptions)
}

open class AgenticQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractAgenticQLSession(context, sessionDelegate, config)
