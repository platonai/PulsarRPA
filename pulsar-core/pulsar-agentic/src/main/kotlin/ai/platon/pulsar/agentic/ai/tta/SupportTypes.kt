package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ToolCall
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

data class ActionDescription constructor(
    val observeElement: ObserveElement? = null,

    val isComplete: Boolean = false,
    val summary: String? = null,
    val nextSuggestions: List<String> = emptyList(),

    val errors: String? = null,
    val modelResponse: ModelResponse? = null
) {
    val toolCall: ToolCall? get() = observeElement?.toolCall
    val locator: String? get() = observeElement?.locator
    val node: DOMTreeNodeEx? get() = observeElement?.node
    val xpath: String? get() = observeElement?.xpath
    val cssSelector: String? get() = observeElement?.cssSelector
    val expression: String? get() = observeElement?.expression
    val cssFriendlyExpression: String? get() = observeElement?.cssFriendlyExpression

    @Deprecated("User cssFriendlyExpression instead")
    val cssFriendlyExpressions: List<String> get() = observeElement?.cssFriendlyExpressions ?: emptyList()

    override fun toString(): String {
        return if (isComplete) "Completed. Summary: $summary"
        else (cssFriendlyExpression ?: modelResponse?.toString() ?: "")
    }
}

data class ToolCallResults(
    val expressions: List<String> = emptyList(),
    val functionResults: List<Any?> = emptyList(),
    val action: ActionDescription? = null,
) {
    val modelResponse: ModelResponse get() = action?.modelResponse ?: ModelResponse.LLM_NOT_AVAILABLE
    val toolCalls: List<ToolCall>? get() = action?.toolCall?.let { listOf(it) }

    companion object {
        val LLM_NOT_AVAILABLE = ToolCallResults(
            emptyList(),
            emptyList(),
        )
    }
}

data class ActionExecuteResult(
    val action: ActionDescription,
    val toolCallResult: ToolCallResult? = null,
    val success: Boolean = false,
    val summary: String? = null,
) {
}

data class AgentAction(
    val step: Int,
    val userInstruction: String,
    val messages: AgentMessageList,

    val agentState: AgentState,
    val browserUseState: BrowserUseState? = null,
    val actionDescription: ActionDescription? = null,
    val actionExecuteResult: ActionExecuteResult? = null,

    val prevAction: AgentAction? = null,
    val nextAction: AgentAction? = null,
    val parent: AgentAction? = null,
    val children: List<AgentAction> = emptyList(),
)
