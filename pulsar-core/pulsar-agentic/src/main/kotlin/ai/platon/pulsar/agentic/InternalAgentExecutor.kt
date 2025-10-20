package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.ai.ActResult
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCallExecutor
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

internal class InternalAgentExecutor(
    val driver: WebDriver,
    val conf: ImmutableConfig
) {
    constructor(session: AgenticSession): this(
        requireNotNull(session.boundDriver) { "Bind a driver for agentic functionalities: `session.bind(driver)`" },
        session.sessionConfig
    )

    val agent = BrowserPerceptiveAgent(driver)

    suspend fun resolve(action: String): ActResult {
        return agent.resolve(action)
    }

    suspend fun act(action: String): ActResult {
        return act(ActionOptions(action = action))
    }

    suspend fun act(action: ActionOptions): ActResult {
        return agent.act(action)
    }

    suspend fun performAct(action: ActionDescription): InstructionResult {
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

    suspend fun execute(action: ActionDescription): InstructionResult {
        if (action.functionCalls.isEmpty()) {
            return InstructionResult(listOf(), listOf(), action.modelResponse)
        }
        val functionCalls = action.functionCalls.take(1)
        val dispatcher = ToolCallExecutor()
        val functionResults = functionCalls.map { fc -> dispatcher.execute(fc, driver) }
        return InstructionResult(action.functionCalls, functionResults, action.modelResponse)
    }

    @Deprecated("Use act instead", replaceWith = ReplaceWith("act(action)"))
    suspend fun instruct(prompt: String): InstructionResult {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val actions = tta.generateWebDriverActionsWithToolCallSpecsDeferred(prompt)

        // Dispatches and executes each action using a SimpleCommandDispatcher.
        val dispatcher = ToolCallExecutor()
        val functionResults = actions.functionCalls.map { action ->
            dispatcher.execute(action, driver)
        }

        return InstructionResult(actions.functionCalls, functionResults, actions.modelResponse)
    }
}
