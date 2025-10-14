package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.detail.InteractiveElement
import com.fasterxml.jackson.databind.JsonNode

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
