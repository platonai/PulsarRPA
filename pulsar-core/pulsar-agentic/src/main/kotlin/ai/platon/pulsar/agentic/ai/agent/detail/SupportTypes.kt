package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.agentic.AgentConfig
import ai.platon.pulsar.agentic.ai.agent.ExtractParams
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ObserveOptions
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.*

/**
 * Enhanced error classification for better retry strategies
 */
sealed class PerceptiveAgentError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class TransientError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
    open class PermanentError(message: String, cause: Throwable? = null) : PerceptiveAgentError(message, cause)
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
    var step: Int,

    var instruction: String = "",
    var screenshotB64: String? = null,

    var actionType: String,
    var targetUrl: String? = null,

    var prevAgentState: AgentState? = null,
    val agentState: AgentState,

    val config: AgentConfig,

    val sessionId: String,
    val timestamp: Instant = Instant.now(),
    val additionalContext: Map<String, Any> = emptyMap(),
) {
    val sid get() = sessionId.take(8)

    val requestId = UUID.randomUUID().toString()

    fun createObserveOptions(): ObserveOptions {
        val options = ObserveOptions(instruction = this.instruction)
        options.additionalContext["context"] = WeakReference(this)
        return options
    }

    fun createObserveParams(options: ObserveOptions, fromAct: Boolean, screenshotB64: String? = null): ObserveParams {
        return ObserveParams(
            instruction = instruction,
            agentState = agentState,
            requestId = requestId,
            returnAction = options.returnAction ?: false,
            logInferenceToFile = config.enableStructuredLogging,
            fromAct = fromAct,
            screenshotB64 = screenshotB64,
        )
    }

    fun createObserveActParams(screenshotB64: String? = null): ObserveParams {
        return ObserveParams(
            instruction = instruction,
            agentState = agentState,
            requestId = requestId,
            fromAct = true,
            returnAction = true,
            logInferenceToFile = config.enableStructuredLogging,
            screenshotB64 = screenshotB64
        )
    }

    fun createExtractParams(schema: ExtractionSchema): ExtractParams {
        return ExtractParams(
            instruction = instruction,
            agentState = agentState,
            schema = schema,
            requestId = requestId,
            logInferenceToFile = config.enableStructuredLogging,
        )
    }
}
