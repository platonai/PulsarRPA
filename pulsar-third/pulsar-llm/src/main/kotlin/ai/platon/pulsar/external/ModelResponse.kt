package ai.platon.pulsar.external

data class ModelResponse(
    var content: String,
    var state: ResponseState = ResponseState.STOP,
    var tokenUsage: TokenUsage = TokenUsage(),
) {
    fun isLocalCached(): Boolean {
        return state == ResponseState.LOCAL_CACHE
    }
}
