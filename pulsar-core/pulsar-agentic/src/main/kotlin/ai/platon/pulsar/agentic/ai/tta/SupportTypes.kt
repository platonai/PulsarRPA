package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.agent.ObserveElement
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.agentic.ai.support.ToolCall

data class InteractiveElement(
    val id: String,
    val tagName: String,
    val selector: String,
    val text: String,
    val type: String?,
    val href: String?,
    val className: String?,
    val placeholder: String?,
    val value: String?,
    val isVisible: Boolean,
    val bounds: ElementBounds
) {
    val description: String
        get() = buildString {
            append("[$tagName")
            if (type != null) append(" type='$type'")
            append("] ")
            if (text.isNotBlank()) append("'$text' ")
            if (placeholder != null) append("placeholder='$placeholder' ")
            if (value != null) append("value='$value' ")
            append("selector='$selector'")
        }

    override fun toString() = description
}

data class ElementBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

data class ActionResponse(
    val elements: List<ObserveElement>,
    val isComplete: Boolean = false,
    val summary: String? = null,
    val suggestions: List<String> = emptyList()
)

data class ActionDescription constructor(
    val modelResponse: ModelResponse,
    val toolCall: ToolCall? = null,
    val locator: String? = null,
    val node: DOMTreeNodeEx? = null,
    val xpath: String? = null,
    val cssSelector: String? = null,
    val expressions: List<String> = emptyList(),
    val cssFriendlyExpressions: List<String> = emptyList(),
    val isComplete: Boolean = false,
    val summary: String? = null,
    val suggestions: List<String> = emptyList()
)

data class InstructionResult(
    val expressions: List<String>,
    val functionResults: List<Any?>,
    val modelResponse: ModelResponse,
    val toolCall: List<ToolCall> = emptyList(),
) {
    companion object {
        val LLM_NOT_AVAILABLE = InstructionResult(
            emptyList(),
            emptyList(),
            modelResponse = ModelResponse.LLM_NOT_AVAILABLE,
            emptyList(),
        )
    }
}
