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
        if (action.expressions.isEmpty() && action.toolCall == null) {
            return InstructionResult(listOf(), functionResults = listOf(), modelResponse = action.modelResponse)
        }

        val dispatcher = ToolCallExecutor()
        val result = if (action.toolCall != null) {
            dispatcher.execute(action.toolCall, driver)
        } else {
            val functionCalls = action.expressions.take(1)
            functionCalls.map { fc -> dispatcher.execute(fc, driver) }.firstOrNull()
        }

        return InstructionResult(action.expressions, functionResults = listOf(result), modelResponse = action.modelResponse)
    }

    suspend fun execute(action: ActionDescription) = performAct(action)

    @Deprecated("Use act instead", replaceWith = ReplaceWith("act(action)"))
    suspend fun instruct(prompt: String): InstructionResult {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val actions = tta.generateWithToolCallSpecs(prompt)

        // Dispatches and executes each action using a SimpleCommandDispatcher.
        val dispatcher = ToolCallExecutor()
        val functionResults = actions.expressions.map { action ->
            dispatcher.execute(action, driver)
        }

        return InstructionResult(actions.expressions, functionResults, actions.modelResponse)
    }
}
