package ai.platon.pulsar.agentic.ai.support

data class ToolCall(
    val domain: String,
    val name: String,
    val args: Map<String, Any?>
)
