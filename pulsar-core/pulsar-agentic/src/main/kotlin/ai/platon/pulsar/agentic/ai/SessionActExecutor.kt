package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor
import ai.platon.pulsar.agentic.tools.executors.BrowserToolExecutor
import ai.platon.pulsar.agentic.tools.executors.WebDriverToolExecutor
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.ToolCallResult
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

internal class SessionActExecutor(
    val session: AgenticSession,
    val driver: WebDriver,
    val conf: ImmutableConfig
) {
    constructor(session: AgenticSession) : this(
        session,
        session.getOrCreateBoundDriver(),
        session.sessionConfig
    )

    private val executors = listOf(
        WebDriverToolExecutor(),
        BrowserToolExecutor()
    )
    private val toolCallExecutor = BasicToolCallExecutor(executors)

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
