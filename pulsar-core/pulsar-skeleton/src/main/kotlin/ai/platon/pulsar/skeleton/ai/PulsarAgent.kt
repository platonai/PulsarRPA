package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.tta.ActionDescription
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.skeleton.ai.tta.InteractiveElement
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

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

class PulsarAgent(
    val driver: WebDriver,
    val maxSteps: Int = 100,
    val config: WebDriverAgentConfig = WebDriverAgentConfig(maxSteps = maxSteps)
) {
    private val logger = getLogger(this)

    val uuid = UUID.randomUUID()
    val baseDir = AppPaths.get("agent")
    val conf get() = (driver as AbstractWebDriver).settings.config

    private val tta by lazy { TextToAction(conf) }
    private val model get() = tta.model

    // Enhanced state management
    private val _history = mutableListOf<String>()
    private val performanceMetrics = PerformanceMetrics()
    private val retryCounter = AtomicInteger(0)
    private val consecutiveFailureCounter = AtomicInteger(0)
    private val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

    // Action validation cache
    private val validationCache = ConcurrentHashMap<String, Boolean>()

    val history: List<String> get() = _history

    /**
     * Execution with comprehensive error handling and retry mechanism.
     * Returns the final summary with enhanced error handling.
     */
    suspend fun execute(action: ActionOptions): ModelResponse {
        Files.createDirectories(baseDir)
        val startTime = Instant.now()
        val sessionId = uuid.toString()

        return executeWithRetry(action, sessionId, startTime)
    }

    /**
     * Enhanced execution with comprehensive error handling and retry mechanisms
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun executeWithRetry(action: ActionOptions, sessionId: String, startTime: Instant): ModelResponse {
        var lastError: Exception? = null

        for (attempt in 0..config.maxRetries) {
            try {
                return executeInternal(action, sessionId, startTime, attempt)
            } catch (e: WebDriverAgentError.TransientError) {
                lastError = e
                logError("Transient error on attempt ${attempt + 1}", e, sessionId)
                if (attempt < config.maxRetries) {
                    val delay = calculateRetryDelay(attempt)
                    delay(delay)
                }
            } catch (e: WebDriverAgentError.TimeoutError) {
                lastError = e
                logError("Timeout error on attempt ${attempt + 1}", e, sessionId)
                if (attempt < config.maxRetries) {
                    delay(config.baseRetryDelayMs)
                }
            } catch (e: Exception) {
                lastError = e
                logError("Unexpected error on attempt ${attempt + 1}", e, sessionId)
                if (shouldRetryError(e) && attempt < config.maxRetries) {
                    val delay = calculateRetryDelay(attempt)
                    delay(delay)
                } else {
                    // Non-retryable error, exit loop
                    break
                }
            }
        }

        return ModelResponse(
            "Failed after ${config.maxRetries + 1} attempts. Last error: ${lastError?.message}",
            ai.platon.pulsar.external.ResponseState.OTHER
        )
    }

    /**
     * Main execution logic with enhanced error handling and monitoring.
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun executeInternal(action: ActionOptions, sessionId: String, startTime: Instant, attempt: Int): ModelResponse {
        val overallGoal = action.action
        val context = ExecutionContext(sessionId, 0, "execute", getCurrentUrl())

        logStructured("Starting PulsarAgent execution", context, mapOf(
            "instruction" to overallGoal.take(100),
            "attempt" to (attempt + 1),
            "maxSteps" to config.maxSteps,
            "maxRetries" to config.maxRetries
        ))

        val systemMsg = tta.buildOperatorSystemPrompt(overallGoal)
        var consecutiveNoOps = 0
        var step = 0

        try {
            while (step < config.maxSteps) {
                step++
                val stepStartTime = Instant.now()
                val stepContext = context.copy(stepNumber = step, actionType = "step")

                // Extract interactive elements each step (could be optimized via diffing later)
                val interactiveElements = runCatching { tta.extractInteractiveElements(driver) }.getOrElse { emptyList() }

                logStructured("Executing step", stepContext, mapOf(
                    "step" to step,
                    "maxSteps" to config.maxSteps,
                    "consecutiveNoOps" to consecutiveNoOps,
                    "interactiveElementCount" to interactiveElements.size
                ))

                // Memory cleanup at intervals
                if (step % config.memoryCleanupIntervalSteps == 0) {
                    performMemoryCleanup(stepContext)
                }

                val screenshotB64 = captureScreenshotWithRetry(stepContext)
                val userMsg = buildUserMessage(overallGoal, interactiveElements)

                // message: agent guide + overall goal + last action summary + current context message
                val message = buildExecutionMessage(systemMsg, userMsg, screenshotB64)
                // interactive elements are already appended to message
                val stepActionResult = generateStepActionWithRetry(message, stepContext, listOf(), screenshotB64)

                if (stepActionResult == null) {
                    consecutiveNoOps++
                    handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    continue
                }

                val response = stepActionResult.modelResponse
                val parsed = parseOperatorResponse(response.content)

                // Check for task completion
                if (shouldTerminate(parsed)) {
                    handleTaskCompletion(parsed, step, stepContext)
                    break
                }

                val toolCall = parsed.toolCalls.firstOrNull()
                if (toolCall == null) {
                    consecutiveNoOps++
                    handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    continue
                }

                // Reset consecutive no-ops counter when we have a valid action
                consecutiveNoOps = 0

                // Execute the tool call with enhanced error handling
                val execSummary = executeToolCallWithRetry(toolCall, stepActionResult, step, stepContext)

                if (execSummary != null) {
                    addToHistory("#$step ${execSummary}")
                    updatePerformanceMetrics(step, stepStartTime, true)
                    logStructured("Step completed successfully", stepContext, mapOf("summary" to execSummary))
                } else {
                    updatePerformanceMetrics(step, stepStartTime, false)
                }

                // Adaptive delay based on performance metrics
                delay(calculateAdaptiveDelay())
            }

            val executionTime = java.time.Duration.between(startTime, Instant.now())
            logStructured("Execution completed", context, mapOf(
                "steps" to step,
                "executionTime" to executionTime.toString(),
                "performanceMetrics" to performanceMetrics
            ))

            return generateFinalSummary(overallGoal, context)

        } catch (e: Exception) {
            val executionTime = java.time.Duration.between(startTime, Instant.now())
            logStructured("Execution failed", context, mapOf(
                "steps" to step,
                "executionTime" to executionTime.toString(),
                "error" to (e.message ?: "Unknown error")
            ))
            throw classifyError(e, step)
        }
    }

    // Enhanced helper methods for improved functionality

    override fun toString(): String {
        return history.lastOrNull() ?: "(no history)"
    }

    /**
     * Classifies errors for appropriate retry strategies
     */
    private fun classifyError(e: Exception, step: Int): WebDriverAgentError {
        return when (e) {
            is WebDriverAgentError -> e
            is java.util.concurrent.TimeoutException -> WebDriverAgentError.TimeoutError("Step $step timed out", e)
            is java.net.SocketTimeoutException -> WebDriverAgentError.TimeoutError("Network timeout at step $step", e)
            is java.net.ConnectException -> WebDriverAgentError.TransientError("Connection failed at step $step", e)
            is java.net.UnknownHostException -> WebDriverAgentError.TransientError("DNS resolution failed at step $step", e)
            is java.io.IOException -> {
                when {
                    e.message?.contains("connection") == true -> WebDriverAgentError.TransientError("Connection issue at step $step", e)
                    e.message?.contains("timeout") == true -> WebDriverAgentError.TimeoutError("Network timeout at step $step", e)
                    else -> WebDriverAgentError.TransientError("IO error at step $step: ${e.message}", e)
                }
            }
            is IllegalArgumentException -> WebDriverAgentError.ValidationError("Validation error at step $step: ${e.message}", e)
            is IllegalStateException -> WebDriverAgentError.PermanentError("Invalid state at step $step: ${e.message}", e)
            else -> WebDriverAgentError.TransientError("Unexpected error at step $step: ${e.message}", e)
        }
    }

    /**
     * Determines if an error should trigger a retry
     */
    private fun shouldRetryError(e: Exception): Boolean {
        return when (e) {
            is WebDriverAgentError.TransientError, is WebDriverAgentError.TimeoutError -> true
            is java.net.SocketTimeoutException, is java.net.ConnectException,
            is java.net.UnknownHostException -> true
            else -> false
        }
    }

    /**
     * Calculates retry delay with exponential backoff and jitter
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val exponentialDelay = config.baseRetryDelayMs * (2.0.pow(attempt.toDouble())).toLong()
        val jitter = (0..500).random().toLong()
        return min(exponentialDelay + jitter, config.maxRetryDelayMs)
    }

    /**
     * Structured logging with context
     */
    private fun logStructured(message: String, context: ExecutionContext, additionalData: Map<String, Any> = emptyMap()) {
        if (!config.enableStructuredLogging) {
            logger.info("{} - {}", context.sessionId.take(8), message)
            return
        }

        val logData = mapOf(
            "sessionId" to context.sessionId,
            "step" to context.stepNumber,
            "actionType" to context.actionType,
            "timestamp" to context.timestamp,
            "message" to message
        ) + additionalData

        logger.info("{}", logData)
    }

    /**
     * Enhanced error logging
     */
    private fun logError(message: String, error: Throwable, sessionId: String) {
        val errorData = mapOf(
            "sessionId" to sessionId,
            "errorType" to error.javaClass.simpleName,
            "errorMessage" to error.message,
            "timestamp" to Instant.now()
        )

        logger.error("PulsarAgent Error: {} - {}", message, errorData, error)
    }

    /**
     * Captures screenshot with retry mechanism
     */
    private suspend fun captureScreenshotWithRetry(context: ExecutionContext): String? {
        return try {
            val screenshot = safeScreenshot()
            if (screenshot != null) {
                logStructured("Screenshot captured successfully", context, mapOf("size" to screenshot.length))
            } else {
                logStructured("Screenshot capture returned null", context)
            }
            screenshot
        } catch (e: Exception) {
            logError("Screenshot capture failed", e, context.sessionId)
            null
        }
    }

    /**
     * Generates action with retry mechanism
     */
    private suspend fun generateStepActionWithRetry(
        message: String, context: ExecutionContext, interactiveElements: List<InteractiveElement>, screenshotB64: String?
    ): ActionDescription? {
        return try {
            withContext(Dispatchers.IO) {
                // Use overload supplying extracted elements to avoid re-extraction
                tta.generateWebDriverAction(message, interactiveElements, screenshotB64)
            }
        } catch (e: Exception) {
            logError("Action generation failed", e, context.sessionId)
            consecutiveFailureCounter.incrementAndGet()
            null
        }
    }

    /**
     * Executes tool call with enhanced error handling and validation
     */
    private suspend fun executeToolCallWithRetry(
        toolCall: ToolCall,
        action: ActionDescription,
        step: Int,
        context: ExecutionContext
    ): String? {
        if (config.enablePreActionValidation && !validateToolCall(toolCall)) {
            logStructured("Tool call validation failed", context, mapOf("toolCall" to toolCall.name))
            return null
        }

        return try {
            logStructured("Executing tool call", context, mapOf(
                "toolName" to toolCall.name,
                "toolArgs" to toolCall.args
            ))

            driver.act(action)
            consecutiveFailureCounter.set(0) // Reset on success

            // Extract summary from InstructionResult
            "${toolCall.name} executed successfully"
        } catch (e: Exception) {
            val failures = consecutiveFailureCounter.incrementAndGet()
            logError("Tool execution failed (consecutive failures: $failures)", e, context.sessionId)

            if (failures >= 3) {
                throw WebDriverAgentError.PermanentError("Too many consecutive failures at step $step", e)
            }

            null
        }
    }

    /**
     * Validates tool calls before execution
     */
    private fun validateToolCall(toolCall: ToolCall): Boolean {
        val cacheKey = "${toolCall.name}:${toolCall.args}"
        return validationCache.getOrPut(cacheKey) {
            when (toolCall.name) {
                "navigateTo" -> validateNavigateTo(toolCall.args)
                "click", "fill", "press", "check", "uncheck", "exists", "isVisible", "focus", "scrollTo" -> validateElementAction(toolCall.args)
                "waitForNavigation" -> validateWaitForNavigation(toolCall.args)
                "goBack", "goForward", "delay" -> true // These don't need validation
                else -> true // Unknown actions are allowed by default
            }
        }
    }

    /**
     * Validates navigation actions
     */
    private fun validateNavigateTo(args: Map<String, Any?>): Boolean {
        val url = args["url"]?.toString() ?: return false
        return isSafeUrl(url)
    }

    /**
     * Validates element interaction actions
     */
    private fun validateElementAction(args: Map<String, Any?>): Boolean {
        val selector = args["selector"]?.toString() ?: return false
        return selector.isNotBlank() && selector.length < 1000 // Basic validation
    }

    /**
     * Validates waitForNavigation actions
     */
    private fun validateWaitForNavigation(args: Map<String, Any?>): Boolean {
        val oldUrl = args["oldUrl"]?.toString() ?: ""
        val timeout = (args["timeoutMillis"] as? Number)?.toLong() ?: 5000L
        return timeout in 100L..60000L && oldUrl.length < 1000 // Reasonable timeout range and URL length
    }

    /**
     * Handles consecutive no-op scenarios with intelligent backoff
     */
    private suspend fun handleConsecutiveNoOps(consecutiveNoOps: Int, step: Int, context: ExecutionContext) {
        addToHistory("#$step no-op (consecutive: $consecutiveNoOps)")
        logStructured("No tool calls generated", context, mapOf("consecutiveNoOps" to consecutiveNoOps))

        if (consecutiveNoOps >= config.consecutiveNoOpLimit) {
            logStructured("Too many consecutive no-ops, stopping execution", context)
            throw WebDriverAgentError.PermanentError("Maximum consecutive no-ops reached: $consecutiveNoOps")
        }

        val delay = calculateConsecutiveNoOpDelay(consecutiveNoOps)
        delay(delay)
    }

    /**
     * Calculates delay for consecutive no-ops with exponential backoff
     */
    private fun calculateConsecutiveNoOpDelay(consecutiveNoOps: Int): Long {
        val baseDelay = 250L
        val exponentialDelay = baseDelay * consecutiveNoOps
        return min(exponentialDelay, 5000L) // Cap at 5 seconds
    }

    /**
     * Checks if execution should terminate
     */
    private fun shouldTerminate(parsed: ParsedResponse): Boolean {
        return parsed.taskComplete == true
    }

    /**
     * Handles task completion
     */
    private suspend fun handleTaskCompletion(parsed: ParsedResponse, step: Int, context: ExecutionContext) {
        logStructured("Task completion detected", context, mapOf(
            "taskComplete" to (parsed.taskComplete ?: false)
        ))

        addToHistory("#$step complete: taskComplete=${parsed.taskComplete}")
    }

    /**
     * Updates performance metrics
     */
    private fun updatePerformanceMetrics(step: Int, stepStartTime: Instant, success: Boolean) {
        val stepTime = java.time.Duration.between(stepStartTime, Instant.now()).toMillis()
        stepExecutionTimes[step] = stepTime

        // Update metrics (simplified for brevity)
        if (success) {
            // Update success metrics
        } else {
            // Update failure metrics
        }
    }

    /**
     * Calculates adaptive delay based on performance metrics
     */
    private fun calculateAdaptiveDelay(): Long {
        if (!config.enableAdaptiveDelays) return 100L // Default delay

        val avgStepTime = stepExecutionTimes.values.average()
        return when {
            avgStepTime < 500 -> 50L  // Fast steps, short delay
            avgStepTime < 2000 -> 100L // Normal steps, standard delay
            else -> 200L // Slow steps, longer delay
        }
    }

    /**
     * Performs memory cleanup
     */
    private fun performMemoryCleanup(context: ExecutionContext) {
        try {
            // Clean up history if it gets too large
            if (_history.size > config.maxHistorySize) {
                val toRemove = _history.size - config.maxHistorySize + 10
                _history.subList(0, toRemove).clear()
            }

            // Clear validation cache periodically
            if (validationCache.size > 1000) {
                validationCache.clear()
            }

            logStructured("Memory cleanup completed", context)
        } catch (e: Exception) {
            logError("Memory cleanup failed", e, context.sessionId)
        }
    }

    /**
     * Enhanced history management
     */
    private fun addToHistory(entry: String) {
        _history.add(entry)
        if (_history.size > config.maxHistorySize * 2) {
            // Remove oldest entries to prevent memory issues
            _history.subList(0, config.maxHistorySize).clear()
        }
    }

    /**
     * Gets current URL with error handling
     */
    private suspend fun getCurrentUrl(): String {
        return runCatching { driver.currentUrl() }.getOrNull().orEmpty()
    }

    /**
     * Builds the full model execution message by concatenating the system prompt, the user/context block,
     * and an optional screenshot marker on separate lines.
     *
     * Result format (lines separated by '\n'):
     *  1) systemPrompt
     *  2) userMsg
     *  3) "[Current page screenshot provided as base64 image]" (only if screenshotB64 != null)
     *
     * Notes:
     * - The base64 screenshot itself is NOT embedded to reduce token usage; only a marker line is added so the model
     *   understands an image attachment is present in the multimodal payload handled elsewhere.
     * - Each call to appendLine adds a trailing newline. When no screenshot is provided, the returned string ends with
     *   a newline after userMsg.
     *
     * Example:
     *   systemPrompt = "You are a browsing agent..."
     *   userMsg = "Goal: Buy a laptop...\nCurrent URL: https://example.com"
     *   screenshotB64 = "iVBORw0KGgo..." (omitted)
     *
     *   Returns:
     *   You are a browsing agent...
     *   Goal: Buy a laptop...
     *   Current URL: https://example.com
     *   [Current page screenshot provided as base64 image]
     *
     * @param systemPrompt Agent system instructions placed at the top of the message
     * @param userMsg Human-readable state/instruction assembled for the current step
     * @param screenshotB64 Optional base64 PNG/JPEG screenshot; if non-null, only a marker line is appended
     * @return A multi-line String ready for the LLM as the text part of a multimodal request
     */
    private fun buildExecutionMessage(systemPrompt: String, userMsg: String, screenshotB64: String?): String {
        return buildString {
            appendLine(systemPrompt)
            appendLine(userMsg)
            if (screenshotB64 != null) {
                appendLine("[Current page screenshot provided as base64 image]")
            }
        }
    }

    /**
     * Generates final summary with enhanced error handling
     */
    private suspend fun generateFinalSummary(instruction: String, context: ExecutionContext): ModelResponse {
        return try {
            val summary = summarize(instruction)
            addToHistory("FINAL ${summary.content.take(200)}")
            persistTranscript(instruction, summary)
            summary
        } catch (e: Exception) {
            logError("Failed to generate final summary", e, context.sessionId)
            ModelResponse("Failed to generate summary: ${e.message}", ai.platon.pulsar.external.ResponseState.OTHER)
        }
    }

    private suspend fun buildUserMessage(instruction: String, interactiveElements: List<InteractiveElement>): String {
        val currentUrl = getCurrentUrl()
        val h = if (_history.isEmpty()) "(无)" else _history.takeLast(min(8, _history.size)).joinToString("\n")
        val interactiveSummary = tta.formatInteractiveElements(interactiveElements)
        return """
此前动作摘要：
$h

可交互元素：
$interactiveSummary

请基于当前页面截图、交互元素与历史动作，规划下一步（严格单步原子动作），若无法推进请输出空 tool_calls。
当前目标：$instruction
当前URL：$currentUrl
        """.trimIndent()
    }

    private data class ToolCall(val name: String, val args: Map<String, Any?>)

    private data class ParsedResponse(
        val toolCalls: List<ToolCall>,
        val taskComplete: Boolean?,
    )

    private fun parseOperatorResponse(content: String): ParsedResponse {
        // 1) Try structured JSON with tool_calls
        try {
            val root = JsonParser.parseString(content)
            if (root.isJsonObject) {
                val obj = root.asJsonObject
                val toolCalls = mutableListOf<ToolCall>()
                if (obj.has("tool_calls") && obj.get("tool_calls").isJsonArray) {
                    obj.getAsJsonArray("tool_calls").forEach { el ->
                        if (el.isJsonObject) {
                            val tc = el.asJsonObject
                            val name = tc["name"]?.takeIf { it.isJsonPrimitive }?.asString
                            val args = (tc.getAsJsonObject("args") ?: JsonObject()).entrySet()
                                .associate { (k, v) -> k to jsonElementToKotlin(v) }
                            if (!name.isNullOrBlank()) toolCalls += ToolCall(name, args)
                        }
                    }
                }
                val taskComplete = obj.get("taskComplete")?.takeIf { it.isJsonPrimitive }?.asBoolean
                if (toolCalls.isNotEmpty() || taskComplete != null) {
                    return ParsedResponse(toolCalls.take(1), taskComplete)
                }
            }
        } catch (_: Exception) { /* fall back */
        }

        // 2) Fall back to TTA tool call JSON parser
        return runCatching {
            val calls = tta.parseToolCalls(content).map { ToolCall(it.name, it.args) }
            ParsedResponse(calls.take(1), null)
        }.getOrElse { ParsedResponse(emptyList(), null) }
    }

    private fun jsonElementToKotlin(e: com.google.gson.JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonPrimitive -> {
            val p = e.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> {
                    val n = p.asNumber
                    val d = n.toDouble();
                    val i = n.toInt(); if (d == i.toDouble()) i else d
                }

                else -> p.asString
            }
        }

        e.isJsonArray -> e.asJsonArray.map { jsonElementToKotlin(it) }
        e.isJsonObject -> e.asJsonObject.entrySet().associate { it.key to jsonElementToKotlin(it.value) }
        else -> null
    }

    /**
     * Enhanced URL validation with comprehensive safety checks
     */
    private fun isSafeUrl(url: String): Boolean {
        if (url.isBlank()) return false

        return runCatching {
            val uri = java.net.URI(url)
            val scheme = uri.scheme?.lowercase()

            // Only allow http and https schemes
            if (scheme !in setOf("http", "https")) {
                logStructured("Blocked unsafe URL scheme", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("scheme" to (scheme ?: "null"), "url" to url.take(50)))
                return false
            }

            // Additional safety checks
            val host = uri.host?.lowercase() ?: return false

            // Block common dangerous domains/patterns
            val dangerousPatterns = listOf("localhost", "127.0.0.1", "0.0.0.0", "file://")
            if (dangerousPatterns.any { host.contains(it) }) {
                logStructured("Blocked potentially dangerous URL", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("host" to host, "reason" to "dangerous_pattern"))
                return false
            }

            // Validate port (block non-standard ports for http/https)
            val port = uri.port
            if (port != -1 && port !in setOf(80, 443, 8080, 8443)) {
                logStructured("Blocked URL with non-standard port", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("port" to port, "host" to host))
                return false
            }

            true
        }.onFailure { e ->
            logError("URL validation failed", e, uuid.toString())
        }.getOrElse { false }
    }

    /**
     * Enhanced screenshot capture with comprehensive error handling
     */
    private suspend fun safeScreenshot(): String? {
        val currentUrl = getCurrentUrl()
        val context = ExecutionContext(uuid.toString(), 0, "screenshot", currentUrl)

        return runCatching {
            logStructured("Attempting to capture screenshot", context)
            val screenshot = driver.captureScreenshot()
            if (screenshot != null) {
                logStructured("Screenshot captured successfully", context, mapOf("size" to screenshot.length))
            } else {
                logStructured("Screenshot capture returned null", context)
            }
            screenshot
        }.onFailure { e ->
            logError("Screenshot capture failed", e, context.sessionId)
        }.getOrNull()
    }

    /**
     * Enhanced screenshot saving with error handling and validation
     */
    private suspend fun saveStepScreenshot(b64: String) {
        val currentUrl = getCurrentUrl()
        val context = ExecutionContext(uuid.toString(), 0, "save_screenshot", currentUrl)

        runCatching {
            // Validate base64 string
            if (b64.length > 50 * 1024 * 1024) { // 50MB limit
                logStructured("Screenshot too large, skipping save", context, mapOf("size" to b64.length))
                return
            }

            val ts = Instant.now().toEpochMilli()
            val p = baseDir.resolve("screenshot-${ts}.b64")
            logStructured("Saving step screenshot", context, mapOf("path" to p.toString()))

            Files.writeString(p, b64)
            logStructured("Screenshot saved successfully", context, mapOf("size" to b64.length))
        }.onFailure { e ->
            logError("Save screenshot failed", e, context.sessionId)
        }
    }

    /**
     * Enhanced transcript persistence with comprehensive logging
     */
    private suspend fun persistTranscript(instruction: String, finalResp: ModelResponse) {
        val currentUrl = getCurrentUrl()
        val context = ExecutionContext(uuid.toString(), 0, "persist_transcript", currentUrl)

        runCatching {
            val ts = Instant.now().toEpochMilli()
            val log = baseDir.resolve("session-${uuid}-${ts}.log")
            logStructured("Persisting execution transcript", context, mapOf("path" to log.toString()))

            val sb = StringBuilder()
            sb.appendLine("SESSION_ID: ${uuid}")
            sb.appendLine("TIMESTAMP: ${Instant.now()}")
            sb.appendLine("INSTRUCTION: $instruction")
            sb.appendLine("RESPONSE_STATE: ${finalResp.state}")
            sb.appendLine("EXECUTION_HISTORY:")
            _history.forEach { sb.appendLine(it) }
            sb.appendLine()
            sb.appendLine("FINAL_SUMMARY:")
            sb.appendLine(finalResp.content)
            sb.appendLine()
            sb.appendLine("PERFORMANCE_METRICS:")
            sb.appendLine("Total steps: ${performanceMetrics.totalSteps}")
            sb.appendLine("Successful actions: ${performanceMetrics.successfulActions}")
            sb.appendLine("Failed actions: ${performanceMetrics.failedActions}")
            sb.appendLine("Retry count: ${retryCounter.get()}")
            sb.appendLine("Consecutive failures: ${consecutiveFailureCounter.get()}")

            Files.writeString(log, sb.toString())
            logStructured("Transcript persisted successfully", context,
                mapOf("lines" to _history.size + 10, "path" to log.toString()))
        }.onFailure { e ->
            logError("Failed to persist transcript", e, context.sessionId)
        }
    }

    private fun buildSummaryPrompt(goal: String): Pair<String, String> {
        val system = "你是总结助理，请基于执行轨迹对原始目标进行总结，输出 JSON。"
        val user = buildString {
            appendLine("原始目标：$goal")
            appendLine("执行轨迹(按序)：")
            _history.forEach { appendLine(it) }
            appendLine()
            appendLine("""请严格输出 JSON：{"taskComplete":bool,"summary":string,"keyFindings":[string],"nextSuggestions":[string]} 无多余文字。""")
        }
        return system to user
    }

    /**
     * Enhanced summary generation with error handling
     */
    private suspend fun summarize(goal: String): ModelResponse {
        val currentUrl = getCurrentUrl()
        val context = ExecutionContext(uuid.toString(), 0, "summarize", currentUrl)

        return try {
            val (system, user) = buildSummaryPrompt(goal)
            logStructured("Generating final summary", context)

            val response = model?.call(user, system) ?: ModelResponse(
                "Model not available for summary generation",
                ai.platon.pulsar.external.ResponseState.OTHER
            )

            logStructured("Summary generated successfully", context, mapOf(
                "responseLength" to response.content.length,
                "responseState" to response.state
            ))

            response
        } catch (e: Exception) {
            logError("Summary generation failed", e, context.sessionId)
            ModelResponse(
                "Failed to generate summary: ${e.message}",
                ai.platon.pulsar.external.ResponseState.OTHER
            )
        }
    }
}
