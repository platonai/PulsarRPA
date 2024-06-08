package ai.platon.pulsar.common.lm

open class ModelResponse(
    val content: String,
    val state: ResponseState,
    val tokenUsage: TokenUsage,
)
