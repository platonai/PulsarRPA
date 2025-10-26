package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.agentic.ai.AgentConfig
import ai.platon.pulsar.common.StructuredLogger
import org.slf4j.Logger
import java.time.Instant

/**
 * Provides structured JSON logging for agent operations.
 *
 * This helper class formats log messages as proper JSON when structured
 * logging is enabled, improving observability and log analysis.
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class StructuredAgentLogger(
    ownerLogger: Logger,
    private val config: AgentConfig
): StructuredLogger(ownerLogger = ownerLogger, enableStructuredLogging = config.enableStructuredLogging) {
    /**
     * Log a structured message with context and additional data.
     *
     * @param message The log message
     * @param context Execution context containing session ID, step number, etc.
     * @param additionalData Additional data to include in the log
     */
    fun info(
        message: String,
        context: ExecutionContext,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        // Build proper JSON log data
        val logData = buildMap {
            put("actionType", context.actionType)
            put("step", context.stepNumber)
            put("message", message)
            put("sessionId", context.sessionId)
            put("timestamp", context.timestamp.toString())
            putAll(additionalData)
        }.toMutableMap()

        // Create proper JSON string

        if (config.enableStructuredLogging) {
            val jsonLog = formatAsJson(logData)
            logger.info("{}", jsonLog)
        } else {
            logData.remove("sessionId")
            logData.remove("timestamp")
            logData.remove("message")
            val log = logData.entries.joinToString(", ") { (key, value) -> "$key:$value" }
            logger.info("{} | {} | {}", message, context.sessionId.take(8), log)
        }
    }

    /**
     * Log an error with context and exception details.
     *
     * @param message Error message
     * @param error The exception that occurred
     * @param sessionId Session identifier
     */
    fun logError(message: String, error: Throwable, sessionId: String) {
        val errorData = mapOf(
            "sessionId" to sessionId,
            "errorType" to error.javaClass.simpleName,
            "errorMessage" to (error.message ?: "Unknown error"),
            "timestamp" to Instant.now().toString()
        )

        if (config.enableStructuredLogging) {
            val jsonLog = formatAsJson(errorData)
            logger.error("PerceptiveAgent Error: {} - {}", message, jsonLog, error)
        } else {
            logger.error("PerceptiveAgent Error: {} - {}", message, errorData, error)
        }
    }

    /**
     * Log observation operation with results.
     *
     * @param instruction The observation instruction
     * @param requestId Request identifier
     * @param resultCount Number of results found
     * @param success Whether the operation succeeded
     */
    fun logObserve(instruction: String, requestId: String, resultCount: Int, success: Boolean) {
        val status = if (success) "OK" else "FAIL"
        val msg = "observe[$requestId] $status ${instruction.take(50)} -> $resultCount elements"

        if (config.enableStructuredLogging) {
            val logData = mapOf(
                "operation" to "observe",
                "requestId" to requestId,
                "instruction" to instruction.take(120),
                "resultCount" to resultCount,
                "success" to success,
                "timestamp" to Instant.now().toString()
            )
            logger.info("{}", formatAsJson(logData))
        } else {
            logger.info(msg)
        }
    }

    /**
     * Log extraction operation.
     *
     * @param instruction The extraction instruction
     * @param requestId Request identifier
     * @param success Whether the operation succeeded
     */
    fun logExtract(instruction: String, requestId: String, success: Boolean) {
        val status = if (success) "OK" else "FAIL"
        val msg = "extract[$requestId] $status ${instruction.take(60)}"

        if (config.enableStructuredLogging) {
            val logData = mapOf(
                "operation" to "extract",
                "requestId" to requestId,
                "instruction" to instruction.take(120),
                "success" to success,
                "timestamp" to Instant.now().toString()
            )
            logger.info("{}", formatAsJson(logData))
        } else {
            logger.info(msg)
        }
    }
}
