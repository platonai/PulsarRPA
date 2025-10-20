package ai.platon.pulsar.agentic.ai.agent

import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.browser.driver.chrome.dom.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.MicroDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.DomDebug
import ai.platon.pulsar.browser.driver.chrome.dom.Locator
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCallExecutor
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
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

    // Reuse ToolCallExecutor to avoid recreation overhead (Medium Priority #14)
    private val toolCallExecutor = ToolCallExecutor()

    // Helper classes for better code organization
    private val pageStateTracker = PageStateTracker(driver, config)
    private val actionValidator = ActionValidator(driver, config)
    private val structuredLogger = StructuredLogger(logger, config)

    // Action validation cache
    private val validationCache = ConcurrentHashMap<String, Boolean>()

    // Enhanced state management
    private val _history = mutableListOf<String>()
    private val performanceMetrics = PerformanceMetrics()
    private val retryCounter = AtomicInteger(0)
    private val consecutiveFailureCounter = AtomicInteger(0)
    private val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

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
     * High Priority #1: Added overall timeout to prevent indefinite hangs.
     */
    override suspend fun act(action: ActionOptions): ActResult {
        return try {
            withTimeout(config.actTimeoutMs) {
                doObserveAct(action)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val msg = "Action timed out after ${config.actTimeoutMs}ms: ${action.action}"
            addToHistory("act TIMEOUT: ${action.action}")
            ActResult(success = false, message = msg, action = action.action)
        }
    }


    override suspend fun act(observe: ObserveResult): ActResult {
        val method = observe.method?.trim().orEmpty()
        val locator = Locator.parse(observe.locator)
        // Use the correct type for arguments from ObserveResult
        val argsMap: Map<String, String> = observe.arguments ?: emptyMap()

        if (method.isBlank()) {
            val msg = "No actionable method provided in ObserveResult for locator: $locator"
            addToHistory(msg)
            return ActResult(success = false, message = msg, action = observe.description)
        }

        // Build a minimal ToolCall-like map for validation and execution
        val toolArgs = mutableMapOf<String, Any?>()
        // Copy provided arguments first (string values)
        argsMap.forEach { (k, v) -> toolArgs[k] = v }

        // Inject selector only when the action targets an element
        val selectorActions = ToolCallExecutor.SELECTOR_ACTIONS
        val noSelectorActions = ToolCallExecutor.NO_SELECTOR_ACTIONS

        val domain = "driver"
        val lowerMethod = method
        val backendNodeId = observe.backendNodeId
        // backend selector is supported since 20251020
        val selector = observe.backendNodeId?.let { "backend:$backendNodeId" }
        if (lowerMethod in selectorActions) {
            // val selector = locator?.absoluteSelector ?: toolArgs["selector"]?.toString()
            toolArgs["selector"] = selector
            if (selector == null) {
                val msg = "A selector is required for $locator | $observe"
                addToHistory(msg)
                return ActResult(success = false, message = msg, action = observe.description)
            }
        }

        // For waitForNavigation validation, provide oldUrl if not present
        if (lowerMethod == "waitForNavigation") {
            if (toolArgs["oldUrl"]?.toString().isNullOrBlank()) {
                toolArgs["oldUrl"] = driver.currentUrl()
            }
            // Provide a reasonable default timeout if not set
            if (toolArgs["timeoutMillis"] == null) {
                toolArgs["timeoutMillis"] = 5000L
            }
        }

        // For navigateTo safety, validate URL; map a single unnamed arg to url if necessary
        if (lowerMethod == "navigateTo") {
            if (!toolArgs.containsKey("url")) {
                // Heuristic: if only one argument exists, treat it as url
                val onlyArgValue = argsMap.values.firstOrNull()
                if (!onlyArgValue.isNullOrBlank()) toolArgs["url"] = onlyArgValue
            }
            val url = toolArgs["url"]?.toString().orEmpty()
            if (!isSafeUrl(url)) {
                val msg = "Blocked unsafe URL: $url"
                addToHistory(msg)
                return ActResult(false, msg, action = observe.description)
            }
        }

        // Pre-action validation (lightweight), reuse local ToolCall(data) class
        if (config.enablePreActionValidation) {
            val ok = actionValidator.validateToolCall(ToolCall(lowerMethod, toolArgs))
            if (!ok) {
                val msg = "Tool call validation failed for $lowerMethod with selector ${selector?.take(120)}"
                structuredLogger.log(
                    msg,
                    ExecutionContext(
                        uuid.toString(),
                        0,
                        "observe_act",
                        runCatching { driver.currentUrl() }.getOrDefault("")
                    )
                )
                addToHistory(msg)
                return ActResult(false, msg, action = observe.description)
            }
        }

        // Prefer using ToolCallExecutor's canonical driver line builder
        val toolCall = ToolCall(
            domain = "driver",
            name = lowerMethod,
            args = toolArgs
        )

        // Execute via WebDriver dispatcher
        return try {
            val actionDesc = ActionDescription(
                expressions = listOf(),
                toolCall = toolCall,
                modelResponse = ModelResponse(
                    content = "ObserveResult action: ${observe.description}",
                    state = ResponseState.STOP
                )
            )
            val result = execute(actionDesc)

            val msg = "Action [$lowerMethod] executed on selector: $locator".trim()
            addToHistory("observe.act -> ${toolCall.name}")
            ActResult(
                success = true,
                message = msg,
                action = toolCall.name
            )
        } catch (e: Exception) {
            logError("observe.act execution failed", e, uuid.toString())
            val msg = e.message ?: "Execution failed"
            addToHistory("observe.act FAIL $lowerMethod ${locator?.absoluteSelector?.take(80)} -> $msg")
            ActResult(success = false, message = msg, action = "$lowerMethod $locator")
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
            val domState = getActiveDOMState()

            val totalHeight = domState.browserState.scrollState.totalHeight
            val viewportHeight = domState.browserState.scrollState.viewport.height
            val params = ExtractParams(
                instruction = instruction,
                browserUseState = domState,
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

    private suspend fun getActiveDOMState(): BrowserUseState {
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

        return domService.getActiveDOMState(snapshotOptions)
    }

    /**
     * High Priority #3: Improved DOM settle detection
     * Waits for DOM to stabilize by checking for mutations
     */
    private suspend fun waitForDOMSettle(timeoutMs: Long, checkIntervalMs: Long) {
        val startTime = System.currentTimeMillis()
        var lastDomHash: Int? = null
        var stableCount = 0
        val requiredStableChecks = 3 // Require 3 consecutive stable checks

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Get a lightweight DOM fingerprint
                val currentHtml = driver.evaluate("document.body?.innerHTML?.length || 0").toString()
                val currentHash = currentHtml.hashCode()

                if (currentHash == lastDomHash) {
                    stableCount++
                    if (stableCount >= requiredStableChecks) {
                        logger.debug("DOM settled after ${System.currentTimeMillis() - startTime}ms")
                        return
                    }
                } else {
                    stableCount = 0
                }

                lastDomHash = currentHash
                delay(checkIntervalMs)
            } catch (e: Exception) {
                logger.warn("Error checking DOM stability: ${e.message}")
                delay(checkIntervalMs)
            }
        }

        logger.debug("DOM settle timeout after ${timeoutMs}ms")
    }

    protected suspend fun execute(action: ActionDescription): InstructionResult {
        val toolCall = action.toolCall
        if (toolCall == null) {
            return InstructionResult(listOf(), listOf(), action.modelResponse)
        }

        val dispatcher = ToolCallExecutor()
        val result = dispatcher.execute(toolCall, driver)
        return InstructionResult(action.expressions, listOf(result), action.modelResponse)
    }

    private suspend fun doObserve(options: ObserveOptions): List<ObserveResult> {
        val instruction = promptBuilder.initObserveUserInstruction(options.instruction)

        val browserState = getActiveDOMState()
        val params = ObserveParams(
            instruction = instruction,
            browserUseState = browserState,
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
        val instruction = promptBuilder.buildToolUsePrompt(action.action, toolCalls, action.variables)
        require(instruction.contains("click")) {
            "Instruction must contains tool list for action: $action"
        }

        // 2) Optional settle wait before observing DOM (if provided)
        val settleMs = action.domSettleTimeoutMs?.toLong()?.coerceAtLeast(0L) ?: config.domSettleTimeoutMs
        if (settleMs > 0) {
            // High Priority #3: Improved DOM settle detection
            pageStateTracker.waitForDOMSettle(settleMs, config.domSettleCheckIntervalMs)
        }

        // 3) Run observe with returnAction=true and fromAct=true so LLM returns an actionable method/args
        val browserState = getActiveDOMState()
        val params = ObserveParams(
            instruction = instruction,
            browserUseState = browserState,
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

        // High Priority #2: Try multiple results with fallback instead of only first()
        val resultsToTry = results.take(config.maxResultsToTry)
        var lastError: String? = null

        for ((index, chosen) in resultsToTry.withIndex()) {
            val method = chosen.method?.trim().orEmpty()
            if (method.isBlank()) {
                lastError = "LLM returned no method for candidate ${index + 1}"
                continue
            }

            // 5) Execute action, and optionally wait for navigation if caller provided timeoutMs
            val oldUrl = driver.currentUrl()
            val execResult = try {
                act(chosen)
            } catch (e: Exception) {
                lastError = "Execution failed for candidate ${index + 1}: ${e.message}"
                logger.warn("Failed to execute candidate ${index + 1}: ${e.message}")
                continue
            }

            if (!execResult.success) {
                lastError = "Candidate ${index + 1} failed: ${execResult.message}"
                continue
            }

            // If a timeout is provided and the action likely triggers navigation, wait for navigation
            val timeoutMs = action.timeoutMs?.toLong()?.takeIf { it > 0 }
            val maybeNavMethod = method in ToolCallExecutor.MAY_NAVIGATE_ACTIONS
            if (timeoutMs != null && maybeNavMethod && execResult.success) {
                // High Priority #4: Fail explicitly on navigation timeout
                val remainingTime = driver.waitForNavigation(oldUrl, timeoutMs)
                if (remainingTime <= 0) {
                    val navError = "Navigation timeout after ${timeoutMs}ms for action: ${action.action}"
                    logger.warn(navError)
                    addToHistory("act NAVIGATION_TIMEOUT: ${action.action}")
                    return ActResult(success = false, message = navError, action = action.action)
                }
            }

            // Success! Return with original action text
            addToHistory("act SUCCESS (candidate ${index + 1}/${resultsToTry.size}): ${action.action}")
            return execResult.copy(action = action.action)
        }

        // All candidates failed
        val msg = "All ${resultsToTry.size} candidates failed. Last error: $lastError"
        addToHistory(msg)
        return ActResult(false, msg, action = action.action)
    }

    private suspend fun doObserve1(params: ObserveParams, browserUseState: BrowserUseState): List<ObserveResult> {
        val requestId: String = params.requestId

        // params.instruction:
        // "Find the most relevant element to perform an action on given the following action ..."
        logObserveStart(params.instruction, requestId)

        return try {
            // High Priority #6: Add timeout to LLM inference calls
            val internalResults = withTimeout(config.llmInferenceTimeoutMs) {
                inference.observe(params)
            }
            val results = internalResults.elements.map { ele ->
                // Multi selectors are supported: `cssPath`, `xpath:`, `backend:`, `node:`, `hash:`, `fbn`, `index`
                val selector = ele.selector.trim()
                val frameIdIndex = selector.substringBefore("/").toIntOrNull()
                val backendNodeId = selector.substringAfterLast("/").toIntOrNull()
                val frameId = frameIdIndex?.let { browserUseState.domState.frameIds[it] }
                val fbnLocator = "fbn:$frameId/$backendNodeId"
                val node = browserUseState.domState.selectorMap[fbnLocator]
                if (node == null) {
                    logger.warn("Failed retrieving backend node | {} | {}", fbnLocator, ele)
                }
                // Use xpath here
                val xpathLocator = node?.xpath?.let { "xpath:$it" } ?: ""
                ObserveResult(
                    locator = xpathLocator.trim(),
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
                logError("Transient error on attempt ${attempt + 1}", e, sessionId)
                if (attempt < config.maxRetries) {
                    val delay = calculateRetryDelay(attempt)
                    delay(delay)
                }
            } catch (e: PerceptiveAgentError.TimeoutError) {
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
        sessionId: String,
        startTime: Instant,
        attempt: Int
    ): ActResult {
        val overallGoal = action.action
        val context = ExecutionContext(sessionId, 0, "execute", getCurrentUrl())

        structuredLogger.log(
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

                // Extract interactive nodes each step (could be optimized via diffing later)
                val browserState = getActiveDOMState()

                // Medium Priority #10: Detect if page state hasn't changed
                val unchangedCount = pageStateTracker.checkStateChange(browserState)
                if (unchangedCount >= 3) {
                    structuredLogger.log("Page state unchanged for $unchangedCount steps, potential loop detected", stepContext)
                    consecutiveNoOps++
                }

                structuredLogger.log(
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

                // Medium Priority #9: Configurable screenshot frequency
                val screenshotB64 = if (step % config.screenshotEveryNSteps == 0) {
                    captureScreenshotWithRetry(stepContext)
                } else {
                    null
                }
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
                val execSummary = doExecuteToolCall(stepActionResult, step, stepContext)

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
            is TimeoutException -> PerceptiveAgentError.TimeoutError("Step $step timed out", e)
            is SocketTimeoutException -> PerceptiveAgentError.TimeoutError("Network timeout at step $step", e)
            is ConnectException -> PerceptiveAgentError.TransientError("Connection failed at step $step", e)
            is UnknownHostException -> PerceptiveAgentError.TransientError(
                "DNS resolution failed at step $step",
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
     * Medium Priority #13: Output proper JSON logs
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

        // Medium Priority #13: Proper JSON logging
        val logData = buildMap {
            put("sessionId", context.sessionId)
            put("step", context.stepNumber)
            put("actionType", context.actionType)
            put("timestamp", context.timestamp.toString())
            put("message", message)
            putAll(additionalData)
        }

        // Use Gson to create proper JSON string
        val jsonLog = JsonParser.parseString(
            logData.entries.joinToString(",", "{", "}") { (k, v) ->
                """"$k":${when(v) {
                    is String -> "\"$v\""
                    is Number, is Boolean -> v.toString()
                    else -> "\"$v\""
                }}"""
            }
        )

        logger.info("{}", jsonLog)
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
        interactiveNodes: List<MicroDOMTreeNode> = listOf(),
        screenshotB64: String?
    ): ActionDescription? {
        return try {
            // Use overload supplying extracted elements to avoid re-extraction
            tta.generate(message, interactiveNodes, screenshotB64)
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
        action: ActionDescription,
        step: Int,
        context: ExecutionContext
    ): String? {
        val toolCall = requireNotNull(action.toolCall) { "Tool call is required" }
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
     * High Priority #5: Deny unknown actions by default for security
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
                // High Priority #5: Deny unknown actions by default
                else -> {
                    if (config.denyUnknownActions) {
                        logger.warn("Unknown action blocked: ${toolCall.name}")
                        false
                    } else {
                        logger.warn("Unknown action allowed (config): ${toolCall.name}")
                        true
                    }
                }
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
     * Medium Priority #11: Improved validation with selector syntax checking
     */
    private fun validateElementAction(args: Map<String, Any?>): Boolean {
        val selector = args["selector"]?.toString() ?: return false

        // Basic validation
        if (selector.isBlank() || selector.length > config.maxSelectorLength) {
            return false
        }

        // Medium Priority #11: Check for common selector syntax patterns
        val hasValidPrefix = selector.startsWith("xpath:") ||
                            selector.startsWith("css:") ||
                            selector.startsWith("#") ||
                            selector.startsWith(".") ||
                            selector.startsWith("//") ||
                            selector.startsWith("fbn:") ||
                            selector.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*$")) // tag name

        return hasValidPrefix
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

    private suspend fun buildUserMessage(instruction: String, browserUseState: BrowserUseState): String {
        val currentUrl = getCurrentUrl()
        val his = if (_history.isEmpty()) "(无)" else _history.takeLast(min(8, _history.size)).joinToString("\n")
        val interactiveNodesJson =  DOMSerializer.toJson(browserUseState.domState.interactiveNodes)

        return """
此前动作摘要：
$his

可交互元素：
$interactiveNodesJson

请基于当前页面截图、交互元素与历史动作，规划下一步（严格单步原子动作），若无法推进请输出空 tool_calls。
当前目标：$instruction
当前URL：$currentUrl
		""".trimIndent()
    }

    private data class ParsedResponse(
        val toolCalls: List<ToolCall>,
        val taskComplete: Boolean?,
    )

    private fun parseOperatorResponse(content: String): ParsedResponse {
        val domain = "driver"

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
                            if (!name.isNullOrBlank()) toolCalls += ToolCall(domain, name, args)
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
            val calls = tta.parseToolCalls(content).map { ToolCall(domain, it.name, it.args) }
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
     * Medium Priority #12: Made configurable for localhost and ports
     */
    private fun isSafeUrl(url: String): Boolean {
        if (url.isBlank()) return false

        if (!URLUtils.isStandard(url)) {
            return false
        }

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

            // Medium Priority #12: Configurable localhost blocking
            if (!config.allowLocalhost) {
                val dangerousPatterns = listOf("localhost", "127.0.0.1", "0.0.0.0", "::1")
                if (dangerousPatterns.any { host.contains(it) }) {
                    logStructured(
                        "Blocked localhost URL (config)", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                        mapOf("host" to host, "reason" to "localhost_blocked")
                    )
                    return false
                }
            }

            // Medium Priority #12: Validate port with configurable whitelist
            val port = uri.port
            if (port != -1 && port !in config.allowedPorts) {
                logStructured(
                    "Blocked URL with non-whitelisted port", ExecutionContext(uuid.toString(), 0, "url_validation", ""),
                    mapOf("port" to port, "host" to host, "allowedPorts" to config.allowedPorts)
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
