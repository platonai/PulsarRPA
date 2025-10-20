package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.external.ModelResponse

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
    val expressions: List<String>,
    val selectedElement: InteractiveElement?,
    val modelResponse: ModelResponse,
) {
    companion object {
        val LLM_NOT_AVAILABLE = ActionDescription(listOf(), null, ModelResponse.LLM_NOT_AVAILABLE)
    }
}

data class InstructionResult(
    val functionCalls: List<String>,
    val functionResults: List<Any?>,
    val modelResponse: ModelResponse,
) {
    companion object {
        val LLM_NOT_AVAILABLE = InstructionResult(
            listOf(),
            listOf(),
            modelResponse = ModelResponse.LLM_NOT_AVAILABLE,
        )
    }
}
