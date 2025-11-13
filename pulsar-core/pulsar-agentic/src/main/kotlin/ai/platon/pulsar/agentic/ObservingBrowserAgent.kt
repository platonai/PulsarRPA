package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.agentic.ai.agent.detail.PerceptiveAgentError
import ai.platon.pulsar.agentic.ai.agent.detail.StructuredAgentLogger
import ai.platon.pulsar.browser.driver.chrome.dom.util.DomDebug
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.ActResult
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ProcessTrace
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import kotlin.math.min
import kotlin.math.pow

/**
 * A reasoning agent that uses [observe -> act -> observe -> act -> ...] pattern to resolve browser use problems.
 * */
class ObservingBrowserAgent constructor(
    session: AgenticSession,
    maxSteps: Int = 100,
    config: AgentConfig = AgentConfig(maxSteps = maxSteps)
) : BrowserPerceptiveAgent(session, maxSteps, config) {
    private val logger = getLogger(this)
    private val slogger = StructuredAgentLogger(logger, config)

    override val stateHistory: List<AgentState> get() = stateManager.stateHistory
    override val processTrace: List<ProcessTrace> get() = stateManager.processTrace

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
            context.agentState,
            mapOf(
                "event" to "resolveStart",
                "session" to sessionId.take(8),
                "goal" to goal,
                "maxSteps" to config.maxSteps.toString(),
                "maxRetries" to config.maxRetries.toString()
            ),
            "🚀 resolve START"
        )

        // Overall timeout to prevent indefinite hangs for a full resolve session
        return try {
            val result = withTimeout(config.resolveTimeoutMs) {
                resolveProblemWithRetry(action, context)
            }
            val dur = Duration.between(sessionStartTime, Instant.now()).toMillis()
            // Not a single-step action, keep it out of AgentState history
            stateManager.trace(
                context.agentState,
                mapOf(
                    "event" to "resolveDone",
                    "session" to sessionId.take(8),
                    "success" to result.success.toString(),
                    "durationMs" to dur.toString()
                ),
                "✅ resolve DONE"
            )
            result
        } catch (_: TimeoutCancellationException) {
            val compact = Strings.compactLog(instruction, 160)
            val msg = "⏳ Resolve timed out after ${config.resolveTimeoutMs}ms: ${instruction}"
            stateManager.trace(
                context.agentState,
                mapOf(
                    "event" to "resolveTimeout",
                    "timeoutMs" to config.resolveTimeoutMs.toString(),
                    "instruction" to compact
                ),
                "⏳ resolve TIMEOUT"
            )
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

            stateManager.trace(
                context.agentState,
                mapOf(
                    "event" to "resolveAttempt",
                    "attemptNo" to attemptNo.toString(),
                    "attemptsTotal" to (config.maxRetries + 1).toString()
                ),
                "🔁 resolve ATTEMPT"
            )

            try {
                val res = doResolveProblem(action, context, attempt)

                stateManager.trace(
                    context.agentState,
                    mapOf(
                        "event" to "resolveAttemptOk",
                        "attemptNo" to attemptNo.toString()
                    ),
                    "✅ resolve ATTEMPT OK"
                )

                return res
            } catch (e: PerceptiveAgentError.TransientError) {
                lastError = e
                logger.error("🔄 resolve.transient attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.trace(
                        context.agentState,
                        mapOf(
                            "event" to "resolveRetry",
                            "cause" to "Transient",
                            "attemptNo" to attemptNo.toString(),
                            "delayMs" to backoffMs.toString(),
                            "msg" to (e.message ?: "")
                        ),
                        "🔁 resolve RETRY"
                    )
                    delay(backoffMs)
                }
            } catch (e: PerceptiveAgentError.TimeoutError) {
                lastError = e
                logger.error("⏳ resolve.timeout attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (attempt < config.maxRetries) {
                    val baseBackoffMs = config.baseRetryDelayMs
                    stateManager.trace(
                        context.agentState,
                        mapOf(
                            "event" to "resolveRetry",
                            "cause" to "Timeout",
                            "attemptNo" to attemptNo.toString(),
                            "delayMs" to baseBackoffMs.toString(),
                            "msg" to (e.message ?: "")
                        ),
                        "🔁 resolve RETRY"
                    )
                    delay(baseBackoffMs)
                }
            } catch (e: Exception) {
                lastError = e
                logger.error("💥 resolve.unexpected attempt={} sid={} msg={}", attempt + 1, sid, e.message, e)

                if (shouldRetryError(e) && attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    stateManager.trace(
                        context.agentState,
                        mapOf(
                            "event" to "resolveRetry",
                            "cause" to "Unexpected",
                            "attemptNo" to attemptNo.toString(),
                            "delayMs" to backoffMs.toString(),
                            "msg" to (e.message ?: "")
                        ),
                        "🔁 resolve RETRY"
                    )
                    delay(backoffMs)
                } else {
                    // Non-retryable error, exit loop
                    break
                }
            }
        }

        stateManager.trace(
            context.agentState,
            mapOf(
                "event" to "resolveFail",
                "attemptsTotal" to (config.maxRetries + 1).toString(),
                "msg" to (lastError?.message ?: "")
            ),
            "❌ resolve FAIL"
        )
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

        var context = initContext

        try {
            loop@ while (context.step < config.maxSteps) {
                // Step setup: ensure URL and settle DOM
                ensureReadyForStep(action)

                context = stateManager.buildExecutionContext(action.action, "step", context.step, baseContext = context)

                // Detect unchanged state for heuristics
                val unchangedCount = pageStateTracker.checkStateChange(context.agentState.browserUseState)
                if (unchangedCount >= 3) {
                    logger.info("⚠️ loop.warn sid={} step={} unchangedSteps={}", sid, context.step, unchangedCount)
                    consecutiveNoOps++
                }

                logger.info(
                    "▶️ step.exec sid={} step={}/{} noOps={}",
                    sid,
                    context.step,
                    config.maxSteps,
                    consecutiveNoOps
                )
                if (logger.isDebugEnabled) {
                    logger.debug("🧩 dom={}", DomDebug.summarizeStr(context.agentState.browserUseState.domState, 5))
                }

                // Memory cleanup at intervals
                if (context.step % config.memoryCleanupIntervalSteps == 0) {
                    performMemoryCleanup(context)
                }

                //**
                // I - Observe and Generate Action
                //**

                // Generate the action for this step
                val observeResults = observe(context.createObserveActOptions())

                observeResults.forEach { result ->
                    val actionDescription = result.actionDescription

                    // Check for task completion
                    if (shouldTerminate(actionDescription)) {
                        onTaskCompletion(actionDescription!!, context)
                        break@loop
                    }

                    if (actionDescription?.toolCall == null) {
                        consecutiveNoOps++
                        val stop = handleConsecutiveNoOps(consecutiveNoOps, context.step, context)
                        if (stop) break@loop
                        continue@loop
                    }

                    // Reset consecutive no-ops counter when we have a valid action
                    consecutiveNoOps = 0

                    //**
                    // II - Execute Tool Call
                    //**

                    // Execute the tool call with enhanced error handling
                    val actResult = act(result)

                    if (actResult.success) {
                        // updateAgentState(context.agentState, actResult)
                        stateManager.updateAgentState(context.agentState, actResult.detail!!)

                        updateTodo(context, actResult)

                        updatePerformanceMetrics(context.step, context.timestamp, true)

                        logger.info("🏁 step.done sid={} step={} result={}", sid, context.step, actResult.result)
                    } else {
                        // Treat validation failures or execution skips as no-ops; no AgentState record
                        consecutiveNoOps++
                        val stop = handleConsecutiveNoOps(consecutiveNoOps, context.step, context)
                        if (stop) break@loop
                        updatePerformanceMetrics(context.step, context.timestamp, false)
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
            logger.error("💥 agent.fail sid={} steps={} dur={} err={}", sid, context.step, executionTime, e.message, e)
            throw retryStrategy.classifyError(e, "step ${context.step}")
        }
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
        stateManager.trace(
            context.agentState,
            mapOf(
                "event" to "noop",
                "step" to step.toString(),
                "consecutive" to consecutiveNoOps.toString()
            ),
            "🕒 no-op"
        )
        logger.info("🕒 noop sid={} step={} consecutive={}", context.sid, step, consecutiveNoOps)
        if (consecutiveNoOps >= config.consecutiveNoOpLimit) {
            logger.info("⛔ noop.stop sid={} step={} limit={}", context.sid, step, config.consecutiveNoOpLimit)
            return true
        }
        val delayMs = calculateConsecutiveNoOpDelay(consecutiveNoOps)
        delay(delayMs)
        return false
    }
}
