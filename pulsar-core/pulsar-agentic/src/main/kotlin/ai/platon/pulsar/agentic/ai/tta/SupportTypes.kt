package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCall

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

data class ActionDescription(
    @Deprecated("Use toolCall instead.", ReplaceWith("toolCall"))
    val expressions: List<String> = emptyList(),
    val modelResponse: ModelResponse,
    val toolCall: ToolCall? = null,
    val selectedElement: InteractiveElement? = null,
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
