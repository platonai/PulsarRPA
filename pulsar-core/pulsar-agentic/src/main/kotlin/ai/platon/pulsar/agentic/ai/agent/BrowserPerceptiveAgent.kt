package ai.platon.pulsar.agentic.ai.agent

import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.InteractiveElement
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.browser.driver.chrome.dom.BrowserState
import ai.platon.pulsar.browser.driver.chrome.dom.DomDebug
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCallExecutor
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class BrowserPerceptiveAgent(
    val driver: WebDriver,
    val maxSteps: Int = 100,
    val config: AgentConfig = AgentConfig(maxSteps = maxSteps)
) : PerceptiveAgent {
    private val logger = getLogger(this)

    private val baseDir = AppPaths.get("agent")
    private val conf get() = (driver as AbstractWebDriver).settings.config

    private val tta by lazy { TextToAction(conf) }
    private val inference by lazy { InferenceEngine(driver, tta.chatModel) }
    private val domService get() = inference.domService
    private val promptBuilder = PromptBuilder()

    // Enhanced state management
    private val _history = mutableListOf<String>()
    private val performanceMetrics = PerformanceMetrics()
    private val retryCounter = AtomicInteger(0)
    private val consecutiveFailureCounter = AtomicInteger(0)
    private val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

    // Action validation cache
    private val validationCache = ConcurrentHashMap<String, Boolean>()

    override val uuid = UUID.randomUUID()
    override val history: List<String> get() = _history

    override suspend fun resolve(instruction: String): ActResult {
        val opts = ActionOptions(action = instruction)
        return resolve(opts)
    }

    /**
     * Run `observe -> act -> observe -> act -> ...` loop to resolve the problem.
     * */
    override suspend fun resolve(action: ActionOptions): ActResult {
        Files.createDirectories(baseDir)
        val startTime = Instant.now()
        val sessionId = uuid.toString()

        return resolveProblemWithRetry(action, sessionId, startTime)
    }

    override suspend fun act(action: String): ActResult {
        val opts = ActionOptions(action = action)
        return act(opts)
    }

    /**
     * Execution with comprehensive error handling and retry mechanism.
     * Returns the final summary with enhanced error handling.
     */
    override suspend fun act(action: ActionOptions): ActResult {
        return doObserveAct(action)
    }

    override suspend fun act(observe: ObserveResult): ActResult {
        val method = observe.method?.trim().orEmpty()
        val selector = observe.locator.trim()
        val argsList = observe.arguments ?: emptyList()

        if (method.isBlank()) {
            val msg = "No actionable method provided in ObserveResult for selector: $selector"
            addToHistory(msg)
            return ActResult(success = false, message = msg, action = observe.description)
        }

        // Build a minimal ToolCall-like map for validation
        val toolArgs = mutableMapOf<String, Any?>()
        // Inject selector when the action likely targets an element
        val selectorActions = ToolCallExecutor.SELECTOR_ACTIONS
        val noSelectorActions = ToolCallExecutor.NO_SELECTOR_ACTIONS

        val lowerMethod = method
        val finalArgsList = mutableListOf<String>()
        if (lowerMethod !in noSelectorActions) {
            toolArgs["selector"] = selector
            // Prepend selector into function call args for element actions
            finalArgsList += selector
        }
        // Append original arguments (as-is, in order)
        finalArgsList += argsList

        // For waitForNavigation validation, provide oldUrl if not present
        if (lowerMethod == "waitForNavigation") {
            toolArgs["oldUrl"] = runCatching { driver.currentUrl() }.getOrDefault("")
        }
        // For navigateTo safety, validate URL if present
        if (lowerMethod == "navigateTo" && finalArgsList.isNotEmpty()) {
            if (!isSafeUrl(finalArgsList.first())) {
                val msg = "Blocked unsafe URL: ${finalArgsList.first()}"
                addToHistory(msg)
                return ActResult(false, msg, action = observe.description)
            }
        }

        // Pre-action validation (lightweight)
        if (config.enablePreActionValidation) {
            val ok = validateToolCall(ToolCall(lowerMethod, toolArgs))
            if (!ok) {
                val msg = "Tool call validation failed for $lowerMethod with selector ${selector.take(120)}"
                logStructured(
                    msg,
                    ExecutionContext(
                        uuid.toString(),
                        0,
                        "observe_act",
                        driver.currentUrl()
                    )
                )
                addToHistory(msg)
                return ActResult(false, msg, action = observe.description)
            }
        }

        // Build dispatcher-compatible function call: driver.method('arg1','arg2',...)
        fun escArg(s: String): String {
            // Use single quotes; escape any existing single quotes and backslashes
            return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"
        }

        val callArgs = finalArgsList.joinToString(",") { escArg(it) }
        val functionCall = "driver.$lowerMethod($callArgs)"

        // Execute via WebDriver dispatcher
        return try {
            val actionDesc = ActionDescription(
                functionCalls = listOf(functionCall),
                selectedElement = null,
                modelResponse = ModelResponse(
                    content = "ObserveResult action: ${observe.description}",
                    state = ResponseState.STOP
                )
            )
            val result = execute(actionDesc)

            val msg = "Action [$method] executed on selector: $selector".trim()
            addToHistory("observe.act -> $functionCall")
            ActResult(
                success = true,
                message = msg,
                action = functionCall
            )
        } catch (e: Exception) {
            logError("observe.act execution failed", e, uuid.toString())
            val msg = e.message ?: "Execution failed"
            addToHistory("observe.act FAIL ${method} ${selector.take(80)} -> ${msg}")
            ActResult(success = false, message = msg, action = "${method} ${selector}")
        }
    }

    override suspend fun extract(instruction: String): ExtractResult {
        val opts = ExtractOptions(instruction = instruction, schema = null)
        return extract(opts)
    }

    override suspend fun extract(options: ExtractOptions): ExtractResult {
        val instruction = promptBuilder.initExtractUserInstruction(options.instruction)
        val requestId = UUID.randomUUID().toString()
        logExtractStart(instruction, requestId)

        val schemaJson = buildSchemaJsonFromMap(options.schema)
        return try {
            val browserState = getBrowserState()

            val totalHeight = browserState.basicState.scrollState.totalHeight
            val viewportHeight = browserState.basicState.scrollState.viewport.height
            val params = ExtractParams(
                instruction = instruction,
                browserState = browserState,
                schema = schemaJson,
                chunksSeen = 0,
                chunksTotal = ceil(totalHeight / viewportHeight).roundToInt(),
                requestId = requestId,
                logInferenceToFile = config.enableStructuredLogging
            )

            val resultNode = inference.extract(params)
            addHistoryExtract(instruction, requestId, true)
            ExtractResult(success = true, message = "OK", data = resultNode)
        } catch (e: Exception) {
            logger.error("extract.error requestId={} msg={}", requestId.take(8), e.message, e)
            addHistoryExtract(instruction, requestId, false)
            ExtractResult(
                success = false,
                message = e.message ?: "extract failed",
                data = JsonNodeFactory.instance.objectNode()
            )
        }
    }

    override suspend fun observe(instruction: String): List<ObserveResult> {
        val opts = ObserveOptions(instruction = instruction, returnAction = null)
        return observe(opts)
    }

    override suspend fun observe(options: ObserveOptions): List<ObserveResult> {
        return doObserve(options)
    }

    private suspend fun getBrowserState(): BrowserState {
        val snapshotOptions = SnapshotOptions(
            maxDepth = 1000,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        return domService.getBrowserState(snapshotOptions)
    }

    protected suspend fun execute(action: ActionDescription): InstructionResult {
        if (action.functionCalls.isEmpty()) {
            return InstructionResult(listOf(), listOf(), action.modelResponse)
        }
        val functionCalls = action.functionCalls.take(1)
        val dispatcher = ToolCallExecutor()
        val functionResults = functionCalls.map { fc -> dispatcher.execute(fc, driver) }
        return InstructionResult(action.functionCalls, functionResults, action.modelResponse)
    }

    private suspend fun doObserve(options: ObserveOptions): List<ObserveResult> {
        val instruction = promptBuilder.initObserveUserInstruction(options.instruction)

        val browserState = getBrowserState()
        val params = ObserveParams(
            instruction = instruction,
            browserState = browserState,
            requestId = UUID.randomUUID().toString(),
            returnAction = options.returnAction ?: false,
            logInferenceToFile = config.enableStructuredLogging,
            fromAct = false
        )

        return doObserve1(params, browserState)
    }

    private suspend fun doObserveAct(action: ActionOptions): ActResult {
        // 1) Build instruction for action-oriented observe
        val toolCalls = ToolCallExecutor.SUPPORTED_TOOL_CALLS
        val instruction = promptBuilder.buildActObservePrompt(action.action, toolCalls, action.variables)
        require(instruction.contains("click")) {
            "Instruction must contains tool list for action: $action"
        }

        // 2) Optional settle wait before observing DOM (if provided)
        val settleMs = action.domSettleTimeoutMs?.toLong()?.coerceAtLeast(0L) ?: 0L
        if (settleMs > 0) {
            // TODO: wait for dom settle by checking network activity, DOM change, field change, etc
            driver.delay(settleMs)
        }

        // 3) Run observe with returnAction=true and fromAct=true so LLM returns an actionable method/args
        val browserState = getBrowserState()
        val params = ObserveParams(
            instruction = instruction,
            browserState = browserState,
            requestId = UUID.randomUUID().toString(),
            returnAction = true,
            logInferenceToFile = config.enableStructuredLogging,
            fromAct = true,
        )

        val results: List<ObserveResult> = doObserve1(params, browserState)

        if (results.isEmpty()) {
            val msg = "doObserveAct: No actionable element found"
            addToHistory(msg)
            return ActResult(false, msg, action = action.action)
        }

        // 4) Choose the top candidate and substitute variables into arguments
        val chosen = results.first()
        val method = chosen.method?.trim().orEmpty()
        if (method.isBlank()) {
            val msg = "observeAct: LLM returned no method for selected element"
            addToHistory(msg)
            return ActResult(false, msg, action = action.action)
        }

        // 5) Execute action, and optionally wait for navigation if caller provided timeoutMs
        val oldUrl = driver.currentUrl()
        val execResult = act(chosen)

        // If a timeout is provided and the action likely triggers navigation, wait for navigation
        val timeoutMs = action.timeoutMs?.toLong()?.takeIf { it > 0 }
        val maybeNavMethod = method in ToolCallExecutor.MAY_NAVIGATE_ACTIONS
        if (timeoutMs != null && maybeNavMethod && execResult.success) {
            // TODO: may not navigate
            val remainingTime = driver.waitForNavigation(oldUrl, timeoutMs)
            if (remainingTime <= 0) {
                logger.info("Timeout to wait for navigation | $action")
            }
        }

        return execResult.copy(action = action.action)
    }

    private suspend fun doObserve1(params: ObserveParams, browserState: BrowserState): List<ObserveResult> {
        val requestId: String = params.requestId

        // params.instruction:
        // "Find the most relevant element to perform an action on given the following action ..."
        logObserveStart(params.instruction, requestId)

        return try {
            val internalResults = inference.observe(params)
            val results = internalResults.elements.map { ele ->
                // The format of elementId: `\d+/\d+`

                // Multi selectors are supported: `xpath:`, `backend:`, `node:`, `hash:`
                val locator = ele.locator
                val frameIdIndex = locator.substringBefore("/").toIntOrNull()
                val backendNodeId = locator.substringAfterLast("/").toIntOrNull()
                val frameId = frameIdIndex?.let { browserState.domState.frameIds[it] }
                val fbnLocator = "fbn:$frameId/$backendNodeId"
                val node = browserState.domState.selectorMap[fbnLocator]
                if (node == null) {
                    logger.warn("Failed retrieving backend node | {} | {}", fbnLocator, ele)
                }
                // Use xpath here
                val xpathLocator = node?.xpath?.let { "xpath:$it" } ?: ""
                ObserveResult(
                    locator = xpathLocator,
                    description = ele.description,
                    backendNodeId = backendNodeId,
                    method = ele.method?.ifBlank { null },
                    arguments = ele.arguments?.takeIf { it.isNotEmpty() }
                )
            }
            addHistoryObserve(params.instruction, requestId, results.size, results.isNotEmpty())
            results
        } catch (e: Exception) {
            logger.error("observeAct.observe.error requestId={} msg={}", requestId.take(8), e.message, e)
            addHistoryObserve(params.instruction, requestId, 0, false)
            emptyList()
        }
    }

    /** Default rich extraction schema (JSON Schema string) */
    private val defaultExtractionSchemaJson: String by lazy {
        val schema = ExtractionSchema(
            listOf(
                ExtractionField("title", type = "string", description = "Page title"),
                ExtractionField(
                    "content",
                    type = "string",
                    description = "Primary textual content of the page",
                    required = false
                ),
                ExtractionField(
                    name = "links",
                    type = "array",
                    description = "Important hyperlinks on the page",
                    required = false,
                    items = ExtractionField(
                        name = "link",
                        type = "object",
                        properties = listOf(
                            ExtractionField("text", type = "string", description = "Anchor text", required = false),
                            ExtractionField("href", type = "string", description = "Href URL", required = false)
                        ),
                        required = false
                    )
                )
            )
        )
        schema.toJsonSchema()
    }

    private fun buildSchemaJsonFromMap(schema: Map<String, String>?): String {
        if (schema == null || schema.isEmpty()) return defaultExtractionSchemaJson
        return legacyMapToExtractionSchema(schema).toJsonSchema()
    }

    private fun logExtractStart(instruction: String, requestId: String) {
        logger.info("extract.start requestId={} instruction='{}'", requestId.take(8), instruction.take(120))
    }

    private fun logObserveStart(instruction: String, requestId: String) {
        logger.info("observe.start requestId={} instruction='{}'", requestId.take(8), instruction.take(120))
    }

    private fun addHistoryExtract(instruction: String, requestId: String, success: Boolean) {
        addToHistory("extract[$requestId] ${if (success) "OK" else "FAIL"} ${instruction.take(60)}")
    }

    private fun addHistoryObserve(instruction: String, requestId: String, size: Int, success: Boolean) {
        addToHistory("observe[$requestId] ${if (success) "OK" else "FAIL"} ${instruction.take(50)} -> $size elements")
    }

    /**
     * Enhanced execution with comprehensive error handling and retry mechanisms
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun resolveProblemWithRetry(
        action: ActionOptions,
        sessionId: String,
        startTime: Instant
    ): ActResult {
        var lastError: Exception? = null

        for (attempt in 0..config.maxRetries) {
            try {
                return doResolveProblem(action, sessionId, startTime, attempt)
            } catch (e: PerceptiveAgentError.TransientError) {
                lastError = e
                logError("Transient error on attempt ${'$'}{attempt + 1}", e, sessionId)
                if (attempt < config.maxRetries) {
                    val delay = calculateRetryDelay(attempt)
                    delay(delay)
                }
            } catch (e: PerceptiveAgentError.TimeoutError) {
                lastError = e
                logError("Timeout error on attempt ${'$'}{attempt + 1}", e, sessionId)
                if (attempt < config.maxRetries) {
                    delay(config.baseRetryDelayMs)
                }
            } catch (e: Exception) {
                lastError = e
                logError("Unexpected error on attempt ${'$'}{attempt + 1}", e, sessionId)
                if (shouldRetryError(e) && attempt < config.maxRetries) {
                    val delay = calculateRetryDelay(attempt)
                    delay(delay)
                } else {
                    // Non-retryable error, exit loop
                    break
                }
            }
        }

        return ActResult(
            success = false,
            message = "Failed after ${'$'}{config.maxRetries + 1} attempts. Last error: ${'$'}{lastError?.message}",
            action = action.action
        )
    }

    /**
     * Main execution logic with enhanced error handling and monitoring.
     * Returns the final summary with enhanced error handling.
     */
    private suspend fun doResolveProblem(
        action: ActionOptions,
        sessionId: String,
        startTime: Instant,
        attempt: Int
    ): ActResult {
        val overallGoal = action.action
        val context = ExecutionContext(sessionId, 0, "execute", getCurrentUrl())

        logStructured(
            "Starting PerceptiveAgent execution", context, mapOf(
                "instruction" to overallGoal.take(100),
                "attempt" to (attempt + 1),
                "maxSteps" to config.maxSteps,
                "maxRetries" to config.maxRetries
            )
        )

        val systemMsg = tta.buildOperatorSystemPrompt(overallGoal)
        var consecutiveNoOps = 0
        var step = 0

        try {
            while (step < config.maxSteps) {
                step++
                val stepStartTime = Instant.now()
                val stepContext = context.copy(stepNumber = step, actionType = "step")

                // Extract interactive elements each step (could be optimized via diffing later)
                // val tinyTree = domService.buildtinyTree()
                val browserState = getBrowserState()
                // val browserState = tta.extractInteractiveElementsDeferred(driver)

                logStructured(
                    "Executing step", stepContext, mapOf(
                        "step" to step,
                        "maxSteps" to config.maxSteps,
                        "consecutiveNoOps" to consecutiveNoOps,
                        "domStateSummary" to DomDebug.summarize(browserState.domState)
                    )
                )

                // Memory cleanup at intervals
                if (step % config.memoryCleanupIntervalSteps == 0) {
                    performMemoryCleanup(stepContext)
                }

                val screenshotB64 = captureScreenshotWithRetry(stepContext)
                val userMsg = buildUserMessage(overallGoal, browserState)

                // message: agent guide + overall goal + last action summary + current context message
                val message = buildExecutionMessage(systemMsg, userMsg, screenshotB64)
                // interactive elements are already appended to message
                val stepActionResult =
                    generateStepActionWithRetry(message, stepContext, listOf(), screenshotB64)

                if (stepActionResult == null) {
                    consecutiveNoOps++
                    val stop = handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    if (stop) break
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
                    val stop = handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    if (stop) break
                    continue
                }

                // Reset consecutive no-ops counter when we have a valid action
                consecutiveNoOps = 0

                // Execute the tool call with enhanced error handling
                val execSummary = doExecuteToolCall(toolCall, stepActionResult, step, stepContext)

                if (execSummary != null) {
                    addToHistory("#$step $execSummary")
                    updatePerformanceMetrics(step, stepStartTime, true)
                    logStructured("Step completed successfully", stepContext, mapOf("summary" to execSummary))
                } else {
                    // Treat validation failures or execution skips as no-ops to avoid infinite loops
                    consecutiveNoOps++
                    val stop = handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    if (stop) break
                    updatePerformanceMetrics(step, stepStartTime, false)
                }

                // Adaptive delay based on performance metrics
                delay(calculateAdaptiveDelay())
            }

            val executionTime = Duration.between(startTime, Instant.now())
            logStructured(
                "Execution completed", context, mapOf(
                    "steps" to step,
                    "executionTime" to executionTime.toString(),
                    "performanceMetrics" to performanceMetrics
                )
            )

            val summary = generateFinalSummary(overallGoal, context)
            val ok = summary.state != ResponseState.OTHER
            return ActResult(success = ok, message = summary.content, action = overallGoal)

        } catch (e: Exception) {
            val executionTime = Duration.between(startTime, Instant.now())
            logStructured(
                "Execution failed", context, mapOf(
                    "steps" to step,
                    "executionTime" to executionTime.toString(),
                    "error" to (e.message ?: "Unknown error")
                )
            )
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
    private fun classifyError(e: Exception, step: Int): PerceptiveAgentError {
        return when (e) {
            is PerceptiveAgentError -> e
            is TimeoutException -> PerceptiveAgentError.TimeoutError("Step ${'$'}step timed out", e)
            is SocketTimeoutException -> PerceptiveAgentError.TimeoutError("Network timeout at step ${'$'}step", e)
            is ConnectException -> PerceptiveAgentError.TransientError("Connection failed at step ${'$'}step", e)
            is UnknownHostException -> PerceptiveAgentError.TransientError(
                "DNS resolution failed at step ${'$'}step",
                e
            )

            is IOException -> {
                when {
                    e.message?.contains("connection") == true -> PerceptiveAgentError.TransientError(
                        "Connection issue at step $step",
                        e
                    )

                    e.message?.contains("timeout") == true -> PerceptiveAgentError.TimeoutError(
                        "Network timeout at step $step",
                        e
                    )

                    else -> PerceptiveAgentError.TransientError("IO error at step $step: ${e.message}", e)
                }
            }

            is IllegalArgumentException -> PerceptiveAgentError.ValidationError(
                "Validation error at step $step: ${e.message}",
                e
            )

            is IllegalStateException -> PerceptiveAgentError.PermanentError(
                "Invalid state at step $step: ${e.message}",
                e
            )

            else -> PerceptiveAgentError.TransientError("Unexpected error at step $step: ${e.message}", e)
        }
    }

    /**
     * Determines if an error should trigger a retry
     */
    private fun shouldRetryError(e: Exception): Boolean {
        return when (e) {
            is PerceptiveAgentError.TransientError, is PerceptiveAgentError.TimeoutError -> true
            is SocketTimeoutException, is ConnectException,
            is UnknownHostException -> true

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

    /**
     * Structured logging with context
     */
    private fun logStructured(
        message: String,
        context: ExecutionContext,
        additionalData: Map<String, Any> = emptyMap()
    ) {
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

        logger.error("PerceptiveAgent Error: {} - {}", message, errorData, error)
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
        message: String,
        context: ExecutionContext,
        interactiveElements: List<InteractiveElement>,
        screenshotB64: String?
    ): ActionDescription? {
        return try {
            // Use overload supplying extracted elements to avoid re-extraction
            tta.generateWebDriverAction(message, interactiveElements, screenshotB64)
        } catch (e: Exception) {
            logError("Action generation failed", e, context.sessionId)
            consecutiveFailureCounter.incrementAndGet()
            null
        }
    }

    /**
     * Executes tool call with enhanced error handling and validation
     */
    private suspend fun doExecuteToolCall(
        toolCall: ToolCall,
        action: ActionDescription,
        step: Int,
        context: ExecutionContext
    ): String? {
        if (config.enablePreActionValidation && !validateToolCall(toolCall)) {
            logStructured(
                "Tool call validation failed",
                context,
                mapOf("toolCall" to toolCall.name, "args" to toolCall.args)
            )
            addToHistory("#$step validation-failed ${toolCall.name}")
            return null
        }

        return try {
            logStructured(
                "Executing tool call", context, mapOf(
                    "toolName" to toolCall.name,
                    "toolArgs" to toolCall.args
                )
            )

            execute(action)
            consecutiveFailureCounter.set(0) // Reset on success

            // Extract summary from InstructionResult
            "${toolCall.name} executed successfully"
        } catch (e: Exception) {
            val failures = consecutiveFailureCounter.incrementAndGet()
            logError("Tool execution failed (consecutive failures: $failures)", e, context.sessionId)

            if (failures >= 3) {
                throw PerceptiveAgentError.PermanentError("Too many consecutive failures at step $step", e)
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
                "click", "fill", "press", "check", "uncheck", "exists", "isVisible", "focus", "scrollTo" -> validateElementAction(
                    toolCall.args
                )

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
    private suspend fun handleConsecutiveNoOps(consecutiveNoOps: Int, step: Int, context: ExecutionContext): Boolean {
        addToHistory("#$step no-op (consecutive: $consecutiveNoOps)")
        logStructured("No tool calls generated", context, mapOf("consecutiveNoOps" to consecutiveNoOps))

        if (consecutiveNoOps >= config.consecutiveNoOpLimit) {
            logStructured("Too many consecutive no-ops, stopping execution", context)
            // Signal caller to stop loop gracefully
            return true
        }

        val delay = calculateConsecutiveNoOpDelay(consecutiveNoOps)
        delay(delay)
        return false
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
    private fun handleTaskCompletion(parsed: ParsedResponse, step: Int, context: ExecutionContext) {
        logStructured(
            "Task completion detected", context, mapOf(
                "taskComplete" to (parsed.taskComplete ?: false)
            )
        )

        addToHistory("#$step complete: taskComplete=${parsed.taskComplete}")
    }

    /**
     * Updates performance metrics
     */
    private fun updatePerformanceMetrics(step: Int, stepStartTime: Instant, success: Boolean) {
        val stepTime = Duration.between(stepStartTime, Instant.now()).toMillis()
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
            ModelResponse("Failed to generate summary: ${e.message}", ResponseState.OTHER)
        }
    }

    private suspend fun buildUserMessage(instruction: String, browserState: BrowserState): String {
        val currentUrl = getCurrentUrl()
        val his = if (_history.isEmpty()) "(无)" else _history.takeLast(min(8, _history.size)).joinToString("\n")
        val interactiveSummary = browserState.domState.json
        return """
此前动作摘要：
$his

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

    private fun jsonElementToKotlin(e: JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonPrimitive -> {
            val p = e.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> {
                    val n = p.asNumber
                    val d = n.toDouble()
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
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()

            // Only allow http and https schemes
            if (scheme !in setOf("http", "https")) {
                logStructured(
                    "Blocked unsafe URL scheme", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("scheme" to (scheme ?: "null"), "url" to url.take(50))
                )
                return false
            }

            // Additional safety checks
            val host = uri.host?.lowercase() ?: return false

            // Block common dangerous domains/patterns
            val dangerousPatterns = listOf("localhost", "127.0.0.1", "0.0.0.0", "file://")
            if (dangerousPatterns.any { host.contains(it) }) {
                logStructured(
                    "Blocked potentially dangerous URL", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("host" to host, "reason" to "dangerous_pattern")
                )
                return false
            }

            // Validate port (block non-standard ports for http/https)
            val port = uri.port
            if (port != -1 && port !in setOf(80, 443, 8080, 8443)) {
                logStructured(
                    "Blocked URL with non-standard port", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("port" to port, "host" to host)
                )
                return false
            }

            true
        }.getOrDefault(false)
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
            logStructured(
                "Transcript persisted successfully", context,
                mapOf("lines" to _history.size + 10, "path" to log.toString())
            )
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

            val response = tta.chatModel.callUmSm(user, system)

            logStructured(
                "Summary generated successfully", context, mapOf(
                    "responseLength" to response.content.length,
                    "responseState" to response.state
                )
            )

            response
        } catch (e: Exception) {
            logError("Summary generation failed", e, context.sessionId)
            ModelResponse(
                "Failed to generate summary: ${e.message}",
                ResponseState.OTHER
            )
        }
    }
}
