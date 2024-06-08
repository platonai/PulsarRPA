package ai.platon.pulsar.common.ai.api

data class TokenUsage(
    val inputTokenCount: Int = 0,
    val outputTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
)
