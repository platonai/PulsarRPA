package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.ai.ActResult
import ai.platon.pulsar.skeleton.ai.ActionOptions
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

    private val toolCallExecutor = ToolCallExecutor()

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
        val toolCall = action.toolCall
        if (action.expressions.isEmpty() && toolCall == null) {
            return InstructionResult(action = action)
        }

        val result = if (toolCall != null) {
            toolCallExecutor.execute(toolCall, driver)
        } else {
            action.expressions.take(1).map { expr -> toolCallExecutor.execute(expr, driver) }.firstOrNull()
        }

        return InstructionResult(
            action.expressions,
            functionResults = listOf(result),
            action = action
        )
    }

    suspend fun execute(action: ActionDescription) = performAct(action)

    @Deprecated("Use act instead", replaceWith = ReplaceWith("act(action)"))
    suspend fun instruct(prompt: String): InstructionResult {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val action = tta.generate(prompt, driver)

        return performAct(action)
    }
}
