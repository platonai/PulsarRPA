package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.agent.detail.*
import ai.platon.pulsar.agentic.ai.todo.ToDoManager
import ai.platon.pulsar.agentic.ai.tta.DetailedActResult
import ai.platon.pulsar.agentic.tools.ActionValidator
import ai.platon.pulsar.browser.driver.chrome.dom.util.DomDebug
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.*
import org.slf4j.helpers.MessageFormatter
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Configuration for enhanced error handling, retry mechanisms and agent behavior.
 *
 * Each field tunes a specific aspect of the autonomous loop:
 * - maxSteps: Upper bound of observe->act iterations in a single resolve session.
 * - maxRetries: Retries for the high-level resolve() in case of transient/timeout errors.
 * - baseRetryDelayMs/maxRetryDelayMs: Exponential backoff parameters.
 * - consecutiveNoOpLimit: Abort after N consecutive steps without actionable tool calls.
 * - actionGenerationTimeoutMs / llmInferenceTimeoutMs: Timeouts for model inference.
 * - screenshotCaptureTimeoutMs / screenshotEveryNSteps: Screenshot cadence & timeout.
 * - memoryCleanupIntervalSteps / maxHistorySize: In-memory history retention & cleanup.
 * - enableAdaptiveDelays: Adds short delays based on average step execution time.
 * - enablePreActionValidation: Validates tool calls before execution for safety.
 * - actTimeoutMs / resolveTimeoutMs: Overall upper bound for act() and resolve().
 * - maxResultsToTry: Number of candidate actions to attempt per model generation in act().
 * - domSettleTimeoutMs / domSettleCheckIntervalMs: Stabilization of DOM before each step.
 * - allowLocalhost / allowedPorts: URL safety policy.
 * - maxSelectorLength / denyUnknownActions: Selector validation & unknown action policy.
 * - todo* flags: Control integration with persistent todolist.md planning & progress.
 */
data class AgentConfig(
    val maxSteps: Int = 100,
    val maxRetries: Int = 3,
    val baseRetryDelayMs: Long = 1_000,
    val maxRetryDelayMs: Long = 30_000,
    val consecutiveNoOpLimit: Int = 5,
    val actionGenerationTimeoutMs: Long = 30_000,
    val screenshotCaptureTimeoutMs: Long = 5_000,
    val enableStructuredLogging: Boolean = false,
    val enableDebugMode: Boolean = false,
    val enablePerformanceMetrics: Boolean = true,
    val memoryCleanupIntervalSteps: Int = 50,
    val maxHistorySize: Int = 100,
    val enableAdaptiveDelays: Boolean = true,
    val enablePreActionValidation: Boolean = true,
    // New configuration options for fixes
    val actTimeoutMs: Long = 10.minutes.inWholeMilliseconds,
    val llmInferenceTimeoutMs: Long = 10.minutes.inWholeMilliseconds,
    val maxResultsToTry: Int = 3,
    val screenshotEveryNSteps: Int = 1,
    val domSettleTimeoutMs: Long = 5000,
    val domSettleCheckIntervalMs: Long = 100,
    val allowLocalhost: Boolean = false,
    val allowedPorts: Set<Int> = setOf(80, 443, 8080, 8443, 3000, 5000, 8000, 9000),
    val maxSelectorLength: Int = 1000,
    val denyUnknownActions: Boolean = false,
    // Overall timeout for resolve() to avoid indefinite hangs
    val resolveTimeoutMs: Long = 24.hours.inWholeMilliseconds,
    // Circuit breaker configuration
    val maxConsecutiveLLMFailures: Int = 5,
    val maxConsecutiveValidationFailures: Int = 8,
    // Checkpointing configuration
    val enableCheckpointing: Boolean = false,
    val checkpointIntervalSteps: Int = 10,
    val maxCheckpointsPerSession: Int = 5,
    // --- todolist.md integration flags ---
    val enableTodoWrites: Boolean = true,
    val todoPlanWithLLM: Boolean = true,
    val todoWriteProgressEveryStep: Boolean = true,
    val todoProgressWriteEveryNSteps: Int = 1,
    val todoMaxProgressLines: Int = 200,
    val todoEnableAutoCheck: Boolean = true,
    val todoTagsFromToolCall: Boolean = true,
)

