package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.skeleton.ai.ActResult
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ToolCallResult

data class AgentResponseAction(
    val domain: String? = null,
    val method: String? = null,

    val arguments: List<Map<String, String>?>? = null,
    val locator: String? = null,
    val description: String? = null,
)

data class AgentResponse(
    val actions: List<AgentResponseAction>,

    val memory: String? = null,
    val thinking: String? = null,
    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val evaluationPreviousGoal: String? = null,
    val nextGoal: String? = null,
)

data class ObserveResponseComplete(
    val taskComplete: Boolean = false,
    val success: Boolean = false,
    val errorCause: String? = null,
    val summary: String? = null,
    val keyFindings: List<String>? = null,
    val nextSuggestions: List<String>? = null,
)

data class ObserveResponseElements(
    val elements: List<ObserveResponseElement>? = null
)

data class ObserveResponseElement(
    val locator: String? = null,
    val description: String? = null,

    val domain: String? = null,
    val method: String? = null,

    val arguments: List<Map<String, String>?>? = null,

    val memory: String? = null,
    val thinking: String? = null,
    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val evaluationPreviousGoal: String? = null,
    val nextGoal: String? = null,
)

data class DetailedActResult(
    val actionDescription: ActionDescription,
    val toolCallResult: ToolCallResult? = null,
    val success: Boolean = false,
    val summary: String? = null,
) {
    fun toActResult(): ActResult {
        return ActResult(
            action = actionDescription.instruction,
            success = success,
            message = summary ?: "",
            result = toolCallResult
        )
    }
}

data class AgentAction(
    val step: Int,
    val userInstruction: String,
    val messages: AgentMessageList,

    val agentState: AgentState,
    val browserUseState: BrowserUseState? = null,
    val actionDescription: ActionDescription? = null,
    val actDetailedResult: DetailedActResult? = null,

    val prevAction: AgentAction? = null,
    val nextAction: AgentAction? = null,
    val parent: AgentAction? = null,
    val children: List<AgentAction> = emptyList(),
)
