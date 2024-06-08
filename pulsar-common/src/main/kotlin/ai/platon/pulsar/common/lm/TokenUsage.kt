package ai.platon.pulsar.common.lm

data class TokenUsage(
    val inputTokenCount: Int = 0,
    val outputTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
)
