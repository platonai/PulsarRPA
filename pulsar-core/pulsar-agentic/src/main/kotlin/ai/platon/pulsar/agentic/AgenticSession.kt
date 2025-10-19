package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.h2.AbstractH2SQLSession
import ai.platon.pulsar.ql.h2.H2SessionDelegate
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.skeleton.context.support.AbstractPulsarContext
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCallExecutor
import com.google.common.annotations.Beta

interface AgenticSession: SQLSession {

    /**
     * Executes an action described by the given string.
     * An agent will be created to analyze the action and generate a step-by-step plan to perform it.
     * Each step in the plan uses at most one tool.
     *
     * @param action A string describing the action to be performed.
     * @return A [ai.platon.pulsar.skeleton.ai.PerceptiveAgent] instance that executes the action.
     * @throws Exception if the action cannot be performed or if an error occurs during execution.
     */
    override suspend fun act(action: String): PerceptiveAgent

    /**
     * Executes an action described by the given [ai.platon.pulsar.skeleton.ai.ActionOptions].
     * An agent will be created to analyze the action and generate a step-by-step plan to perform it.
     * Each step in the plan uses at most one tool.
     *
     * @param action An [ai.platon.pulsar.skeleton.ai.ActionOptions] object describing the action to be performed.
     * @return A [ai.platon.pulsar.skeleton.ai.PerceptiveAgent] instance that executes the action.
     * @throws Exception if the action cannot be performed or if an error occurs during execution.
     */
    override suspend fun act(action: ActionOptions): PerceptiveAgent

    /**
     * Perform an action described by [action].
     *
     * @param action The action description that describes the action to be performed by the webdriver.
     * @return The response from the model, though in this implementation, the return value is not explicitly used.
     */
    @Beta
    override suspend fun performAct(action: ActionDescription): InstructionResult

    @Beta
    override suspend fun execute(action: ActionDescription): InstructionResult

    /**
     * Instructs the webdriver to perform a series of actions based on the given prompt.
     * This function converts the prompt into a sequence of webdriver actions, which are then executed.
     *
     * @param prompt The textual prompt that describes the actions to be performed by the webdriver.
     * @return The response from the model, though in this implementation, the return value is not explicitly used.
     */
    @Deprecated("Use act instead", ReplaceWith("multiAct(action)"))
    override suspend fun instruct(prompt: String): InstructionResult
}

open class AbstractAgenticSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractH2SQLSession(context, sessionDelegate, config), AgenticSession {

    override suspend fun act(action: String): PerceptiveAgent {
        return act(ActionOptions(action = action))
    }

    override suspend fun act(action: ActionOptions): PerceptiveAgent {
        val driver = requireNotNull(boundDriver) { "Bind a WebDriver to use `act`: session.bind(driver)" }
        val agent = BrowserPerceptiveAgent(driver)

        agent.act(action)

        return agent
    }

    override suspend fun performAct(action: ActionDescription): InstructionResult {
        val driver = requireNotNull(boundDriver) { "Bind a WebDriver to use `performAct`" }
        if (action.functionCalls.isEmpty()) {
            return InstructionResult(listOf(), listOf(), action.modelResponse)
        }
        val functionCalls = action.functionCalls

        // Dispatches and executes each action using a SimpleCommandDispatcher.
        val dispatcher = ToolCallExecutor()
        val functionResults = functionCalls.map { action ->
            dispatcher.execute(action, driver)
        }
        return InstructionResult(action.functionCalls, functionResults, action.modelResponse)
    }

    override suspend fun execute(action: ActionDescription): InstructionResult {
        val driver = requireNotNull(boundDriver) { "Bind a WebDriver to use `execute`" }
        if (action.functionCalls.isEmpty()) {
            return InstructionResult(listOf(), listOf(), action.modelResponse)
        }
        val functionCalls = action.functionCalls.take(1)
        val dispatcher = ToolCallExecutor()
        val functionResults = functionCalls.map { fc -> dispatcher.execute(fc, driver) }
        return InstructionResult(action.functionCalls, functionResults, action.modelResponse)
    }

    @Deprecated("Use act instead", replaceWith = ReplaceWith("act(action)"))
    override suspend fun instruct(prompt: String): InstructionResult {
        val driver = requireNotNull(boundDriver) { "Bind a WebDriver to use `act`" }

        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(sessionConfig)

        val actions = tta.generateWebDriverActionsWithToolCallSpecsDeferred(prompt)

        // Dispatches and executes each action using a SimpleCommandDispatcher.
        val dispatcher = ToolCallExecutor()
        val functionResults = actions.functionCalls.map { action ->
            dispatcher.execute(action, driver)
        }

        return InstructionResult(actions.functionCalls, functionResults, actions.modelResponse)
    }
}

open class DefaultAgenticSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractAgenticSession(context, sessionDelegate, config)
