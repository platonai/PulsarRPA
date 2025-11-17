package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.agentic.AgentConfig
import ai.platon.pulsar.agentic.ai.agent.ExtractParams
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ObserveOptions
import ai.platon.pulsar.skeleton.ai.ObserveResult
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
data class ExecutionContext constructor(
    var step: Int,

    var instruction: String = "",
    var screenshotB64: String? = null,

    var event: String,
    var targetUrl: String? = null,

    val agentState: AgentState,
    val stateHistory: List<AgentState>,

    val config: AgentConfig,

    val sessionId: String,
    val timestamp: Instant = Instant.now(),
    val additionalContext: Map<String, Any> = emptyMap(),

    var baseContext: WeakReference<ExecutionContext> = WeakReference<ExecutionContext>(null)
) {
    val sid get() = sessionId.take(8)

    val uuid = UUID.randomUUID().toString()

    val prevAgentState: AgentState? get() = agentState.prevState

    fun createObserveParams(
        options: ObserveOptions,
        fromAct: Boolean,
        resolve: Boolean
    ): ObserveParams {
        return ObserveParams(
            context = this,
            returnAction = options.returnAction ?: false,
            logInferenceToFile = config.logInferenceToFile,
            fromAct = fromAct,
            resolve = resolve
        )
    }

    fun createObserveActParams(resolve: Boolean): ObserveParams {
        return ObserveParams(
            context = this,
            fromAct = true,
            returnAction = true,
            resolve = resolve,
            logInferenceToFile = config.logInferenceToFile,
        )
    }

    fun createExtractParams(schema: ExtractionSchema): ExtractParams {
        return ExtractParams(
            instruction = instruction,
            agentState = agentState,
            schema = schema,
            requestId = uuid,
            logInferenceToFile = config.logInferenceToFile,
        )
    }
}

fun ActionOptions.setContext(context: ExecutionContext) {
    additionalContext["context"] = context
}

fun ActionOptions.getContext(): ExecutionContext? {
    return additionalContext["context"] as? ExecutionContext
}

fun ObserveResult.setContext(context: ExecutionContext) {
    additionalContext["context"] = context
}

fun ObserveResult.getContext(): ExecutionContext? {
    return additionalContext["context"] as? ExecutionContext
}

fun ObserveOptions.setContext(context: ExecutionContext) {
    additionalContext["context"] = context
}

fun ObserveOptions.getContext(): ExecutionContext? {
    return additionalContext["context"] as? ExecutionContext
}
