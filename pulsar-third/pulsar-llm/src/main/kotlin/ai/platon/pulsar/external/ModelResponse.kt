package ai.platon.pulsar.external

data class ModelResponse(
    val content: String,
    val state: ResponseState,
    val tokenUsage: TokenUsage,
)
