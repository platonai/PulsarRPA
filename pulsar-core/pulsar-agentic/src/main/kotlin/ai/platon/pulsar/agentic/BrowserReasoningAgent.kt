package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.detail.*
import ai.platon.pulsar.agentic.ai.todo.ToDoManager
import ai.platon.pulsar.browser.driver.chrome.dom.util.DomDebug
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ChatModelException
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

/**
 * A reasoning agent that uses [observe -> act -> observe -> act -> ...] pattern to resolve browser use problems.
 * */
class BrowserReasoningAgent constructor(
    session: AgenticSession,
    maxSteps: Int = 100,
    config: AgentConfig = AgentConfig(maxSteps = maxSteps)
) : BrowserPerceptiveAgent(session, maxSteps, config) {
    private val logger = getLogger(this)
    private val slogger = StructuredAgentLogger(logger, config)

    private val todo: ToDoManager

    // Helper classes for better code organization
    private val pageStateTracker = PageStateTracker(session, config)
    private val stateManager by lazy { AgentStateManager(this) }
    // Enhanced state management

    private val performanceMetrics = PerformanceMetrics()
    private val retryCounter = AtomicInteger(0)
    private val consecutiveFailureCounter = AtomicInteger(0)
    private val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

    override val stateHistory: List<AgentState> get() = stateManager.stateHistory
    override val processTrace: List<ProcessTrace> get() = stateManager.processTrace

    init {
        todo = ToDoManager(toolExecutor.fs, config, uuid, slogger)
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
        val instruction = action.action
        val context = stateManager.buildInitExecutionContext(action)
        val sessionId = context.sessionId
        val sessionStartTime = context.timestamp

        // Add start history for better traceability (meta record only)
        val goal = Strings.compactLog(instruction, 160)
        stateManager.trace(
            "🚀 resolve START session=${sessionId.take(8)} goal='$goal' " +
                    "maxSteps=${config.maxSteps} maxRetries=${config.maxRetries}"
        )

        // Overall timeout to prevent indefinite hangs for a full resolve session
        return try {
            val result = withTimeout(config.resolveTimeoutMs) {
                resolveProblemWithRetry(action, context)
            }
            val dur = Duration.between(sessionStartTime, Instant.now()).toMillis()
            // Not a single-step action, keep it out of AgentState history
            stateManager.trace("✅ resolve DONE session=${sessionId.take(8)} success=${result.success} dur=${dur}ms")
            result
        } catch (_: TimeoutCancellationException) {
            val msg = "⏳ Resolve timed out after ${config.resolveTimeoutMs}ms: ${instruction}"
            stateManager.trace("⏳ resolve TIMEOUT: ${Strings.compactLog(instruction, 160)}")
            ActResult(success = false, message = msg, action = instruction)
        }
    }

    override fun toString(): String {
        return stateHistory.lastOrNull()?.toString() ?: "(no history)"
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

        for (attempt in 0..config.maxRetries) {
            val attemptNo = attempt + 1

            stateManager.trace("🔁 resolve ATTEMPT ${attemptNo}/${config.maxRetries + 1}")

            try {
                val res = doResolveProblem(action, context, attempt)

                stateManager.trace("✅ resolve ATTEMPT $attemptNo OK")

                return res
            } catch (e: PerceptiveAgentError.TransientError) {
                lastError = e
                logger.error("🔄 resolve.transient attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.trace("🔁 resolve RETRY $attemptNo cause=Transient delay=${backoffMs}ms msg=${e.message}")
                    delay(backoffMs)
                }
            } catch (e: PerceptiveAgentError.TimeoutError) {
                lastError = e
                logger.error("⏳ resolve.timeout attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val baseBackoffMs = config.baseRetryDelayMs
                    stateManager.trace("🔁 resolve RETRY $attemptNo cause=Timeout delay=${baseBackoffMs}ms msg=${e.message}")
                    delay(baseBackoffMs)
                }
            } catch (e: Exception) {
                lastError = e
                logger.error("💥 resolve.unexpected attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (shouldRetryError(e) && attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.trace("🔁 resolve RETRY $attemptNo cause=Unexpected delay=${backoffMs}ms msg=${e.message}")
                    delay(backoffMs)
                } else {
                    // Non-retryable error, exit loop
                    break
                }
            }
        }

        stateManager.trace("❌ resolve FAIL after ${config.maxRetries + 1} attempts: ${lastError?.message}")
        return ActResult(
            success = false,
            message = "Failed after ${config.maxRetries + 1} attempts. Last error: ${lastError?.message}",
            action = action.action
        )
    }

    /**
     * Main execution logic with enhanced error handling and monitoring.
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun doResolveProblem(
        action: ActionOptions,
        initContext: ExecutionContext,
        attempt: Int
    ): ActResult {
        val sid = initContext.sid
        logger.info(
            "🚀 agent.start sid={} step={} url={} instr='{}' attempt={} maxSteps={} maxRetries={}",
            sid, initContext.step, initContext.targetUrl, Strings.compactLog(initContext.instruction, 100),
            attempt + 1, config.maxSteps, config.maxRetries
        )

        // Initialize todolist.md if enabled and empty
        if (config.enableTodoWrites) {
            try {
                todo.primeIfEmpty(initContext.instruction, initContext.targetUrl)
            } catch (e: Exception) {
                slogger.logError("📝❌ todo.prime.fail", e, sid)
            }
        }

        // agent general guide
        var consecutiveNoOps = 0
        var step = 0

        var context = initContext

        try {
            loop@ while (step < config.maxSteps) {
                step++

                // Step setup: ensure URL and settle DOM
                ensureReadyForStep(action)

                // Build AgentState and snapshot after settling
                require(step == context.step + 1) { "Step should be exactly (context.stepNumber + 1)" }

                context = stateManager.buildExecutionContext(action.action, "step", step, baseContext = context)
                val agentState = context.agentState
                val browserUseState = agentState.browserUseState

                // Detect unchanged state for heuristics
                val unchangedCount = pageStateTracker.checkStateChange(browserUseState)
                if (unchangedCount >= 3) {
                    logger.info("⚠️ loop.warn sid={} step={} unchangedSteps={}", sid, step, unchangedCount)
                    consecutiveNoOps++
                }

                logger.info("▶️ step.exec sid={} step={}/{} noOps={}", sid, step, config.maxSteps, consecutiveNoOps)
                if (logger.isDebugEnabled) {
                    logger.debug("🧩 dom={}", DomDebug.summarizeStr(browserUseState.domState, 5))
                }

                // Memory cleanup at intervals
                if (step % config.memoryCleanupIntervalSteps == 0) {
                    performMemoryCleanup(context)
                }

                //**
                // I - Observe and Generate Action
                //**

                // Generate the action for this step
                val options = ObserveOptions(instruction = context.instruction)
                val observeResults = observe(options)

                observeResults.forEach { result ->
                    val actionDescription = result.actionDescription

                    // Check for task completion
                    if (shouldTerminate(actionDescription)) {
                        onTaskCompletion(actionDescription!!, context)
                        break@loop
                    }

                    if (actionDescription?.toolCall == null) {
                        consecutiveNoOps++
                        val stop = handleConsecutiveNoOps(consecutiveNoOps, step, context)
                        if (stop) break@loop
                        continue@loop
                    }

                    // Reset consecutive no-ops counter when we have a valid action
                    consecutiveNoOps = 0

                    //**
                    // II - Execute Tool Call
                    //**

                    // Execute the tool call with enhanced error handling
                    val actOptions = ActionOptions(action = context.instruction)
                    val actResult = act(actOptions)

                    if (actResult.success) {
                        // updateAgentState(context.agentState, actResult)

                        updateTodo(context, actResult)

                        updatePerformanceMetrics(step, context.timestamp, true)

                        logger.info("🏁 step.done sid={} step={} result={}", sid, step, actResult.result)
                    } else {
                        // Treat validation failures or execution skips as no-ops; no AgentState record
                        consecutiveNoOps++
                        val stop = handleConsecutiveNoOps(consecutiveNoOps, step, context)
                        if (stop) break@loop
                        updatePerformanceMetrics(step, context.timestamp, false)
                    }
                }

                // Adaptive delay based on performance metrics
                delay(calculateAdaptiveDelay())
            }

            val summary = generateFinalSummary(context.instruction, context)
            val ok = summary.state != ResponseState.OTHER
            return ActResult(
                success = ok,
                message = summary.content,
                action = context.instruction,
                result = context.agentState.toolCallResult
            )
        } catch (e: Exception) {
            val executionTime = Duration.between(startTime, Instant.now())
            logger.error("💥 agent.fail sid={} steps={} dur={} err={}", sid, step, executionTime, e.message, e)
            throw classifyError(e, step)
        }
    }

    private suspend fun updateTodo(context: ExecutionContext, actResult: ActResult) {
        val sid = context.sessionId
        val step = context.step
    }

    /**
     * Classifies errors for appropriate retry strategies
     */
    private fun classifyError(e: Exception, step: Int): PerceptiveAgentError {
        return when (e) {
            is PerceptiveAgentError -> e
            is TimeoutException -> PerceptiveAgentError.TimeoutError("⏳ Step $step timed out", e)
            is SocketTimeoutException -> PerceptiveAgentError.TimeoutError("⏳ Network timeout at step $step", e)
            is ConnectException -> PerceptiveAgentError.TransientError("🔄 Connection failed at step $step", e)
            is ChatModelException -> PerceptiveAgentError.TimeoutError("⏳ Chat model timeout at step $step", e)
            is UnknownHostException -> PerceptiveAgentError.TransientError(
                "🔄 DNS resolution failed at step $step", e
            )

            is IOException -> {
                when {
                    e.message?.contains("connection") == true -> PerceptiveAgentError.TransientError(
                        "🔄 Connection issue at step $step", e
                    )

                    e.message?.contains("timeout") == true -> PerceptiveAgentError.TimeoutError(
                        "⏳ Network timeout at step $step", e
                    )

                    else -> PerceptiveAgentError.TransientError("🔄 IO error at step $step: ${e.message}", e)
                }
            }

            is IllegalArgumentException -> PerceptiveAgentError.ValidationError(
                "🚫 Validation error at step $step: ${e.message}", e
            )

            is IllegalStateException -> PerceptiveAgentError.PermanentError(
                "🛑 Invalid state at step $step: ${e.message}", e
            )

            else -> PerceptiveAgentError.TransientError("💥 Unexpected error at step $step: ${e.message}", e)
        }
    }

    /**
     * Determines if an error should trigger a retry
     */
    private fun shouldRetryError(e: Exception): Boolean {
        return when (e) {
            is PerceptiveAgentError.TransientError, is PerceptiveAgentError.TimeoutError -> true
            is SocketTimeoutException, is ConnectException, is UnknownHostException -> true

            else -> false
        }
    }

    /**
     * Calculates retry delay with exponential backoff and jitter
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        // Use multiplicative jitter so delay is monotonic w.r.t attempt
        val baseExp = config.baseRetryDelayMs * (2.0.pow(attempt.toDouble()))
        val jitterPercent = (0..30).random() / 100.0 // 0%..30% multiplicative jitter
        val withJitter = baseExp * (1.0 + jitterPercent)
        return min(withJitter.toLong(), config.maxRetryDelayMs)
    }

    private fun calculateConsecutiveNoOpDelay(consecutiveNoOps: Int): Long {
        val baseDelay = 250L
        val exponentialDelay = baseDelay * consecutiveNoOps
        return min(exponentialDelay, 5000L)
    }

    private fun performMemoryCleanup(context: ExecutionContext) {
        try {
            if (stateHistory.size > config.maxHistorySize) {
                val toRemove = stateHistory.size - config.maxHistorySize + 10
                stateManager.clearUpHistory(toRemove)
            }
            logger.info("🧹 mem.cleanup sid={} step={}", context.sid, context.step)
        } catch (e: Exception) {
            logger.error("🧹❌ mem.cleanup.fail sid={} msg={}", context.sid, e.message, e)
        }
    }

    private suspend fun handleConsecutiveNoOps(consecutiveNoOps: Int, step: Int, context: ExecutionContext): Boolean {
        require(step == context.step) { "Step should be consistent with context.stepNumber" }
        stateManager.trace("🕒 #$step no-op (consecutive: $consecutiveNoOps)")
        logger.info("🕒 noop sid={} step={} consecutive={}", context.sid, step, consecutiveNoOps)
        if (consecutiveNoOps >= config.consecutiveNoOpLimit) {
            logger.info("⛔ noop.stop sid={} step={} limit={}", context.sid, step, config.consecutiveNoOpLimit)
            return true
        }
        val delayMs = calculateConsecutiveNoOpDelay(consecutiveNoOps)
        delay(delayMs)
        return false
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
}
