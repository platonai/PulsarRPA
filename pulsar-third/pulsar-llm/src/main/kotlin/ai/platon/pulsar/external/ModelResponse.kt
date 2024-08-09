package ai.platon.pulsar.external

data class ModelResponse(
    val content: String,
    val state: ResponseState = ResponseState.STOP,
    val tokenUsage: TokenUsage = TokenUsage(),
)
