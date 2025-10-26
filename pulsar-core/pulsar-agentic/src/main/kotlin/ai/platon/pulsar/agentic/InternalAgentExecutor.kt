package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.ai.ActResult
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.support.ToolCallExecutor
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

internal class InternalAgentExecutor(
    val session: AgenticSession,
    val driver: WebDriver,
    val conf: ImmutableConfig
) {
    constructor(session: AgenticSession): this(
        session,
        requireNotNull(session.boundDriver) { "Bind a driver for agentic functionalities: `session.bind(driver)`" },
        session.sessionConfig
    )

    private val dispatcher = ToolCallExecutor()

    val agent = BrowserPerceptiveAgent(driver, session)

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
        if (action.cssFriendlyExpressions.isEmpty() && action.toolCall == null) {
            return InstructionResult(listOf(), functionResults = listOf(), modelResponse = action.modelResponse)
        }

        val result = if (action.toolCall != null) {
            dispatcher.execute(action.toolCall, driver)
        } else {
            val expressions = action.cssFriendlyExpressions.take(1)
            expressions.map { fc -> dispatcher.execute(fc, driver) }.firstOrNull()
        }

        return InstructionResult(action.cssFriendlyExpressions, functionResults = listOf(result), modelResponse = action.modelResponse)
    }

    suspend fun execute(action: ActionDescription) = performAct(action)

    @Deprecated("Use act instead", replaceWith = ReplaceWith("act(action)"))
    suspend fun instruct(prompt: String): InstructionResult {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val action = tta.generate(prompt, driver)

        val result = if (action.toolCall != null) {
            dispatcher.execute(action.toolCall, driver)
        } else {
            val expressions = action.cssFriendlyExpressions.take(1)
            expressions.map { fc -> dispatcher.execute(fc, driver) }.firstOrNull()
        }

        return InstructionResult(action.cssFriendlyExpressions, listOf(result), action.modelResponse)
    }
}
