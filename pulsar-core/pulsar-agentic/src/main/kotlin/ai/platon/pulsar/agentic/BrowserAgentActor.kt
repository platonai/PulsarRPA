package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.agent.InferenceEngine
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.agent.detail.AgentStateManager
import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.agentic.ai.agent.detail.PageStateTracker
import ai.platon.pulsar.agentic.ai.tta.ContextToAction
import ai.platon.pulsar.agentic.ai.tta.DetailedActResult
import ai.platon.pulsar.agentic.tools.AgentToolManager
import ai.platon.pulsar.common.NotSupportedException
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.slf4j.helpers.MessageFormatter
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class BrowserAgentActor(
    val session: AgenticSession,
    val config: AgentConfig
) : PerceptiveAgent {
    private val logger = getLogger(BrowserAgentActor::class)
    private val closed = AtomicBoolean(false)

    internal val cta by lazy { ContextToAction(session.sessionConfig) }
    internal val inference by lazy { InferenceEngine(session, cta.chatModel) }
    internal val domService get() = inference.domService
    internal val promptBuilder = PromptBuilder()

    internal val toolExecutor by lazy { AgentToolManager(this) }

    internal val activeDriver get() = session.getOrCreateBoundDriver()

    // Helper classes for better code organization
    internal val pageStateTracker = PageStateTracker(session, config)
    internal val stateManager by lazy { AgentStateManager(this, pageStateTracker) }

    val startTime = Instant.now()
    // val isClosed get() = closed.get()

    override val uuid = UUID.randomUUID()
    override val stateHistory: List<AgentState> get() = stateManager.stateHistory
    override val processTrace: List<ProcessTrace> get() = stateManager.processTrace

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

    override suspend fun act(observe: ObserveResult): ActResult {
        val instruction = observe.agentState.instruction
        val agentState = observe.agentState
        val observeElement = observe.observeElement
        observeElement ?: return ActResult.Companion.failed("No observation", instruction)
        val actionDescription =
            observe.actionDescription ?: return ActResult.Companion.failed("No action description", instruction)
        val toolCall = observeElement.toolCall ?: return ActResult.Companion.failed("No tool call", instruction)
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
        val messages = AgentMessageList()
        val instruction = promptBuilder.initObserveUserInstruction(options.instruction)
        messages.addUser(instruction, name = "user_request")

        val context = (options.additionalContext["context"]?.get() as? ExecutionContext)
            ?: throw IllegalStateException("Illegal context")

        val actionDescription = takeScreenshotAndObserve(options, context, messages)

        return actionDescription.toObserveResults(context.agentState)
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
        return ActResult.Companion.failed(msg, instruction)
    }

    private suspend fun actInternal(
        actionDescription: ActionDescription,
        context: ExecutionContext
    ): DetailedActResult? {
        val step = context.step
        val toolCall = actionDescription.toolCall ?: return null

        return try {
            logger.info(
                "🛠️ tool.exec sid={} step={} tool={} args={}",
                context.sid,
                context.step,
                toolCall.method,
                toolCall.arguments
            )

            val toolCallResult = toolExecutor.execute(actionDescription, "resolve, #$step")


            val summary = "✅ ${toolCall.method} executed successfully"
            stateManager.trace(context.agentState, mapOf("event" to "toolExecOk", "tool" to toolCall.method), summary)
            DetailedActResult(actionDescription, toolCallResult, success = true, summary)
        } catch (e: Exception) {
            logger.error(
                "🛠️❌ tool.exec.fail sid={} step={} msg={}",
                context.sid,
                context.step,
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

    private suspend fun takeScreenshotAndObserve(
        options: Any,
        context: ExecutionContext,
        messages: AgentMessageList
    ): ActionDescription {
        val interactiveElements = context.agentState.browserUseState.getInteractiveElements()

        val actionDescription = try {
            domService.addHighlights(interactiveElements)

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

    protected suspend fun captureScreenshotWithRetry(context: ExecutionContext): String? {
        val attempts = 2
        var lastEx: Exception? = null
        for (i in 1..attempts) {
            try {
                val screenshot = activeDriver.captureScreenshot()
                if (screenshot != null) {
                    logger.info(
                        "📸✅ screenshot.ok sid={} step={} size={} attempt={} ",
                        context.sid,
                        context.step,
                        screenshot.length,
                        i
                    )
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

            return actionDescription
        } catch (e: Exception) {
            logger.error("❌ observeAct.observe.error requestId={} msg={}", requestId.take(8), e.message, e)
            ActionDescription(instruction, exception = e, modelResponse = ModelResponse.Companion.INTERNAL_ERROR)
        }
    }

    override fun close() {

    }
}
