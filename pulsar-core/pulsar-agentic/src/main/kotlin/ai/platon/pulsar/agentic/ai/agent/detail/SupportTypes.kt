package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.skeleton.ai.AgentState
import java.time.Instant

/**
 * Enhanced error classification for better retry strategies
 */
sealed class PerceptiveAgentError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class TransientError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class PermanentError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class TimeoutError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class ResourceExhaustedError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    class ValidationError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
}

/**
 * Performance metrics for monitoring and optimization
 */
data class PerformanceMetrics(
    var totalSteps: Int = 0,
    var successfulActions: Int = 0,
    var failedActions: Int = 0,
    val averageActionTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0,
    val memoryUsageMB: Double = 0.0,
    val retryCount: Int = 0,
    val consecutiveFailures: Int = 0
)

/**
 * Structured logging context for better debugging
 */
data class ExecutionContext(
    val sessionId: String,
    var step: Int,
    var actionType: String,
    var targetUrl: String? = null,

    var userRequest: String = "",
    var screenshotB64: String? = null,

    val timestamp: Instant = Instant.now(),
    val additionalContext: Map<String, Any> = emptyMap(),

    var prevAgentState: AgentState? = null,
    var agentState: AgentState? = null
)
