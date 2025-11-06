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
        val toolCall = action.toolCall ?: return ToolCallResult(success = false, message = "no tool call")

        val evaluate = when (toolCall.domain) {
            "driver" -> toolCallExecutor.execute(toolCall, driver)
            "browser" -> toolCallExecutor.execute(toolCall, driver.browser)
//            "fs" -> toolCallExecutor.execute(toolCall, fs)
//            "agent" -> toolCallExecutor.execute(toolCall, this)
            else -> throw IllegalArgumentException("❓ Unsupported domain: ${toolCall.domain} | $toolCall")
        }

        return ToolCallResult(
            success = true,
            evaluate = evaluate,
            message = "performAct",
            expression = action.cssFriendlyExpression,
            modelResponse = action.modelResponse?.content
        )
    }

    suspend fun performActs(actionDescriptions: String): List<ToolCallResult> {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val actions = tta.generateActions(actionDescriptions, driver)

        return actions.map { performAct(it) }
    }
}