open class BrowserPerceptiveAgent constructor(
    session: AgenticSession,
    val maxSteps: Int = 100,
    config: AgentConfig = AgentConfig(maxSteps = maxSteps)
) : BrowserAgentActor(session, config) {
    private val logger = getLogger(this)
    private val slogger = StructuredAgentLogger(logger, config)

    private val closed = AtomicBoolean(false)
    val isClosed: Boolean get() = closed.get()

    // A dedicated scope for all agent work so close() can cancel promptly
    private val agentJob = SupervisorJob()
    private val agentScope = CoroutineScope(Dispatchers.Default + agentJob)

    private val conf get() = session.sessionConfig

//    private val cta by lazy { ContextToAction(conf) }
//    internal val inference by lazy { InferenceEngine(session, cta.chatModel) }
//    internal val domService get() = inference.domService
//    internal val promptBuilder = PromptBuilder()
//    internal val toolExecutor by lazy { AgentToolManager(this) }

    private val todo: ToDoManager by lazy { ToDoManager(toolExecutor.fs, config, uuid, slogger) }

//    internal val activeDriver get() = session.getOrCreateBoundDriver()

    // Helper classes for better code organization
//    internal val pageStateTracker = PageStateTracker(session, config)
    private val actionValidator = ActionValidator()

    // Enhanced state management

//    internal val stateManager by lazy { AgentStateManager(this, pageStateTracker) }
    private val performanceMetrics = PerformanceMetrics()
    private val consecutiveFailureCounter = AtomicInteger(0)
    private val consecutiveLLMFailureCounter = AtomicInteger(0)
    private val consecutiveValidationFailureCounter = AtomicInteger(0)
    private val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

    // New components for better separation of concerns
    private val circuitBreaker = CircuitBreaker(
        maxLLMFailures = config.maxConsecutiveLLMFailures,
        maxValidationFailures = config.maxConsecutiveValidationFailures,
        maxExecutionFailures = 3
    )
    private val retryStrategy = RetryStrategy(
        maxRetries = config.maxRetries,
        baseDelayMs = config.baseRetryDelayMs,
        maxDelayMs = config.maxRetryDelayMs
    )
    private val retryCounter = AtomicInteger(0)
    private val checkpointManager = CheckpointManager(baseDir.resolve("checkpoints"))

    val baseDir get() = toolExecutor.baseDir

    override val uuid = UUID.randomUUID()
    override val stateHistory: List<AgentState> get() = stateManager.stateHistory
    override val processTrace: List<ProcessTrace> get() = stateManager.processTrace

    constructor(
        driver: WebDriver, session: AgenticSession, maxSteps: Int = 100,
        config: AgentConfig = AgentConfig(maxSteps = maxSteps)
    ) : this(session, maxSteps = maxSteps, config = config) {
        session.bindDriver(driver)
    }

    /**
     * High-level problem resolution entry. Builds an ActionOptions and delegates to resolve(ActionOptions).
     */
    override suspend fun resolve(problem: String): ActResult {
        val opts = ActionOptions(action = problem)
        return resolve(opts)
    }

    /**
     * Run an autonomous loop (observe -> act -> ...) attempting to fulfill the user goal described
     * in the ActionOptions. Applies retry and timeout strategies; records structured traces but keeps
     * stateHistory focused on executed tool actions only.
     */
    override suspend fun resolve(action: ActionOptions): ActResult {
        if (isClosed) return ActResult(false, "USER interrupted", action = action.action)
        return try {
            withContext(agentScope.coroutineContext) { resolveInCoroutine(action) }
        } catch (_: CancellationException) {
            ActResult(false, "USER interrupted", action = action.action)
        }
    }

    private suspend fun resolveInCoroutine(action: ActionOptions): ActResult {
        val instruction = action.action
        val context = stateManager.buildInitExecutionContext(action)
        val sessionStartTime = context.timestamp

        // Add start history for better traceability (meta record only)
        val goal = Strings.compactLog(instruction, 160)
        stateManager.trace(
            context.agentState,
            mapOf(
                "event" to "resolveStart",
                "session" to context.sid,
                "goal" to goal,
                "maxSteps" to config.maxSteps,
                "maxRetries" to config.maxRetries
            ),
            "🚀 resolve START"
        )

        // Overall timeout to prevent indefinite hangs for a full resolve session
        // Calculate effective timeout accounting for potential retry delays
        val maxPossibleDelays = (0 until config.maxRetries).fold(0L) { acc, i -> acc + calculateRetryDelay(i) }
        val effectiveTimeout = config.resolveTimeoutMs + maxPossibleDelays

        return try {
            val result = withTimeout(effectiveTimeout) {
                resolveProblemWithRetry(action, context)
            }

            val dur = Duration.between(sessionStartTime, Instant.now()).toMillis()
            // Not a single-step action, keep it out of AgentState history
            stateManager.trace(
                context.agentState, mapOf(
                    "event" to "resolveDone", "session" to context.sid,
                    "success" to result.success, "durationMs" to dur
                ), "✅ resolve DONE"
            )

            result
        } catch (_: TimeoutCancellationException) {
            val msg = "⏳ Resolve timed out after ${effectiveTimeout}ms (base: ${config.resolveTimeoutMs}ms + " +
                    "retries: ${maxPossibleDelays}ms): $instruction"
            stateManager.trace(
                context.agentState, mapOf(
                    "event" to "resolveTimeout",
                    "timeoutMs" to effectiveTimeout, "instruction" to Strings.compactLog(instruction, 160)
                ),
                "⏳ resolve TIMEOUT"
            )
            ActResult(success = false, message = msg, action = instruction)
        }
    }

    /**
     * Convenience wrapper building ActionOptions from a raw action string.
     */
    override suspend fun act(action: String): ActResult {
        val opts = ActionOptions(action = action)
        return act(opts)
    }

    /**
     * Executes a single observe->act cycle for a supplied ActionOptions. Times out after actTimeoutMs
     * to prevent indefinite hangs. Model may produce multiple candidate tool calls internally; only
     * one successful execution is recorded in stateHistory.
     */
    override suspend fun act(action: ActionOptions): ActResult {
        if (isClosed) return ActResult(false, "USER interrupted", action = action.action)
        return try {
            withContext(agentScope.coroutineContext) {
                super.act(action)
                // actInCoroutine(action)
            }
        } catch (_: CancellationException) {
            ActResult(false, "USER interrupted", action = action.action)
        }
    }

    private suspend fun actInCoroutine(action: ActionOptions): ActResult {
        val context = stateManager.buildInitExecutionContext(action)
        val action = if (action.agentState == null) {
            action.copy(agentState = context.agentState)
        } else action

        return try {
            withTimeout(config.actTimeoutMs) {
                val messages = AgentMessageList()
                doObserveAct(action, messages)
            }
        } catch (_: TimeoutCancellationException) {
            val msg = "⏳ Action timed out after ${config.actTimeoutMs}ms: ${action.action}"
            stateManager.trace(
                context.agentState,
                mapOf(
                    "event" to "actTimeout",
                    "timeoutMs" to config.actTimeoutMs,
                    "instruction" to action.action
                ),
                "⏳ act TIMEOUT"
            )
            ActResult(success = false, message = msg, action = action.action)
        }
    }

    /**
     * Executes a tool call derived from a prior observation result. Performs patching (selector/url),
     * validation, and updates AgentState history on success or failure.
     */
    override suspend fun act(observe: ObserveResult): ActResult {
        if (isClosed) return ActResult(false, "USER interrupted", action = observe.agentState.instruction)
        return try {
            withContext(agentScope.coroutineContext) {
                super.act(observe)
                // actInCoroutine(observe)
            }
        } catch (_: CancellationException) {
            ActResult(false, "USER interrupted", action = observe.agentState.instruction)
        }
    }

    private suspend fun actInCoroutine(observe: ObserveResult): ActResult {
        val instruction = observe.agentState.instruction
        val agentState = observe.agentState
        val observeElement = observe.observeElement
        observeElement ?: return ActResult.failed("No observation", instruction)
        val actionDescription =
            observe.actionDescription ?: return ActResult.failed("No action description", instruction)
        val toolCall = observeElement.toolCall ?: return ActResult.failed("No tool call", instruction)
        val method = toolCall.method

        return try {
            val context = stateManager.buildExecutionContext(instruction, "step")
            val detailedActResult = actInternal(actionDescription, context)
            val toolCallResult = detailedActResult?.toolCallResult

            logger.info(
                "✅ Action executed | {} | {}/{} | {}",
                method, observe.locator, observeElement.cssSelector, observeElement.cssFriendlyExpression
            )

            val msg = MessageFormatter.arrayFormat(
                "✅ Action executed | {} | {}/{} | {}",
                arrayOf(method, observe.locator, observeElement.cssSelector, observeElement.cssFriendlyExpression)
            )

            stateManager.updateAgentState(agentState, observeElement, toolCall, toolCallResult, msg.message)

            ActResult(success = true, action = toolCall.method, message = msg.message, result = toolCallResult)
        } catch (e: Exception) {
            logger.error("❌ observe.act execution failed sid={} msg={}", uuid.toString().take(8), e.message, e)
            val msg = e.message ?: "Execution failed"

            stateManager.updateAgentState(agentState, observeElement, toolCall, null, msg, success = false)

            ActResult(success = false, message = msg, action = toolCall.method)
        }
    }

    /**
     * Structured extraction: builds a rich prompt with DOM snapshot & optional JSON schema; performs
     * two-stage LLM calls (extract + metadata) and merges results with token/time metrics.
     */
    override suspend fun extract(options: ExtractOptions): ExtractResult {
        if (isClosed) return ExtractResult(
            success = false,
            message = "USER interrupted",
            data = JsonNodeFactory.instance.objectNode()
        )
        return try {
            withContext(agentScope.coroutineContext) {
                super.extract(options)
                // extractInCoroutine(options)
            }
        } catch (_: CancellationException) {
            ExtractResult(success = false, message = "USER interrupted", data = JsonNodeFactory.instance.objectNode())
        }
    }

    private suspend fun extractInCoroutine(options: ExtractOptions): ExtractResult {
        val instruction = promptBuilder.initExtractUserInstruction(options.instruction)
        val context = stateManager.buildExecutionContext(instruction, "extract")
        logExtractStart(context)

        return try {
            val params = context.createExtractParams(options.schema)
            val resultNode = inference.extract(params)
            addHistoryExtract(instruction, context.sid, true)
            ExtractResult(success = true, message = "OK", data = resultNode)
        } catch (e: Exception) {
            logger.error("❌ extract.error requestId={} msg={}", context.sid, e.message, e)
            addHistoryExtract(instruction, context.sid, false)
            ExtractResult(
                success = false, message = e.message ?: "extract failed", data = JsonNodeFactory.instance.objectNode()
            )
        }
    }

    /**
     * Convenience overload for structured extraction. When only an instruction string is provided,
     * it uses the built-in ExtractionSchema.DEFAULT.
     *
     * @param instruction The extraction instruction from the user.
     * @return The extraction result produced by the model.
     */
    override suspend fun extract(instruction: String): ExtractResult {
        val opts = ExtractOptions(instruction = instruction, ExtractionSchema.DEFAULT)
        return extract(opts)
    }

    /**
     * Convenience overload for structured extraction that constrains the result with a JSON schema.
     *
     * @param instruction The extraction instruction from the user.
     * @param schema The JSON schema used to constrain the returned data structure.
     * @return The extraction result produced by the model.
     */
    override suspend fun extract(instruction: String, schema: ExtractionSchema): ExtractResult {
        val opts = ExtractOptions(instruction = instruction, schema = schema)
        return extract(opts)
    }

    /**
     * Observes the page given an instruction, returning zero or more ObserveResult objects describing
     * candidate elements and potential actions (if returnAction=true).
     */
    override suspend fun observe(options: ObserveOptions): List<ObserveResult> {
        if (isClosed) return emptyList()
        return withContext(agentScope.coroutineContext) {
            super.observe(options)
            // observeInCoroutine(options)
        }
    }

    override suspend fun observe(instruction: String): List<ObserveResult> {
        val opts = ObserveOptions(instruction = instruction, returnAction = null)
        return observe(opts)
    }

    private suspend fun observeInCoroutine(options: ObserveOptions): List<ObserveResult> {
        val messages = AgentMessageList()
        val instruction = promptBuilder.initObserveUserInstruction(options.instruction)
        messages.addUser(instruction, name = "user_request")

        val context = (options.additionalContext["context"]?.get() as? ExecutionContext)
            ?: throw IllegalStateException("Illegal context")

        val actionDescription = takeScreenshotAndObserve(options, context, messages)

        return actionDescription.toObserveResults(context.agentState)
    }

    fun stop() = close()

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { agentJob.cancel(CancellationException("USER interrupted via close()")) }
            // Best-effort trace for visibility; avoid throwing
            runCatching {
                val last = stateHistory.lastOrNull()
                stateManager.trace(last, mapOf("event" to "userClose"), "🛑 USER CLOSE")
            }
        }
    }

    /**
     * Returns a concise summary of the latest agent state; if no history exists, returns a placeholder text.
     */
    override fun toString(): String {
        return stateHistory.lastOrNull()?.toString() ?: "(no history)"
    }

    protected suspend fun generateActions(context: ExecutionContext): ActionDescription {
        val screenshotB64 = if (context.step % config.screenshotEveryNSteps == 0) {
            if (isClosed) return ActionDescription(context.instruction, exception = CancellationException("closed"))
            captureScreenshotWithRetry(context)
        } else null
        context.screenshotB64 = screenshotB64

        // Prepare messages for model
        val messages = promptBuilder.buildResolveObserveMessageList(context, stateHistory)

        return try {
            if (isClosed) throw CancellationException("closed")
            val action = cta.generate(messages, context)
            circuitBreaker.recordSuccess(CircuitBreaker.FailureType.LLM_FAILURE)
            consecutiveLLMFailureCounter.set(0) // Keep for backward compatibility
            action
        } catch (e: Exception) {
            val failures = try {
                circuitBreaker.recordFailure(CircuitBreaker.FailureType.LLM_FAILURE)
            } catch (cbError: CircuitBreakerTrippedException) {
                throw PerceptiveAgentError.PermanentError(cbError.message ?: "Circuit breaker tripped", cbError)
            }

            consecutiveLLMFailureCounter.set(failures) // Keep for backward compatibility
            logger.error("🤖❌ action.gen.fail sid={} failures={} msg={}", context.sid, failures, e.message, e)
            consecutiveFailureCounter.incrementAndGet()

            ActionDescription(context.instruction, exception = e, modelResponse = ModelResponse.INTERNAL_ERROR)
        }
    }

    protected suspend fun ensureReadyForStep(action: ActionOptions) {
        val driver = activeDriver
        val url = driver.url()
        if (url.isBlank() || url == "about:blank") {
            driver.navigateTo(AppConstants.SEARCH_ENGINE_URL)
        }

        // Only wait for DOM settle just before collection DOM tree data
//        val settleMs = action.domSettleTimeoutMs?.toLong()?.coerceAtLeast(0L) ?: config.domSettleTimeoutMs
//        if (settleMs > 0) {
//            pageStateTracker.waitForDOMSettle(settleMs, config.domSettleCheckIntervalMs)
//        }
    }

    private suspend fun doResolveProblem(
        action: ActionOptions,
        initContext: ExecutionContext,
        attempt: Int
    ): ActResult {
        initializeResolution(initContext, attempt)
        var consecutiveNoOps = 0
        var context = initContext
        val startTime = Instant.now()
        try {
            while (!isClosed && context.step < config.maxSteps) {
                val stepResult = processSingleStep(action, context, consecutiveNoOps)
                context = stepResult.context
                consecutiveNoOps = stepResult.consecutiveNoOps
                if (stepResult.shouldStop) break
            }

            return buildFinalActResult(initContext.instruction, context, startTime)
        } catch (_: CancellationException) {
            logger.info("""🛑 [USER interrupted] sid={} steps={}""", context.sid, context.step)
            return ActResult(success = false, message = "USER interrupted", action = initContext.instruction)
        } catch (e: Exception) {
            throw handleResolutionFailure(e, context, startTime)
        }
    }

    private suspend fun doObserveAct(options: ActionOptions, messages: AgentMessageList): ActResult {
        val instruction = promptBuilder.buildObserveActToolUsePrompt(options.action)
        messages.addUser(instruction, "user_request")

        val context = stateManager.buildExecutionContext(instruction, "observeAct", agentState = options.agentState)

        val actionDescription = takeScreenshotAndObserve(options, context, messages)

        val observeResults = actionDescription.toObserveResults(context.agentState)

        if (observeResults.isEmpty()) {
            val msg = "⚠️ doObserveAct: No actionable element found"
            stateManager.trace(context.agentState, mapOf("event" to "observeActNoAction"), msg)
            return ActResult(false, msg, action = instruction)
        }

        val resultsToTry = observeResults.take(config.maxResultsToTry)
        var lastError: String? = null
        val actResults = mutableListOf<ActResult>()
        for ((index, chosen) in resultsToTry.withIndex()) {
            if (isClosed) return ActResult(false, "USER interrupted", action = instruction)
            val method = chosen.method?.trim().orEmpty()
            if (method.isBlank()) {
                lastError = "LLM returned no method for candidate ${index + 1}"
                continue
            }

            val actResult = try {
                act(chosen).also { actResults.add(it) }
            } catch (e: Exception) {
                lastError = "Execution failed for candidate ${index + 1}: ${e.message}"
                logger.warn("⚠️ Failed to execute candidate {}: {}", index + 1, e.message)
                continue
            }

            if (!actResult.success) {
                lastError = "Candidate ${index + 1} failed: ${actResult.message}"
                continue
            }

            stateManager.trace(
                context.agentState,
                mapOf(
                    "event" to "actSuccess",
                    "candidateIndex" to (index + 1),
                    "candidateTotal" to resultsToTry.size
                ),
                "✅ act SUCCESS"
            )
            return actResult
        }

        val msg = "❌ All ${resultsToTry.size} candidates failed. Last error: $lastError"
        stateManager.trace(context.agentState, mapOf("event" to "actAllFailed", "candidates" to resultsToTry.size), msg)
        return ActResult.failed(msg, instruction)
    }

    private suspend fun takeScreenshotAndObserve(
        options: Any,
        context: ExecutionContext,
        messages: AgentMessageList
    ): ActionDescription {
        val interactiveElements = context.agentState.browserUseState.getInteractiveElements()

        val actionDescription = try {
            domService.addHighlights(interactiveElements)

            if (isClosed) return ActionDescription(context.instruction, exception = CancellationException("closed"))
            val screenshotB64 = captureScreenshotWithRetry(context)

            val params = when (options) {
                is ObserveOptions -> context.createObserveParams(options, false, screenshotB64)
                is ActionOptions -> context.createObserveActParams(screenshotB64)
                else -> throw IllegalArgumentException("Not supported option")
            }

            observeAndInference(params, messages)
        } finally {
            runCatching { domService.removeHighlights(interactiveElements) }
                .onFailure { e -> logger.warn("⚠️ Failed to remove highlights: ${e.message}") }
        }

        return actionDescription
    }

    private suspend fun observeAndInference(params: ObserveParams, messages: AgentMessageList): ActionDescription {
        requireNotNull(messages.instruction) { "User instruction is required | $messages" }
        requireNotNull(params.agentState) { "Agent state has to be available" }
        require(params.instruction == messages.instruction?.content)

        val instruction = params.instruction
        val requestId: String = params.requestId

        return try {
            val actionDescription = withTimeout(config.llmInferenceTimeoutMs) {
                inference.observe(params, messages)
            }

            val results = actionDescription.observeElements ?: emptyList()
            addHistoryObserve(instruction, requestId, results.size, results.isNotEmpty())
            return actionDescription
        } catch (e: Exception) {
            logger.error("❌ observeAct.observe.error requestId={} msg={}", requestId.take(8), e.message, e)
            addHistoryObserve(instruction, requestId, 0, false)
            ActionDescription(instruction, exception = e, modelResponse = ModelResponse.INTERNAL_ERROR)
        }
    }

    private fun logExtractStart(context: ExecutionContext) {
        logger.info(
            "🔍 extract.start requestId={} instruction='{}'",
            context.sid, PromptBuilder.compactPrompt(context.instruction, 200)
        )
    }

    private fun logObserveStart(instruction: String, requestId: String) {
        logger.info(
            "👀 observe.start requestId={} instruction='{}'",
            requestId.take(8),
            PromptBuilder.compactPrompt(instruction, 200)
        )
    }

    private fun addHistoryExtract(instruction: String, requestId: String, success: Boolean) {
        val compactPrompt = PromptBuilder.compactPrompt(instruction, 200)
        // Extraction is not a tool action; keep it in record history only
        stateManager.trace(
            stateHistory.lastOrNull(),
            mapOf(
                "event" to "extract",
                "requestId" to requestId.take(8),
                "success" to success
            ),
            "🔍 extract $compactPrompt"
        )
    }

    private fun addHistoryObserve(instruction: String, requestId: String, size: Int, success: Boolean) {
        stateManager.trace(
            stateHistory.lastOrNull(),
            mapOf(
                "event" to "observe",
                "requestId" to requestId.take(8),
                "success" to success,
                "size" to size,
                "instruction" to PromptBuilder.compactPrompt(instruction, 200)
            ),
            "👀 observe"
        )
    }

    /**
     * Enhanced execution with comprehensive error handling and retry mechanisms
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun resolveProblemWithRetry(
        action: ActionOptions, context: ExecutionContext
    ): ActResult {
        var lastError: Exception? = null
        val sid = context.sid
        var currentContext = context

        for (attempt in 0..config.maxRetries) {
            val attemptNo = attempt + 1

            stateManager.trace(
                currentContext.agentState,
                mapOf(
                    "event" to "resolveAttempt", "attemptNo" to attemptNo,
                    "attemptsTotal" to (config.maxRetries + 1)
                ),
                "🔁 resolve ATTEMPT"
            )

            try {
                val res = doResolveProblem(action, currentContext, attempt)

                stateManager.trace(
                    currentContext.agentState,
                    mapOf("event" to "resolveAttemptOk", "attemptNo" to attemptNo),
                    "✅ resolve ATTEMPT OK"
                )

                return res
            } catch (e: PerceptiveAgentError.TransientError) {
                lastError = e
                logger.error("🔄 resolve.transient attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.trace(
                        currentContext.agentState,
                        mapOf(
                            "event" to "resolveRetry",
                            "cause" to "Transient",
                            "attemptNo" to attemptNo,
                            "delayMs" to backoffMs,
                            "msg" to (e.message ?: "")
                        ),
                        "🔁 resolve RETRY"
                    )

                    // Clean up partial state before retry
                    try {
                        cleanupPartialState(currentContext)
                        // Rebuild context for next attempt to avoid corrupted state
                        currentContext = stateManager.buildInitExecutionContext(action)
                    } catch (cleanupError: Exception) {
                        logger.warn("⚠️ Failed to cleanup state before retry: ${cleanupError.message}")
                    }

                    delay(backoffMs)
                }
            } catch (e: PerceptiveAgentError.TimeoutError) {
                lastError = e
                logger.error("⏳ resolve.timeout attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val baseBackoffMs = config.baseRetryDelayMs
                    stateManager.trace(
                        currentContext.agentState,
                        mapOf(
                            "event" to "resolveRetry",
                            "cause" to "Timeout",
                            "attemptNo" to attemptNo,
                            "delayMs" to baseBackoffMs,
                            "msg" to (e.message ?: "")
                        ),
                        "🔁 resolve RETRY"
                    )

                    // Clean up partial state before retry
                    try {
                        cleanupPartialState(currentContext)
                        currentContext = stateManager.buildInitExecutionContext(action)
                    } catch (cleanupError: Exception) {
                        logger.warn("⚠️ Failed to cleanup state before retry: ${cleanupError.message}")
                    }

                    delay(baseBackoffMs)
                }
            } catch (e: Exception) {
                lastError = e
                logger.error("💥 resolve.unexpected attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (shouldRetryError(e) && attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.trace(
                        currentContext.agentState,
                        mapOf(
                            "event" to "resolveRetry",
                            "cause" to "Unexpected",
                            "attemptNo" to attemptNo,
                            "delayMs" to backoffMs,
                            "msg" to (e.message ?: "")
                        ),
                        "🔁 resolve RETRY"
                    )

                    // Clean up partial state before retry
                    try {
                        cleanupPartialState(currentContext)
                        currentContext = stateManager.buildInitExecutionContext(action)
                    } catch (cleanupError: Exception) {
                        logger.warn("⚠️ Failed to cleanup state before retry: ${cleanupError.message}")
                    }

                    delay(backoffMs)
                } else {
                    // Non-retryable error, exit loop
                    break
                }
            }
        }

        stateManager.trace(
            currentContext.agentState,
            mapOf(
                "event" to "resolveFail", "attemptsTotal" to (config.maxRetries + 1), "msg" to lastError?.message
            ),
            "❌ resolve FAIL"
        )

        return ActResult(
            success = false,
            message = "Failed after ${config.maxRetries + 1} attempts. Last error: ${lastError?.message}",
            action = action.action
        )
    }

    private suspend fun initializeResolution(initContext: ExecutionContext, attempt: Int) {
        val sid = initContext.sid
        logger.info(
            "🚀 agent.start sid={} step={} url={} instr='{}' attempt={} maxSteps={} maxRetries={}",
            sid, initContext.step, initContext.targetUrl, Strings.compactLog(initContext.instruction, 100),
            attempt + 1, config.maxSteps, config.maxRetries
        )
        if (config.enableTodoWrites) {
            runCatching { todo.primeIfEmpty(initContext.instruction, initContext.targetUrl) }
                .onFailure { e -> slogger.logError("📝❌ todo.prime.fail", e, sid) }
        }
    }

    private data class StepProcessingResult(
        val context: ExecutionContext,
        val consecutiveNoOps: Int,
        val shouldStop: Boolean
    )

    private suspend fun processSingleStep(
        action: ActionOptions,
        ctxIn: ExecutionContext,
        noOpsIn: Int
    ): StepProcessingResult {
        var context = ctxIn
        var consecutiveNoOps = noOpsIn
        val nextStep = context.step + 1

        ensureReadyForStep(action)

        context = stateManager.buildExecutionContext(action.action, "step", nextStep, baseContext = context)
        val agentState = context.agentState
        val browserUseState = agentState.browserUseState
        val step = context.step
        val sid = context.sid

        val unchangedCount = pageStateTracker.checkStateChange(browserUseState)
        if (unchangedCount >= 3) {
            logger.info("⚠️ loop.warn sid={} step={} unchangedSteps={}", sid, step, unchangedCount); consecutiveNoOps++
        }

        logger.info("▶️ step.exec sid={} step={}/{} noOps={}", sid, step, config.maxSteps, consecutiveNoOps)
        if (logger.isDebugEnabled) logger.debug("🧩 dom={}", DomDebug.summarizeStr(browserUseState.domState, 5))
        if (step % config.memoryCleanupIntervalSteps == 0) performMemoryCleanup(context)
        if (config.enableCheckpointing && step % config.checkpointIntervalSteps == 0) {
            runCatching { saveCheckpoint(context) }
                .onFailure { e -> logger.warn("💾❌ checkpoint.save.fail sid={} step={} msg={}", sid, step, e.message) }
        }

        val actionDescription = generateActions(context)

        if (logger.isDebugEnabled) {
            logger.debug("Let's make a final step decision | {}", actionDescription.modelResponse)
        }

        if (shouldTerminate(actionDescription)) {
            onTaskCompletion(actionDescription, context)
            return StepProcessingResult(context, consecutiveNoOps, true)
        }

        if (actionDescription.toolCall == null) {
            consecutiveNoOps++
            val stop = handleConsecutiveNoOps(consecutiveNoOps, context)
            updatePerformanceMetrics(step, context.timestamp, false)
            if (stop) return StepProcessingResult(context, consecutiveNoOps, true)
            delay(calculateAdaptiveDelay())
            return StepProcessingResult(context, consecutiveNoOps, false)
        }

        consecutiveNoOps = 0
        val detailedActResult = actInternal(actionDescription, context)
        if (detailedActResult != null) {
            stateManager.updateAgentState(context.agentState, detailedActResult)
            updateTodo(context, detailedActResult)
            updatePerformanceMetrics(step, context.timestamp, true)
            val preview = detailedActResult.toolCallResult?.evaluate?.preview
            logger.info("🏁 step.done sid={} step={} tcResult={}", sid, step, preview)
        } else {
            consecutiveNoOps++
            val stop = handleConsecutiveNoOps(consecutiveNoOps, context)
            updatePerformanceMetrics(step, context.timestamp, false)
            if (stop) return StepProcessingResult(context, consecutiveNoOps, true)
        }
        delay(calculateAdaptiveDelay())
        return StepProcessingResult(context, consecutiveNoOps, false)
    }

    // ===== Helper methods appended for modularization =====

    private fun classifyError(e: Exception, step: Int): PerceptiveAgentError {
        return retryStrategy.classifyError(e, "step $step")
    }

    private fun shouldRetryError(e: Exception): Boolean {
        return retryStrategy.shouldRetry(e)
    }

    private fun calculateRetryDelay(attempt: Int): Long {
        return retryStrategy.calculateDelay(attempt)
    }

    private suspend fun cleanupPartialState(context: ExecutionContext) {
        try {
            logger.info("🧹 cleanup.partial sid={} step={}", context.sid, context.step)
            circuitBreaker.reset()
            consecutiveFailureCounter.set(0)
            consecutiveLLMFailureCounter.set(0)
            consecutiveValidationFailureCounter.set(0)
            actionValidator.clearCache()
            // pageStateTracker.waitForDOMSettle(1000, 100)
        } catch (e: Exception) {
            logger.warn("⚠️ cleanup.partial.fail sid={} msg={}", context.sid, e.message)
        }
    }

    private suspend fun actInternal(
        actionDescription: ActionDescription,
        context: ExecutionContext
    ): DetailedActResult? {
        val step = context.step
        val toolCall = actionDescription.toolCall ?: return null

        if (config.enablePreActionValidation && !actionValidator.validateToolCall(toolCall)) {
            val failures = try {
                circuitBreaker.recordFailure(CircuitBreaker.FailureType.VALIDATION_FAILURE)
            } catch (cbError: CircuitBreakerTrippedException) {
                throw PerceptiveAgentError.PermanentError(cbError.message ?: "Circuit breaker tripped", cbError)
            }
            consecutiveValidationFailureCounter.set(failures)
            logger.info(
                "🛑 tool.validate.fail sid={} step={} failures={} locator={} | {}({}) | {}",
                context.sid, context.step, failures, actionDescription.locator, toolCall.method, toolCall.arguments,
                actionDescription.cssFriendlyExpression
            )
            stateManager.trace(
                context.agentState,
                mapOf("event" to "validationFailed", "step" to step, "tool" to toolCall.method),
                "🛑 validation-failed"
            )
            return null
        }

        circuitBreaker.recordSuccess(CircuitBreaker.FailureType.VALIDATION_FAILURE)
        consecutiveValidationFailureCounter.set(0)

        return try {
            logger.info(
                "🛠️ tool.exec sid={} step={} tool={} args={}",
                context.sid,
                context.step,
                toolCall.method,
                toolCall.arguments
            )

            val toolCallResult = toolExecutor.execute(actionDescription, "resolve, #$step")

            circuitBreaker.recordSuccess(CircuitBreaker.FailureType.EXECUTION_FAILURE)
            consecutiveFailureCounter.set(0)
            val summary = "✅ ${toolCall.method} executed successfully"
            stateManager.trace(context.agentState, mapOf("event" to "toolExecOk", "tool" to toolCall.method), summary)
            DetailedActResult(actionDescription, toolCallResult, success = true, summary)
        } catch (e: Exception) {
            val failures = try {
                circuitBreaker.recordFailure(CircuitBreaker.FailureType.EXECUTION_FAILURE)
            } catch (cbError: CircuitBreakerTrippedException) {
                throw PerceptiveAgentError.PermanentError(cbError.message ?: "Circuit breaker tripped", cbError)
            }
            consecutiveFailureCounter.set(failures)
            logger.error(
                "🛠️❌ tool.exec.fail sid={} step={} failures={} msg={}",
                context.sid,
                context.step,
                failures,
                e.message,
                e
            )
            stateManager.trace(
                context.agentState,
                mapOf("event" to "toolExecUnexpectedFail", "tool" to toolCall.method),
                "💥 unexpected failure"
            )
            null
        }
    }

    private suspend fun updateTodo(context: ExecutionContext, detailedActResult: DetailedActResult) {
        if (!config.enableTodoWrites) return
        val sid = context.sessionId
        val step = context.step
        val toolCall = detailedActResult.actionDescription.toolCall
        val observeElement = detailedActResult.actionDescription.observeElement
        val shouldWrite = config.todoWriteProgressEveryStep ||
                (config.todoProgressWriteEveryNSteps > 1 && step % config.todoProgressWriteEveryNSteps == 0)
        if (!shouldWrite) return
        val urlNow0 = activeDriver.currentUrl()
        runCatching {
            todo.appendProgress(step, toolCall, observeElement, urlNow0, detailedActResult.summary)
            todo.updateProgressCounter()
            if (config.todoEnableAutoCheck) {
                val tags = todo.buildTags(toolCall, urlNow0)
                if (tags.isNotEmpty()) todo.markPlanItemDoneByTags(tags)
            }
        }.onFailure { e -> slogger.logError("📝❌ todo.progress.fail", e, sid) }
    }

    private fun performMemoryCleanup(context: ExecutionContext) {
        try {
            synchronized(stateManager) {
                if (stateHistory.size > config.maxHistorySize) {
                    val toRemove = stateHistory.size - config.maxHistorySize + 10
                    stateManager.clearUpHistory(toRemove)
                }
            }
            actionValidator.clearCache()
            logger.info("🧹 mem.cleanup sid={} step={} historySize={}", context.sid, context.step, stateHistory.size)
        } catch (e: Exception) {
            logger.error("🧹❌ mem.cleanup.fail sid={} msg={}", context.sid, e.message, e)
        }
    }

    protected suspend fun summarize(goal: String, context: ExecutionContext): ModelResponse {
        return try {
            val (system, user) = promptBuilder.buildSummaryPrompt(goal, stateHistory)
            slogger.info("📝⏳ Generating final summary", context)
            val response = cta.chatModel.callUmSm(user, system)
            slogger.info(
                "📝✅ Summary generated successfully", context,
                mapOf("responseLength" to response.content.length, "responseState" to response.state)
            )
            response
        } catch (e: Exception) {
            slogger.logError("📝❌ Summary generation failed", e, context.sessionId)
            ModelResponse("Failed to generate summary: ${e.message}", ResponseState.OTHER)
        }
    }

    protected suspend fun generateFinalSummary(instruction: String, context: ExecutionContext): ModelResponse {
        return try {
            val summary = summarize(instruction, context)
            stateManager.trace(
                context.agentState,
                mapOf("event" to "final", "preview" to summary.content.take(200)),
                "🧾 FINAL"
            )
            persistTranscript(instruction, summary, context)
            summary
        } catch (e: Exception) {
            logger.error("📝❌ agent.summary.fail sid={} msg={}", context.sid, e.message, e)
            ModelResponse("Failed to generate summary: ${e.message}", ResponseState.OTHER)
        }
    }

    protected fun persistTranscript(instruction: String, finalResp: ModelResponse, context: ExecutionContext) {
        runCatching {
            val ts = Instant.now().toEpochMilli()
            val log = baseDir.resolve("session-${uuid}-${ts}.log")
            slogger.info("🧾💾 Persisting execution transcript", context, mapOf("path" to log))
            val sb = StringBuilder()
            sb.appendLine("SESSION_ID: ${uuid}")
            sb.appendLine("TIMESTAMP: ${Instant.now()}")
            sb.appendLine("INSTRUCTION: $instruction")
            sb.appendLine("RESPONSE_STATE: ${finalResp.state}")
            sb.appendLine("EXECUTION_HISTORY:")
            stateHistory.forEach { sb.appendLine(it) }
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
            Files.writeString(log, sb)
            slogger.info(
                "🧾✅ Transcript persisted successfully",
                context,
                mapOf("lines" to stateHistory.size + 10, "path" to log)
            )
        }.onFailure { e -> slogger.logError("🧾❌ Failed to persist transcript", e, context.sessionId) }
    }

    private suspend fun handleConsecutiveNoOps(consecutiveNoOps: Int, context: ExecutionContext): Boolean {
        val step = context.step
        stateManager.trace(
            context.agentState,
            mapOf("event" to "noop", "step" to step, "consecutive" to consecutiveNoOps),
            "🕒 no-op"
        )
        logger.info("🕒 noop sid={} step={} consecutive={}", context.sid, step, consecutiveNoOps)
        if (consecutiveNoOps >= config.consecutiveNoOpLimit) {
            logger.info("⛔ noop.stop sid={} step={} limit={}", context.sid, step, config.consecutiveNoOpLimit)
            return true
        }
        if (isClosed) return true
        val delayMs = calculateConsecutiveNoOpDelay(consecutiveNoOps)
        delay(delayMs)
        val job = currentCoroutineContext()[Job]
        if (job == null || !job.isActive) {
            logger.info("🕒 noop cancelled sid={} step={}", context.sid, step)
            return true
        }
        return false
    }

    private fun calculateConsecutiveNoOpDelay(consecutiveNoOps: Int): Long {
        val baseDelay = 250L
        val exponentialDelay = baseDelay * consecutiveNoOps
        return min(exponentialDelay, 5000L)
    }

    private fun updatePerformanceMetrics(step: Int, stepStartTime: Instant, success: Boolean) {
        val stepTime = Duration.between(stepStartTime, Instant.now()).toMillis()
        stepExecutionTimes[step] = stepTime
        performanceMetrics.totalSteps += 1
        if (success) performanceMetrics.successfulActions += 1 else performanceMetrics.failedActions += 1
    }

    private fun calculateAdaptiveDelay(): Long {
        if (!config.enableAdaptiveDelays) return 100L
        val avgStepTime = stepExecutionTimes.values.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        return when {
            avgStepTime < 500 -> 50L
            avgStepTime < 2000 -> 100L
            else -> 200L
        }
    }

    private fun saveCheckpoint(context: ExecutionContext) {
        if (!config.enableCheckpointing) return
        val checkpoint = AgentCheckpoint(
            sessionId = context.sessionId,
            currentStep = context.step,
            instruction = context.instruction,
            targetUrl = context.targetUrl,
            recentStateHistory = stateHistory.takeLast(20).map { AgentStateSnapshot.from(it) },
            totalSteps = performanceMetrics.totalSteps,
            successfulActions = performanceMetrics.successfulActions,
            failedActions = performanceMetrics.failedActions,
            failureCounts = circuitBreaker.getFailureCounts().mapKeys { it.key.name },
            configSnapshot = mapOf(
                "maxSteps" to config.maxSteps,
                "maxRetries" to config.maxRetries,
                "consecutiveNoOpLimit" to config.consecutiveNoOpLimit
            ),
            metadata = mapOf(
                "agentUuid" to uuid,
                "startTime" to startTime
            )
        )
        val path = checkpointManager.save(checkpoint)
        logger.info("💾 checkpoint.saved sid={} step={} path={}", context.sid, context.step, path)
        checkpointManager.pruneOldCheckpoints(context.sessionId, config.maxCheckpointsPerSession)
    }

    protected fun shouldTerminate(actionDescription: ActionDescription? = null): Boolean {
        return when {
            actionDescription == null -> false
            actionDescription.isComplete -> true
            actionDescription.expression?.contains("agent.done") == true -> true
            else -> false
        }
    }

    protected suspend fun onTaskCompletion(action: ActionDescription, context: ExecutionContext) {
        val step = context.step
        val sid = context.sessionId

        logger.info("✅ task.complete sid={} step={} complete={}", sid.take(8), step, action.isComplete)
        stateManager.trace(
            context.agentState,
            mapOf("event" to "complete", "step" to step, "taskComplete" to action.isComplete),
            "#${step} complete"
        )
        if (config.enableTodoWrites) {
            runCatching { todo.onTaskCompletion(context.instruction) }
                .onFailure { e -> slogger.logError("📝❌ todo.complete.fail", e, sid) }
        }
    }

    private suspend fun buildFinalActResult(
        instruction: String,
        context: ExecutionContext,
        startTime: Instant
    ): ActResult {
        val executionTime = Duration.between(startTime, Instant.now())

        if (closed.get()) {
            logger.info("""🛑 [USER interrupted] sid={} steps={} dur={}""", context.sid, context.step, executionTime)
            return ActResult(
                success = false,
                message = "USER interrupted",
                action = instruction
            )
        }

        logger.info("✅ agent.done sid={} steps={} dur={}", context.sid, context.step, executionTime)
        val summary = generateFinalSummary(instruction, context)
        val ok = summary.state != ResponseState.OTHER
        return ActResult(
            success = ok,
            message = summary.content,
            action = instruction,
            result = context.agentState.toolCallResult
        )
    }

    private fun handleResolutionFailure(
        e: Exception,
        context: ExecutionContext,
        startTime: Instant
    ): PerceptiveAgentError {
        val executionTime = Duration.between(startTime, Instant.now())
        logger.error(
            "💥 agent.fail sid={} steps={} dur={} err={}",
            context.sid,
            context.step,
            executionTime,
            e.message,
            e
        )
        runCatching { stateManager.removeLastIfStep(context.step) }
            .onFailure { re ->
                logger.warn(
                    "⚠️ rollback failed sid={} step={} msg={}",
                    context.sid,
                    context.step,
                    re.message
                )
            }
        return classifyError(e, context.step)
    }
    // ===== end helper methods =====
}
