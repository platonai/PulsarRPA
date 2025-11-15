package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.agent.InferenceEngine
import ai.platon.pulsar.agentic.ai.agent.detail.AgentStateManager
import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.agentic.ai.agent.detail.PageStateTracker
import ai.platon.pulsar.agentic.ai.agent.detail.getContext
import ai.platon.pulsar.agentic.ai.tta.ContextToAction
import ai.platon.pulsar.agentic.tools.AgentToolManager
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.NotSupportedException
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.slf4j.helpers.MessageFormatter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

open class BrowserAgentActor(
    val session: AgenticSession,
    val config: AgentConfig
) : PerceptiveAgent {
    private val logger = getLogger(BrowserAgentActor::class)
    private val _startTime: Instant = Instant.now()
    private val _uuid: UUID = UUID.randomUUID()
    private val _baseDir: Path = AppPaths.get("agent")
        .resolve(DateTimes.PATH_SAFE_FORMAT_101.format(_startTime))
        .resolve(_uuid.toString())

    protected val cta by lazy { ContextToAction(session.sessionConfig) }
    protected val inference by lazy { InferenceEngine(session, cta.chatModel) }
    protected val domService get() = inference.domService
    protected val promptBuilder = PromptBuilder()

    protected val toolExecutor by lazy { AgentToolManager(_baseDir, this) }

    // Helper classes for better code organization
    protected val pageStateTracker = PageStateTracker(session, config)
    protected val stateManager by lazy { AgentStateManager(this, domService, pageStateTracker) }

    override val uuid get() = _uuid
    override val stateHistory: List<AgentState> get() = stateManager.stateHistory
    override val processTrace: List<ProcessTrace> get() = stateManager.processTrace

    val activeDriver get() = session.getOrCreateBoundDriver()
    val startTime get() = _startTime
    val baseDir: Path get() = _baseDir

    init {
        Files.createDirectories(baseDir)
    }

    override suspend fun resolve(action: ActionOptions): ActResult {
        throw NotSupportedException("Not supported, use stateful agents instead, such as BrowserPerceptiveAgent, DelegatingPerceptiveAgent, etc.")
    }

    override suspend fun resolve(problem: String): ActResult {
        throw NotSupportedException("Not supported, use stateful agents instead, such as BrowserPerceptiveAgent, DelegatingPerceptiveAgent, etc.")
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
        val context = action.getContext() ?: stateManager.buildInitExecutionContext(action)

        return try {
            withTimeout(config.actTimeoutMs) {
                doObserveAct(action)
            }
        } catch (_: TimeoutCancellationException) {
            val msg = "⏳ Action timed out after ${config.actTimeoutMs}ms: ${action.action}"
            stateManager.addTrace(
                context.agentState,
                items = mapOf("event" to "actTimeout", "timeoutMs" to config.actTimeoutMs, "instruction" to action.action),
                message = "⏳ act TIMEOUT"
            )
            ActResult.failed(msg, action.action)
        }
    }

    override suspend fun act(observe: ObserveResult): ActResult {
        val instruction = observe.agentState.instruction
        val context = observe.getContext()
        val agentState = observe.agentState
        val observeElement = observe.observeElement
        context ?: return ActResult.failed("No context to act", instruction)
        observeElement ?: return ActResult.failed("No observation to act", instruction)
        val actionDescription =
            observe.actionDescription ?: return ActResult.failed("No action description to act", instruction)
        val toolCall = observeElement.toolCall ?: return ActResult.failed("No tool call to act", instruction)
        val method = toolCall.method

        return try {
            val detailedActResult = executeToolCall(actionDescription, context)

            val toolCallResult = detailedActResult.toolCallResult

            logger.info(
                "✅ Action executed | {} | {}/{} | {}",
                method, observe.locator, observeElement.cssSelector, observeElement.pseudoExpression
            )

            val msg = MessageFormatter.arrayFormat(
                "✅ Action executed | {} | {}/{} | {}",
                arrayOf(method, observe.locator, observeElement.cssSelector, observeElement.pseudoExpression)
            )

            stateManager.updateAgentState(agentState, observeElement, toolCall, toolCallResult, msg.message)

            // ActResult(success = true, action = toolCall.method, message = msg.message, result = toolCallResult, actionDescription = actionDescription)
            detailedActResult.toActResult()
        } catch (e: Exception) {
            logger.error("❌ observe.act execution failed sid={} msg={}", uuid.toString().take(8), e.message, e)

            val msg = MessageFormatter.arrayFormat(
                "❌ observe.act execution failed | {} | {}/{} | {}",
                arrayOf(method, observe.locator, observeElement.cssSelector, e.message)
            ).toString()

            stateManager.updateAgentState(agentState, observeElement, toolCall, null, msg, success = false)

            ActResult.failed(msg, toolCall.method)
        }
    }

    /**
     * Structured extraction: builds a rich prompt with DOM snapshot & optional JSON schema; performs
     * two-stage LLM calls (extract + metadata) and merges results with token/time metrics.
     */
    override suspend fun extract(options: ExtractOptions): ExtractResult {
        val instruction = promptBuilder.initExtractUserInstruction(options.instruction)
        val context = stateManager.buildExecutionContext(instruction, "extract")

        return try {
            val params = context.createExtractParams(options.schema)
            val resultNode = inference.extract(params)

            ExtractResult(success = true, message = "OK", data = resultNode)
        } catch (e: Exception) {
            logger.error("❌ extract.error requestId={} msg={}", context.sid, e.message, e)

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
        val opts = ExtractOptions(instruction = instruction, ExtractionSchema.Companion.DEFAULT)
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
    override suspend fun observe(instruction: String): List<ObserveResult> {
        val opts = ObserveOptions(instruction = instruction, returnAction = null)
        return observe(opts)
    }

    override suspend fun observe(options: ObserveOptions): List<ObserveResult> {
        val ctx = options.getContext()
        val context = if (ctx == null) {
            val instruction = promptBuilder.initObserveUserInstruction(options.instruction).instruction?.content
            stateManager.buildInitExecutionContext(options.copy(instruction = instruction))
        } else ctx

        val actionDescription = captureScreenshotAndObserve(options, context, options.resolve)

        return actionDescription.toObserveResults(context.agentState, context)
    }

    private suspend fun doObserveAct(options: ActionOptions): ActResult {
        val options = when {
            !options.resolve -> options.copy(action = promptBuilder.buildObserveActToolUsePrompt(options.action))
            else -> options
        }

        val context = requireNotNull(options.getContext()) { "Context is required to doObserveAct" }

        val actionDescription = captureScreenshotAndObserve(options, context, options.resolve)

        if (actionDescription.isComplete) {
            return ActResult.complete(actionDescription)
        }

        val observeResults = actionDescription.toObserveResults(context.agentState, context)

        if (observeResults.isEmpty()) {
            val msg = "⚠️ doObserveAct: No observe result"
            stateManager.addTrace(context.agentState, mapOf("event" to "observeActNoAction"), msg)
            return ActResult.failed(msg, action = options.action)
        }

        val resultsToTry = observeResults.take(config.maxResultsToTry)
        var lastError: String? = null
        val actResults = mutableListOf<ActResult>()
        for ((index, chosen) in resultsToTry.withIndex()) {
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

            stateManager.addTrace(
                context.agentState,
                mapOf(
                    "event" to "actSuccess", "candidateIndex" to (index + 1), "candidateTotal" to resultsToTry.size),
                "✅ act SUCCESS"
            )

            return actResult
        }

        val msg = "❌ All ${resultsToTry.size} candidates failed. Last error: $lastError"
        stateManager.addTrace(context.agentState, mapOf("event" to "actAllFailed", "candidates" to resultsToTry.size), msg)
        return ActResult.failed(msg, options.action)
    }

    private suspend fun executeToolCall(
        actionDescription: ActionDescription,
        context: ExecutionContext
    ): DetailedActResult {
        val step = context.step
        val toolCall = actionDescription.toolCall
            ?: return DetailedActResult.failed(actionDescription, message = "No toolCall")

        return try {
            logger.info(
                "🛠️ tool.exec sid={} step={} tool={} args={}",
                context.sid, context.step, toolCall.method, toolCall.arguments)

            val toolCallResult = toolExecutor.execute(actionDescription, "resolve, #$step")
            val success = toolCallResult.success

            val summary = "✅ ${toolCall.method} executed | " + if (success) "✅ success" else """☑️ executed"""
            stateManager.addTrace(context.agentState,
                mapOf("event" to "toolExecOk", "tool" to toolCall.method), summary)

            DetailedActResult(actionDescription, toolCallResult, success = success, summary)
        } catch (e: Exception) {
            logger.error("🛠️❌ tool.exec.fail sid={} step={} msg={}", context.sid, context.step, e.message, e)
            stateManager.addTrace(
                context.agentState,
                mapOf("event" to "toolExecUnexpectedFail", "tool" to toolCall.method),
                "💥 unexpected failure"
            )
            val message = "💥 unexpected failure | tool.exec.fail sid=${context.sid} step=${context.step}"
            DetailedActResult.failed(actionDescription, e, message)
        }
    }

    private suspend fun captureScreenshotAndObserve(
        options: Any,
        context: ExecutionContext,
        resolve: Boolean,
    ): ActionDescription {
        val observeOptions = options as? ObserveOptions
        val drawOverlay = observeOptions?.drawOverlay ?: false

        val params = when (options) {
            is ObserveOptions -> context.createObserveParams(
                options,
                fromAct = false,
                resolve = resolve
            )

            is ActionOptions -> context.createObserveActParams(resolve)
            else -> throw IllegalArgumentException("Not supported option")
        }

        val interactiveElements = context.agentState.browserUseState.getInteractiveElements()
        val actionDescription = try {
            if (drawOverlay) {
                domService.addHighlights(interactiveElements)
            }

            context.screenshotB64 = captureScreenshotWithRetry(context)

            withTimeout(config.llmInferenceTimeoutMs) {
                inference.observe(params, context)
            }
        } finally {
            if (drawOverlay) {
                runCatching { domService.removeHighlights(interactiveElements) }
                    .onFailure { e -> logger.warn("⚠️ Failed to remove highlights: ${e.message}") }
            }
        }

        return actionDescription
    }

    protected suspend fun captureScreenshotWithRetry(context: ExecutionContext): String? {
        val attempts = 2
        var lastEx: Exception? = null
        for (i in 1..attempts) {
            try {
                val screenshot = activeDriver.captureScreenshot()
                if (screenshot != null) {
                    logger.info("📸✅ screenshot.ok sid={} step={} size={} attempt={} ",
                        context.sid, context.step, screenshot.length, i)
                    return screenshot
                } else {
                    logger.info("📸⚪ screenshot.null sid={} step={} attempt={}", context.sid, context.step, i)
                }
            } catch (e: Exception) {
                lastEx = e
                logger.warn("📸⚠️ screenshot attempt {} failed: {}", i, e.message)
                delay(200)
            }
        }

        if (lastEx != null) {
            logger.error("📸❌ screenshot.fail sid={} msg={}", context.sid, lastEx.message, lastEx)
        }

        return null
    }

    override fun close() {

    }
}
