package ai.platon.pulsar.skeleton.ai.detail

import java.time.Instant

data class InteractiveElement(
    val id: String,
    val tagName: String,
    val selector: String,
    val text: String,
    val type: String?,
    val href: String?,
    val className: String?,
    val placeholder: String?,
    val value: String?,
    val isVisible: Boolean,
    val bounds: ElementBounds
) {
    val description: String
        get() = buildString {
            append("[$tagName")
            if (type != null) append(" type='$type'")
            append("] ")
            if (text.isNotBlank()) append("'$text' ")
            if (placeholder != null) append("placeholder='$placeholder' ")
            if (value != null) append("value='$value' ")
            append("selector='$selector'")
        }

    override fun toString() = description
}

data class ElementBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)


/**
 * Configuration for enhanced error handling and retry mechanisms
 */
data class WebDriverAgentConfig(
    val maxSteps: Int = 100,
    val maxRetries: Int = 3,
    val baseRetryDelayMs: Long = 1000,
    val maxRetryDelayMs: Long = 30000,
    val consecutiveNoOpLimit: Int = 5,
    val actionGenerationTimeoutMs: Long = 30000,
    val screenshotCaptureTimeoutMs: Long = 5000,
    val enableStructuredLogging: Boolean = false,
    val enableDebugMode: Boolean = false,
    val enablePerformanceMetrics: Boolean = true,
    val memoryCleanupIntervalSteps: Int = 50,
    val maxHistorySize: Int = 100,
    val enableAdaptiveDelays: Boolean = true,
    val enablePreActionValidation: Boolean = true
)

/**
 * Enhanced error classification for better retry strategies
 */
sealed class WebDriverAgentError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class TransientError(message: String, cause: Throwable? = null) : WebDriverAgentError(message, cause)
    class PermanentError(message: String, cause: Throwable? = null) : WebDriverAgentError(message, cause)
    class TimeoutError(message: String, cause: Throwable? = null) : WebDriverAgentError(message, cause)
    class ResourceExhaustedError(message: String, cause: Throwable? = null) : WebDriverAgentError(message, cause)
    class ValidationError(message: String, cause: Throwable? = null) : WebDriverAgentError(message, cause)
}

/**
 * Performance metrics for monitoring and optimization
 */
data class PerformanceMetrics(
    val totalSteps: Int = 0,
    val successfulActions: Int = 0,
    val failedActions: Int = 0,
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
    val stepNumber: Int,
    val actionType: String,
    val targetUrl: String,
    val timestamp: Instant = Instant.now(),
    val additionalContext: Map<String, Any> = emptyMap()
)
