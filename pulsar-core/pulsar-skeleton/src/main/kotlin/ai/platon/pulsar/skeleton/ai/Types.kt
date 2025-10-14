package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.detail.InteractiveElement

data class ActionOptions(
    val action: String,
    val modelName: String? = null,
    val variables: Map<String, String>? = null,
    val domSettleTimeoutMs: Int? = null,
    val timeoutMs: Int? = null,
    val iframes: Boolean? = null
)

data class ActResult(
    val success: Boolean,
    val message: String,
    val action: String
)

data class ExtractOptions<T>(
    val instruction: String? = null,
    val schema: T? = null,
    val modelName: String? = null,
    val modelClientOptions: Map<String, Any>? = null,
    val domSettleTimeoutMs: Long? = null,
    val selector: String? = null,
    val iframes: Boolean? = null,
    val frameId: String? = null
)

data class ObserveOptions(
    val instruction: String? = null,
    val modelName: String? = null,
    val modelClientOptions: Map<String, Any>? = null,
    val domSettleTimeoutMs: Long? = null,
    val returnAction: Boolean? = null,

    val drawOverlay: Boolean? = null,
    val iframes: Boolean? = null,
    val frameId: String? = null
)

data class ObserveResult(
    val selector: String,
    val description: String,
    val backendNodeId: Int? = null,
    val method: String? = null,
    val arguments: List<String>? = null
)

data class ActionDescription(
    val functionCalls: List<String>,
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
