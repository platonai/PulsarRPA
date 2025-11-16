package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.detail.*
import ai.platon.pulsar.agentic.ai.todo.ToDoManager
import ai.platon.pulsar.agentic.tools.ActionValidator
import ai.platon.pulsar.browser.driver.chrome.dom.util.DomDebug
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.*
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
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
    val logInferenceToFile: Boolean = true,
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

    protected val closed = AtomicBoolean(false)
    val isClosed: Boolean get() = closed.get()

    // A dedicated scope for all agent work so close() can cancel promptly
    protected val agentJob = SupervisorJob()
    protected val agentScope = CoroutineScope(Dispatchers.Default + agentJob)

    protected val todo: ToDoManager by lazy { ToDoManager(toolExecutor.fs, config, uuid, slogger) }
    protected val actionValidator = ActionValidator()

    protected val performanceMetrics = PerformanceMetrics()
    protected val consecutiveFailureCounter = AtomicInteger(0)
    protected val consecutiveLLMFailureCounter = AtomicInteger(0)
    protected val consecutiveValidationFailureCounter = AtomicInteger(0)
    protected val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

    // New components for better separation of concerns
    protected val circuitBreaker = CircuitBreaker(
        maxLLMFailures = config.maxConsecutiveLLMFailures,
        maxValidationFailures = config.maxConsecutiveValidationFailures,
        maxExecutionFailures = 3
    )
    protected val retryStrategy = RetryStrategy(
        maxRetries = config.maxRetries,
        baseDelayMs = config.baseRetryDelayMs,
        maxDelayMs = config.maxRetryDelayMs
    )
    protected val retryCounter = AtomicInteger(0)
    protected val checkpointManager = CheckpointManager(baseDir.resolve("checkpoints"))

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
            val result = withContext(agentScope.coroutineContext) { resolveInCoroutine(action) }
            result.result
        } catch (_: CancellationException) {
            ActResult(false, "USER interrupted", action = action.action)
        } finally {
            stateManager.writeProcessTrace()
        }
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
            }
        } catch (_: CancellationException) {
            ActResult(false, "USER interrupted", action = action.action)
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
            }
        } catch (_: CancellationException) {
            ActResult(false, "USER interrupted", action = observe.agentState.instruction)
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
            }
        } catch (_: CancellationException) {
            ExtractResult(success = false, message = "USER interrupted", data = JsonNodeFactory.instance.objectNode())
        }
    }

    /**
     * Observes the page given an instruction, returning zero or more ObserveResult objects describing
     * candidate elements and potential actions (if returnAction=true).
     */
    override suspend fun observe(options: ObserveOptions): List<ObserveResult> {
        if (isClosed) return emptyList()

        val context = options.getContext() ?: stateManager.buildInitExecutionContext(options, "observe")
        options.setContext(context)

        if (!options.resolve) {
            return withContext(agentScope.coroutineContext) {
                if (isClosed) throw CancellationException("closed")
                super.observe(options)
            }
        }

        try {
            val results = withContext(agentScope.coroutineContext) {
                if (isClosed) throw CancellationException("closed")
                super.observe(options)
            }

            circuitBreaker.recordSuccess(CircuitBreaker.FailureType.LLM_FAILURE)
            consecutiveLLMFailureCounter.set(0) // Keep for backward compatibility

            return results
        } catch (e: Exception) {
            handleObserveException(e, context)
            return emptyList()
        }
    }

    fun stop() = close()

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { agentJob.cancel(CancellationException("USER interrupted via close()")) }
            // Best-effort trace for visibility; avoid throwing
            runCatching {
                val last = stateHistory.lastOrNull()
                stateManager.addTrace(last, emptyMap(), event = "userClose", message = "🛑 USER CLOSE")
            }
        }
    }

    /**
     * Returns a concise summary of the latest agent state; if no history exists, returns a placeholder text.
     */
    override fun toString(): String {
        return stateHistory.lastOrNull()?.toString() ?: "(no history)"
    }

    protected data class ResolveResult(
        val context: ExecutionContext,
        val result: ActResult
    )

    private suspend fun resolveInCoroutine(action: ActionOptions): ResolveResult {
        val instruction = action.action
        val baseContext = stateManager.buildBaseExecutionContext(action, "resolve-init")
        val sessionStartTime = baseContext.timestamp

        // Add start history for better traceability (meta record only)
        stateManager.addTrace(
            baseContext.agentState,
            mapOf(
                "session" to baseContext.sid,
                "goal" to Strings.compactInline(instruction, 160),
                "maxSteps" to config.maxSteps,
                "maxRetries" to config.maxRetries
            ),
            event = "resolveStart",
            message = "🚀 resolve START"
        )

        // Overall timeout to prevent indefinite hangs for a full resolve session
        // Calculate effective timeout accounting for potential retry delays
        val maxPossibleDelays = (0 until config.maxRetries).fold(0L) { acc, i -> acc + calculateRetryDelay(i) }
        val effectiveTimeout = config.resolveTimeoutMs + maxPossibleDelays

        return try {
            val result = withTimeout(effectiveTimeout) {
                resolveProblemWithRetry(action, baseContext)
            }

            val dur = Duration.between(sessionStartTime, Instant.now()).toMillis()
            // Not a single-step action, keep it out of AgentState history
            stateManager.addTrace(
                result.context.agentState, mapOf(
                    "session" to baseContext.sid,
                    "success" to result.result.success, "durationMs" to dur
                ), event = "resolveDone", message = "✅ resolve DONE"
            )

            result
        } catch (_: TimeoutCancellationException) {
            val msg = "⏳ Resolve timed out after ${effectiveTimeout}ms (base: ${config.resolveTimeoutMs}ms + " +
                    "retries: ${maxPossibleDelays}ms): $instruction"
            stateManager.addTrace(
                baseContext.agentState, mapOf(
                    "timeoutMs" to effectiveTimeout, "instruction" to Strings.compactInline(instruction, 160)
                ),
                event = "resolveTimeout",
                message = "⏳ resolve TIMEOUT"
            )
            val actResult = ActResult(success = false, message = msg, action = instruction)
            ResolveResult(baseContext, actResult)
        } finally {
            // clear history so the next task will have a clean operation trace for summary.
            // but we do not clear process trace which will be kept to trace all operations and states.
            stateManager.clearHistory()
        }
    }

    protected suspend fun generateActions(context: ExecutionContext): ActionDescription {
        context.screenshotB64 = if (context.step % config.screenshotEveryNSteps == 0) {
            if (isClosed) return ActionDescription(
                context.instruction,
                context = context,
                exception = CancellationException("closed")
            )
            captureScreenshotWithRetry(context)
        } else null

        // Prepare messages for model
        val messages = promptBuilder.buildResolveMessageListAll(context)

        return try {
            if (isClosed) {
                throw CancellationException("closed")
            }

            val actionDescription = cta.generate(messages, context)
            requireNotNull(context.agentState.actionDescription) { "Filed should be set: context.agentState.actionDescription" }
            circuitBreaker.recordSuccess(CircuitBreaker.FailureType.LLM_FAILURE)
            consecutiveLLMFailureCounter.set(0) // Keep for backward compatibility

            actionDescription
        } catch (e: Exception) {
            handleObserveException(e, context)

            ActionDescription(
                context.instruction,
                context = context,
                exception = e,
                modelResponse = ModelResponse.INTERNAL_ERROR
            )
        }
    }

    private fun handleObserveException(e: Exception, context: ExecutionContext) {
        val failures = try {
            circuitBreaker.recordFailure(CircuitBreaker.FailureType.LLM_FAILURE)
        } catch (cbError: CircuitBreakerTrippedException) {
            throw PerceptiveAgentError.PermanentError(cbError.message ?: "Circuit breaker tripped", cbError)
        }

        consecutiveLLMFailureCounter.set(failures) // Keep for backward compatibility
        logger.error("🤖❌ action.gen.fail sid={} failures={} msg={}", context.sid, failures, e.message, e)
        consecutiveFailureCounter.incrementAndGet()
    }

    protected suspend fun prepareStep(
        action: ActionOptions,
        ctxIn: ExecutionContext,
        noOpsIn: Int
    ): ExecutionContext {
        val context = ensureReadyForStep(action, "step", ctxIn)
        requireNotNull(action.getContext()) { "Filed should be set: action.context" }

        val agentState = context.agentState
        val browserUseState = agentState.browserUseState
        val step = context.step
        val sid = context.sid

        var consecutiveNoOps = noOpsIn
        val unchangedCount = pageStateTracker.checkStateChange(browserUseState)
        if (unchangedCount >= 3) {
            logger.info("⚠️ loop.warn sid={} step={} unchangedSteps={}", sid, step, unchangedCount)
            consecutiveNoOps++
        }

        logger.info("▶️ step.exec sid={} step={}/{} noOps={}", sid, step, config.maxSteps, consecutiveNoOps)
        if (logger.isDebugEnabled) {
            logger.debug("🧩 dom={}", DomDebug.summarizeStr(browserUseState.domState, 5))
        }
        if (step % config.memoryCleanupIntervalSteps == 0) {
            performMemoryCleanup(context)
        }
        if (config.enableCheckpointing && step % config.checkpointIntervalSteps == 0) {
            runCatching { saveCheckpoint(context) }
                .onFailure { e -> logger.warn("💾❌ checkpoint.save.fail sid={} step={} msg={}", sid, step, e.message) }
        }

        return context
    }

    protected suspend fun ensureReadyForStep(
        action: ActionOptions, event: String, ctxIn: ExecutionContext
    ): ExecutionContext {
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

        val instruction = action.action
        val step = ctxIn.step + 1
        val context = stateManager.buildExecutionContext(instruction, step, event, baseContext = ctxIn)
        action.setContext(context)

        return context
    }

    private suspend fun doResolveProblem(
        initActionOptions: ActionOptions,
        initContext: ExecutionContext,
        attempt: Int
    ): ResolveResult {
        initializeResolution(initContext, attempt)
        var consecutiveNoOps = 0
        var context = initContext
        val startTime = Instant.now()
        try {
            val action = initActionOptions.copy(resolve = true)
            while (!isClosed && context.step < config.maxSteps) {
                try {
                    val stepResult = step(action, context, consecutiveNoOps)
                    context = stepResult.context
                    consecutiveNoOps = stepResult.consecutiveNoOps
                    if (stepResult.shouldStop) break
                } finally {
                    stateManager.addToHistory(context.agentState)
                }
            }

            val result = buildFinalActResult(initContext.instruction, context, startTime)

            return ResolveResult(context, result)
        } catch (_: CancellationException) {
            logger.info("""🛑 [USER interrupted] sid={} steps={}""", context.sid, context.step)
            val result = ActResult(success = false, message = "USER interrupted", action = initContext.instruction)
            return ResolveResult(context, result)
        } catch (e: Exception) {
            throw handleResolutionFailure(e, context, startTime)
        }
    }

    /**
     * Enhanced execution with comprehensive error handling and retry mechanisms
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun resolveProblemWithRetry(
        action: ActionOptions, context: ExecutionContext
    ): ResolveResult {
        var lastError: Exception? = null
        val sid = context.sid
        var currentContext = context

        for (attempt in 0..config.maxRetries) {
            val attemptNo = attempt + 1

            stateManager.addTrace(
                currentContext.agentState,
                mapOf(
                    "attemptNo" to attemptNo
                ),
                event = "resolveAttempt",
                message = "🔁 resolve ATTEMPT"
            )

            try {
                val result = doResolveProblem(action, currentContext, attempt)
                currentContext = result.context

                stateManager.addTrace(
                    currentContext.agentState,
                    mapOf("attemptNo" to attemptNo),
                    event = "resolveAttemptOk",
                    message = "✅ resolve ATTEMPT OK"
                )

                return result
            } catch (e: PerceptiveAgentError.TransientError) {
                lastError = e
                logger.error("🔄 resolve.transient attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.addTrace(
                        currentContext.agentState,
                        mapOf(
                            "cause" to "Transient",
                            "attemptNo" to attemptNo,
                            "delayMs" to backoffMs,
                            "msg" to (e.message ?: "")
                        ),
                        event = "resolveRetry",
                        message = "🔁 resolve RETRY"
                    )

                    // Clean up partial state before retry
                    try {
                        cleanupPartialState(currentContext)
                        // Rebuild context for next attempt to avoid corrupted state
                        currentContext = stateManager.buildBaseExecutionContext(action, "resolve-recovery")
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
                    stateManager.addTrace(
                        currentContext.agentState,
                        mapOf(
                            "cause" to "Timeout",
                            "attemptNo" to attemptNo,
                            "delayMs" to baseBackoffMs,
                            "msg" to (e.message ?: "")
                        ),
                        event = "resolveRetry",
                        message = "🔁 resolve RETRY"
                    )

                    // Clean up partial state before retry
                    try {
                        cleanupPartialState(currentContext)
                        currentContext = stateManager.buildBaseExecutionContext(action, "resolve-init-recovery")
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
                    stateManager.addTrace(
                        currentContext.agentState,
                        mapOf(
                            "cause" to "Unexpected",
                            "attemptNo" to attemptNo,
                            "delayMs" to backoffMs,
                            "msg" to (e.message ?: "")
                        ),
                        event = "resolveRetry",
                        message = "🔁 resolve RETRY"
                    )

                    // Clean up partial state before retry
                    try {
                        cleanupPartialState(currentContext)
                        currentContext = stateManager.buildBaseExecutionContext(action, "resolve-init-recovery")
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

        stateManager.addTrace(
            currentContext.agentState,
            mapOf(
                "attemptsTotal" to (config.maxRetries + 1), "msg" to lastError?.message
            ),
            event = "resolveFail",
            message = "❌ resolve FAIL"
        )

        val actResult = ActResult(
            success = false,
            message = "Failed after ${config.maxRetries + 1} attempts. Last error: ${lastError?.message}",
            action = action.action
        )

        return ResolveResult(currentContext, actResult)
    }

    protected suspend fun initializeResolution(initContext: ExecutionContext, attempt: Int) {
        val sid = initContext.sid
        logger.info(
            "🚀 agent.start sid={} step={} url={} instr='{}' attempt={} maxSteps={} maxRetries={}",
            sid, initContext.step, initContext.targetUrl, Strings.compactInline(initContext.instruction, 100),
            attempt + 1, config.maxSteps, config.maxRetries
        )
        if (config.enableTodoWrites) {
            runCatching { todo.primeIfEmpty(initContext.instruction, initContext.targetUrl) }
                .onFailure { e -> slogger.logError("📝❌ todo.prime.fail", e, sid) }
        }
    }

    data class StepProcessingResult(
        val context: ExecutionContext,
        val consecutiveNoOps: Int,
        val shouldStop: Boolean
    )

    protected open suspend fun step(
        action: ActionOptions,
        ctxIn: ExecutionContext,
        noOpsIn: Int
    ): StepProcessingResult {
        var consecutiveNoOps = noOpsIn
        val step = ctxIn.step

        val context = prepareStep(action, ctxIn, consecutiveNoOps)

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
        val detailedActResult = executeToolCall(actionDescription, context)

        if (detailedActResult != null) {
            stateManager.updateAgentState(context, detailedActResult)
            updateTodo(context, detailedActResult.actionDescription)
            updatePerformanceMetrics(step, context.timestamp, true)
            val preview = detailedActResult.toolCallResult?.evaluate?.preview
            logger.info("🏁 step.done sid={} step={} tcResult={}", context.sid, step, preview)
        } else {
            consecutiveNoOps++
            val stop = handleConsecutiveNoOps(consecutiveNoOps, context)
            updatePerformanceMetrics(step, context.timestamp, false)
            if (stop) return StepProcessingResult(context, consecutiveNoOps, true)
        }

        delay(calculateAdaptiveDelay())
        return StepProcessingResult(context, consecutiveNoOps, false)
    }

    protected fun classifyError(e: Exception, step: Int) = retryStrategy.classifyError(e, "step $step")

    protected fun shouldRetryError(e: Exception) = retryStrategy.shouldRetry(e)

    protected fun calculateRetryDelay(attempt: Int) = retryStrategy.calculateDelay(attempt)

    protected fun cleanupPartialState(context: ExecutionContext) {
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

    private suspend fun executeToolCall(
        actionDescription: ActionDescription,
        context: ExecutionContext
    ): DetailedActResult? {
        context.agentState.event = "toolExec"

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
            stateManager.addTrace(
                context.agentState,
                mapOf("step" to step, "tool" to toolCall.method),
                event = "validationFailed",
                message = "🛑 validation-failed"
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

            stateManager.addTrace(
                context.agentState,
                mapOf("tool" to toolCall.method),
                event = "toolExecOk",
                message = summary
            )
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
            stateManager.addTrace(
                context.agentState,
                mapOf("tool" to toolCall.method),
                event = "toolExecUnexpectedFail",
                message = "💥 unexpected failure"
            )
            null
        }
    }

    protected suspend fun updateTodo(context: ExecutionContext, actResult: ActResult) {
        val actionDescription = actResult.detail?.actionDescription
        requireNotNull(actionDescription) { "actionDescription should be set in actResult.additionalVariables" }
        updateTodo(context, actionDescription)
    }

    protected suspend fun updateTodo(context: ExecutionContext, actionDescription: ActionDescription) {
        if (!config.enableTodoWrites) return
        val sid = context.sessionId
        val step = context.step
        val toolCall = actionDescription.toolCall
        val observeElement = actionDescription.observeElement
        val shouldWrite = config.todoWriteProgressEveryStep ||
                (config.todoProgressWriteEveryNSteps > 1 && step % config.todoProgressWriteEveryNSteps == 0)
        if (!shouldWrite) return
        val urlNow0 = activeDriver.currentUrl()
        runCatching {
            todo.appendProgress(step, toolCall, observeElement, urlNow0, actionDescription.summary)
            todo.updateProgressCounter()
            if (config.todoEnableAutoCheck) {
                val tags = todo.buildTags(toolCall, urlNow0)
                if (tags.isNotEmpty()) todo.markPlanItemDoneByTags(tags)
            }
        }.onFailure { e -> slogger.logError("📝❌ todo.progress.fail", e, sid) }
    }

    protected fun performMemoryCleanup(context: ExecutionContext) {
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

    protected suspend fun summarize(goal: String, ctxIn: ExecutionContext): ModelResponse {
        val step = ctxIn.step + 1
        val context = stateManager.buildExecutionContext(goal, step, event = "summarize", baseContext = ctxIn)
        context.agentState.event = "summary"

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
            stateManager.addTrace(
                context.agentState,
                mapOf("summaryPreview" to summary.content.take(200)),
                event = "final",
                message = "🧾 FINAL"
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
            val log = baseDir.resolve("session-${ts}.log")
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

    protected suspend fun handleConsecutiveNoOps(consecutiveNoOps: Int, context: ExecutionContext): Boolean {
        val step = context.step
        stateManager.addTrace(
            context.agentState,
            mapOf("step" to step, "consecutive" to consecutiveNoOps),
            event = "noop",
            message = "🕒 no-op"
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

    protected fun calculateConsecutiveNoOpDelay(consecutiveNoOps: Int): Long {
        val baseDelay = 250L
        val exponentialDelay = baseDelay * consecutiveNoOps
        return min(exponentialDelay, 5000L)
    }

    protected fun updatePerformanceMetrics(step: Int, stepStartTime: Instant, success: Boolean) {
        val stepTime = Duration.between(stepStartTime, Instant.now()).toMillis()
        stepExecutionTimes[step] = stepTime
        performanceMetrics.totalSteps += 1
        if (success) performanceMetrics.successfulActions += 1 else performanceMetrics.failedActions += 1
    }

    protected fun calculateAdaptiveDelay(): Long {
        if (!config.enableAdaptiveDelays) return 100L
        val avgStepTime = stepExecutionTimes.values.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        return when {
            avgStepTime < 500 -> 50L
            avgStepTime < 2000 -> 100L
            else -> 200L
        }
    }

    protected fun saveCheckpoint(context: ExecutionContext) {
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

    protected fun shouldTerminate(actResult: ActResult? = null): Boolean {
        return shouldTerminate(actResult?.detail?.actionDescription)
    }

    protected fun shouldTerminate(actionDescription: ActionDescription? = null): Boolean {
        return when {
            actionDescription == null -> false
            actionDescription.isComplete -> true
            actionDescription.expression?.contains("agent.done") == true -> true
            else -> false
        }
    }

    protected suspend fun onTaskCompletion(actResult: ActResult, context: ExecutionContext) {
        val actionDescription = actResult.detail?.actionDescription ?: return
        onTaskCompletion(actionDescription, context)
    }

    protected suspend fun onTaskCompletion(action: ActionDescription, context: ExecutionContext) {
        val step = context.step
        val sid = context.sessionId

        require(action.isComplete) { "Required action.isComplete" }
        // require(context.agentState.isComplete) { "Required context.agentState.isComplete" }
        context.agentState.isComplete = true

        logger.info("✅ task.complete sid={} step={} complete={}", sid.take(8), step, action.isComplete)
        stateManager.addTrace(
            context.agentState,
            event = "complete",
            message = "#${step} complete"
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
        context.agentState.event = "summary"

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
        e: Exception, context: ExecutionContext, startTime: Instant
    ): PerceptiveAgentError {
        val executionTime = Duration.between(startTime, Instant.now())
        logger.error(
            "💥 agent.fail sid={} steps={} dur={} err={}",
            context.sid, context.step, executionTime, e.message, e
        )
        runCatching { stateManager.removeLastIfStep(context.step) }
            .onFailure { logger.warn("⚠️ rollback failed sid={} step={} msg={}", context.sid, context.step, e.message) }
        return classifyError(e, context.step)
    }
}
