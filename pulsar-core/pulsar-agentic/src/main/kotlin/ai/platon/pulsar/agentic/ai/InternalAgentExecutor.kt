package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.agentic.tools.ToolCallExecutor
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.ai.ToolCallResult
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

internal class InternalAgentExecutor(
    val session: AgenticSession,
    val driver: WebDriver,
    val conf: ImmutableConfig
) {
    constructor(session: AgenticSession) : this(
        session,
        session.getOrCreateBoundDriver(),
        session.sessionConfig
    )

    private val toolCallExecutor = ToolCallExecutor()

    suspend fun performAct(action: ActionDescription): ToolCallResult {
        val toolCall = action.toolCall
        if (action.expression.isNullOrBlank() && toolCall == null) {
            return ToolCallResult(success = false)
        }

        val result = if (toolCall != null) {
            toolCallExecutor.execute(toolCall, driver)
        } else {
            action.expression?.let { expr -> toolCallExecutor.execute(expr, driver) }
        }

        return ToolCallResult(
            true,
            result = result,
            message = "performing ${action.toolCall?.method} action",
            expression = action.expression,
        )
    }

    suspend fun performActs(actionDescriptions: String): List<ToolCallResult> {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val actions = tta.generateActions(actionDescriptions, driver)

        return actions.map { performAct(it) }
    }
}
