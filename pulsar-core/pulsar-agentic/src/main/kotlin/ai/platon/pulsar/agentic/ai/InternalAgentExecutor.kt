package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.agentic.ai.tta.ToolCallResult
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
        session.getOrCreateBoundDriver(),
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

    suspend fun performAct(action: ActionDescription): ToolCallResult {
        val toolCall = action.toolCall
        if (action.expression.isNullOrBlank() && toolCall == null) {
            return ToolCallResult()
        }

        val result = if (toolCall != null) {
            toolCallExecutor.execute(toolCall, driver)
        } else {
            action.expression?.let { expr -> toolCallExecutor.execute(expr, driver) }
        }

        return ToolCallResult(
            action.expression ?: "",
            functionResult = result,
            action = action
        )
    }

    suspend fun execute(action: ActionDescription) = performAct(action)

    suspend fun plainActs(actionDescriptions: String): List<ToolCallResult> {
        // Converts the prompt into a sequence of webdriver actions using TextToAction.
        val tta = TextToAction(conf)

        val actions = tta.generateActions(actionDescriptions, driver)

        return actions.map { performAct(it) }
    }
}
