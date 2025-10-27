package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.agent.*
import ai.platon.pulsar.agentic.ai.agent.detail.*
import ai.platon.pulsar.agentic.ai.support.AgentTool
import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.ActionExecuteResult
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.browser.driver.chrome.dom.DomDebug
import ai.platon.pulsar.browser.driver.chrome.dom.Locator
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.browser.driver.chrome.dom.model.TabState
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.Pson
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import kotlinx.coroutines.TimeoutCancellationException
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
import kotlin.math.min
import kotlin.math.pow

/**
 * Configuration for enhanced error handling and retry mechanisms
 */
data class AgentConfig(
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
    val enablePreActionValidation: Boolean = true,
    // New configuration options for fixes
    val actTimeoutMs: Long = 60_000,
    val llmInferenceTimeoutMs: Long = 60_000,
    val maxResultsToTry: Int = 3,
    val screenshotEveryNSteps: Int = 1,
    val domSettleTimeoutMs: Long = 2000,
    val domSettleCheckIntervalMs: Long = 100,
    val allowLocalhost: Boolean = false,
    val allowedPorts: Set<Int> = setOf(80, 443, 8080, 8443, 3000, 5000, 8000, 9000),
    val maxSelectorLength: Int = 1000,
    val denyUnknownActions: Boolean = false,
    // Overall timeout for resolve() to avoid indefinite hangs
    val resolveTimeoutMs: Long = 60 * 60_000
)

class BrowserPerceptiveAgent(
    val driver: WebDriver,
    val session: AgenticSession,
    val maxSteps: Int = 100,
    val config: AgentConfig = AgentConfig(maxSteps = maxSteps)
) : PerceptiveAgent {
    private val ownerLogger = getLogger(this)
    private val slogger = StructuredAgentLogger(ownerLogger, config)
    private val logger = ownerLogger

    private val activeDriver get() = session.boundDriver ?: driver
    private val baseDir = AppPaths.get("agent")
    private val conf get() = (activeDriver as AbstractWebDriver).settings.config

    private val tta by lazy { TextToAction(conf) }
    private val inference by lazy { InferenceEngine(session, tta.chatModel) }
    private val domService get() = inference.domService
    private val promptBuilder = PromptBuilder()

    // Reuse ToolCallExecutor to avoid recreation overhead (Medium Priority #14)
    private val toolCallExecutor = ToolCallExecutor()

    // Helper classes for better code organization
    private val pageStateTracker = PageStateTracker(session, config)
    private val actionValidator = ActionValidator(config)

    // Action validation cache
    private val validationCache = ConcurrentHashMap<String, Boolean>()

    // Enhanced state management
    private val _history = mutableListOf<AgentState>()
    private val _recordHistory = mutableListOf<String>()
    private val performanceMetrics = PerformanceMetrics()
    private val retryCounter = AtomicInteger(0)
    private val consecutiveFailureCounter = AtomicInteger(0)
    private val stepExecutionTimes = ConcurrentHashMap<Int, Long>()

    // Jackson mapper aligned with project conventions
    private val jsonMapper by lazy { pulsarObjectMapper() }

    override val uuid = UUID.randomUUID()
    override val history: List<AgentState> get() = _history

    override suspend fun resolve(problem: String): ActResult {
        val opts = ActionOptions(action = problem)
        return resolve(opts)
    }

    /**
     * Run `observe -> act -> observe -> act -> ...` loop to resolve the problem.
     * */
    override suspend fun resolve(action: ActionOptions): ActResult {
        Files.createDirectories(baseDir)
        val startTime = Instant.now()
        val sessionId = uuid.toString()

        // Add start history for better traceability
        addToRecordHistory("resolve START session=${sessionId.take(8)} goal='${Strings.compactWhitespaces(action.action, 160)}' maxSteps=${config.maxSteps} maxRetries=${config.maxRetries}")

        // Overall timeout to prevent indefinite hangs for a full resolve session
        return try {
            val result = withTimeout(config.resolveTimeoutMs) {
                resolveProblemWithRetry(action, sessionId, startTime)
            }
            val dur = Duration.between(startTime, Instant.now()).toMillis()
            addToHistory("resolve DONE session=${sessionId.take(8)} success=${result.success} dur=${dur}ms")
            result
        } catch (_: TimeoutCancellationException) {
            val msg = "Resolve timed out after ${config.resolveTimeoutMs}ms: ${action.action}"
            addToHistory("resolve TIMEOUT: ${Strings.compactWhitespaces(action.action, 160)}")
            ActResult(success = false, message = msg, action = action.action)
        }
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
        } catch (_: TimeoutCancellationException) {
            val msg = "Action timed out after ${config.actTimeoutMs}ms: ${action.action}"
            addToHistory("act TIMEOUT: ${action.action}")
            ActResult(success = false, message = msg, action = action.action)
        }
    }

    override suspend fun act(observe: ObserveResult): ActResult {
        val method = observe.method?.trim()?.takeIf { it.isNotEmpty() }
        val locator = observe.locator?.let { Locator.parse(it) }
        // Use the correct type for arguments from ObserveResult
        val argsMap: Map<String, Any?> = observe.arguments ?: emptyMap()

        if (method == null || locator == null) {
            val msg = "No valuable observations were made | " + Pson.toJson(observe)
            addToHistory(msg)
            return ActResult(success = false, message = msg, action = "")
        }

        // Build a minimal ToolCall-like map for validation and execution
        val toolArgs = mutableMapOf<String, Any?>()
        // Copy provided arguments first (string values)
        argsMap.forEach { (k, v) -> toolArgs[k] = v }

        // Inject selector only when the action targets an element
        val selectorActions = AgentTool.SELECTOR_ACTIONS
        // val noSelectorActions = ToolCallExecutor.NO_SELECTOR_ACTIONS // unused

        // val domain = "driver" // unused
        val domain = observe.domain ?: "unknown"
        val lowerMethod = method
        val backendNodeId = observe.backendNodeId
        // backend selector is supported since 20251020; fallback to provided locator string when absent
        val selector = observe.backendNodeId?.let { "backend:$backendNodeId" } ?: observe.locator?.takeIf { it.isNotBlank() }
        if (lowerMethod in selectorActions) {
            toolArgs["selector"] = selector
            if (selector == null) {
                val msg = "No selector observation were made $locator | $observe"
                addToHistory(msg)
                return ActResult(success = false, message = msg, action = method)
            }
        }

        val driver = requireNotNull(activeDriver)
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
            val url = toolArgs["url"]?.toString()
            if (url == null) {
                val msg = "No url observation were made | " + Pson.toJson(observe)
                addToHistory(msg)
                return ActResult(false, msg, action = method)
            }

            if (!isSafeUrl(url)) {
                val msg = "Blocked unsafe URL: $url"
                addToHistory(msg)
                return ActResult(false, msg, action = method)
            }
        }

        // Pre-action validation (lightweight), reuse local ToolCall(data) class
        if (config.enablePreActionValidation) {
            val ok = actionValidator.validateToolCall(ToolCall(domain, lowerMethod, toolArgs))
            if (!ok) {
                val msg = "Tool call validation failed for $lowerMethod with selector ${selector?.take(120)}"
                val sid = uuid.toString().take(8)
                val curUrl = runCatching { driver.currentUrl() }.getOrDefault("")
                logger.info("observe_act.validate.fail sid={} url={} msg={} ", sid, curUrl, msg)
                addToHistory(msg)
                return ActResult(false, msg, action = method)
            }
        }

        // Prefer using ToolCallExecutor's canonical driver line builder
        val toolCall = ToolCall(
            domain = observe.domain ?: "unknown",
            method = lowerMethod,
            arguments = toolArgs
        )

        // Execute via WebDriver dispatcher
        return try {
            val observeElement = observe.observeElements?.firstOrNull()
            val action = ActionDescription(observeElement = observeElement)
            doExecute(action)

            val msg = "Action [$lowerMethod] executed on selector: $locator".trim()
            addToHistory("observe.act -> ${toolCall.method}")
            ActResult(success = true, message = msg, action = toolCall.method)
        } catch (e: Exception) {
            logger.error("observe.act execution failed sid={} msg={}", uuid.toString().take(8), e.message, e)
            val msg = e.message ?: "Execution failed"
            addToHistory("observe.act FAIL $lowerMethod ${locator.absoluteSelector} -> $msg")
            ActResult(success = false, message = msg, action = toolCall.method)
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
            val browserUseState = getBrowserUseState()

            val scrollState = browserUseState.browserState.scrollState
            val params = ExtractParams(
                instruction = instruction,
                browserUseState = browserUseState,
                schema = schemaJson,
                chunksSeen = scrollState.chunksSeen,
                chunksTotal = scrollState.chunksTotal,
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

    private suspend fun getBrowserUseState(): BrowserUseState {
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

        val baseState = domService.getBrowserUseState(snapshotOptions = snapshotOptions)

        // Inject tabs information
        return injectTabsInfo(baseState)
    }

    /**
     * Inject tabs information into BrowserUseState.
     * Collects all tabs from the current browser and marks the active tab.
     */
    private suspend fun injectTabsInfo(baseState: BrowserUseState): BrowserUseState {
        val currentDriver = session.boundDriver ?: return baseState
        val browser = currentDriver.browser

        val tabs = browser.drivers.map { (tabId, driver) ->
            val url = try { driver.currentUrl() } catch (_: Exception) { "about:blank" }
            val title = try {
                driver.evaluate("document.title").toString().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
            TabState(
                id = tabId,
                driverId = driver.id,
                url = url,
                title = title,
                active = (driver == currentDriver)
            )
        }

        val activeTabId = browser.drivers.entries.find { it.value == currentDriver }?.key

        val enhancedBrowserState = baseState.browserState.copy(
            tabs = tabs,
            activeTabId = activeTabId
        )

        return BrowserUseState(
            browserState = enhancedBrowserState,
            domState = baseState.domState
        )
    }

    private suspend fun doExecute(action: ActionDescription): InstructionResult {
        val toolCall = action.toolCall ?: return InstructionResult(action = action)
        val driver = activeDriver
        val result = toolCallExecutor.execute(toolCall, driver)

        // Handle browser.switchTab - bind the new driver to the session
        if (toolCall.method == "switchTab") {
            handleSwitchTab(result)
        }

        return InstructionResult(action.expressions, listOf(result), action = action)
    }

    /**
     * Handle switching to a new tab by binding the target driver to the session.
     */
    private fun handleSwitchTab(result: Any?) {
        when (result) {
            is Int -> {
                // result is the driverId
                val browser = requireNotNull(session.boundDriver?.browser) {
                    "No browser bound to session"
                }
                val targetDriver = browser.drivers.values.find { it.id == result }
                if (targetDriver != null) {
                    session.bindDriver(targetDriver)
                    logger.info("Session bound to new driver {} after switchTab", result)
                } else {
                    logger.warn("switchTab returned driverId {} but driver not found in browser", result)
                }
            }
            is Map<*, *> -> {
                // Error response from switchTab
                val error = result["error"] as? String
                val message = result["message"] as? String
                logger.warn("switchTab failed: {} - {}", error, message)
            }
            else -> {
                logger.warn("Unexpected switchTab result type: {}", result?.javaClass?.simpleName)
            }
        }
    }

    private suspend fun doObserve(options: ObserveOptions): List<ObserveResult> {
        // Returns:
        // `options.instruction`
        // OR: "Find elements that can be used for any future actions in the page."
        val instruction = promptBuilder.initObserveUserInstruction(options.instruction)

        val browserUseState = getBrowserUseState()
        val params = ObserveParams(
            instruction = instruction,
            browserUseState = browserUseState,
            requestId = UUID.randomUUID().toString(),
            returnAction = options.returnAction ?: false,
            logInferenceToFile = config.enableStructuredLogging,
            fromAct = false
        )

        return doObserve1(params, browserUseState)
    }

    private suspend fun doObserveAct(action: ActionOptions): ActResult {
        // 1) Build instruction for action-oriented observe
        val toolCalls = AgentTool.SUPPORTED_TOOL_CALLS
        val instruction = promptBuilder.buildObserveActToolUsePrompt(action.action, toolCalls, action.variables)
        require(instruction.contains("click")) {
            "Instruction must contains tool list for action: $action"
        }

        // 3) Run observe with returnAction=true and fromAct=true so LLM returns an actionable method/args
        val browserUseState = getBrowserUseState()
        val params = ObserveParams(
            instruction = instruction,
            browserUseState = browserUseState,
            requestId = UUID.randomUUID().toString(),
            returnAction = true,
            logInferenceToFile = config.enableStructuredLogging,
            fromAct = true,
        )

        val results: List<ObserveResult> = doObserve1(params, browserUseState)

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
            val driver = activeDriver
            val oldUrl = driver.currentUrl()
            val execResult = try {
                act(chosen)
            } catch (e: Exception) {
                lastError = "Execution failed for candidate ${index + 1}: ${e.message}"
                logger.warn("Failed to execute candidate {}: {}", index + 1, e.message)
                continue
            }

            if (!execResult.success) {
                lastError = "Candidate ${index + 1} failed: ${execResult.message}"
                continue
            }

            // If a timeout is provided and the action likely triggers navigation, wait for navigation
            val timeoutMs = action.timeoutMs?.toLong()?.takeIf { it > 0 }
            val maybeNavMethod = method in AgentTool.MAY_NAVIGATE_ACTIONS
            if (timeoutMs != null && maybeNavMethod) {
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
                requireNotNull(ele.locator)
                val locator = browserUseState.domState.getAbsoluteFBNLocator(ele.locator)
                val node = if (locator != null) browserUseState.domState.locatorMap[locator] else null
                if (node == null) {
                    logger.warn("Failed retrieving backend node | {} | {}", locator, Pson.toJson(ele))
                }
                // Use xpath here
                val xpathLocator = node?.xpath?.let { "xpath:$it" } ?: ""

                ObserveResult(
                    description = ele.description ?: "(No comment ...)",
                    locator = xpathLocator.trim(),
                    backendNodeId = locator?.backendNodeId,
                    domain = ele.domain?.ifBlank { null },
                    method = ele.method?.ifBlank { null },
                    arguments = ele.arguments?.takeIf { it.isNotEmpty() },

                    currentPageContentSummary = ele.currentPageContentSummary,
                    actualLastActionImpact = ele.actualLastActionImpact,
                    expectedNextActionImpact = ele.expectedNextActionImpact,
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
        logger.info(
            "extract.start requestId={} instruction='{}'",
            requestId.take(8),
            PromptBuilder.compactPrompt(instruction, 200)
        )
    }

    private fun logObserveStart(instruction: String, requestId: String) {
        logger.info(
            "observe.start requestId={} instruction='{}'",
            requestId.take(8),
            PromptBuilder.compactPrompt(instruction, 200)
        )
    }

    private fun addHistoryExtract(instruction: String, requestId: String, success: Boolean) {
        val compactPrompt = PromptBuilder.compactPrompt(instruction, 200)
        addToHistory("extract[$requestId] ${if (success) "OK" else "FAIL"} $compactPrompt")
    }

    private fun addHistoryObserve(instruction: String, requestId: String, size: Int, success: Boolean) {
        addToHistory(
            "observe[$requestId] ${if (success) "OK" else "FAIL"} ${
                Strings.compactWhitespaces(
                    instruction,
                    200
                )
            } -> $size elements"
        )
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
            val attemptNo = attempt + 1
            addToRecordHistory("resolve ATTEMPT ${attemptNo}/${config.maxRetries + 1}")
            try {
                val res = doResolveProblem(action, sessionId, startTime, attempt)
                addToRecordHistory("resolve ATTEMPT ${attemptNo} OK")
                return res
            } catch (e: PerceptiveAgentError.TransientError) {
                lastError = e
                logger.error("resolve.transient attempt={} sid={} msg={}", attempt + 1, sessionId.take(8), e.message, e)
                if (attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    addToRecordHistory("resolve RETRY ${attemptNo} cause=Transient delay=${backoffMs}ms msg=${e.message}")
                    delay(backoffMs)
                }
            } catch (e: PerceptiveAgentError.TimeoutError) {
                lastError = e
                logger.error("resolve.timeout attempt={} sid={} msg={}", attempt + 1, sessionId.take(8), e.message, e)
                if (attempt < config.maxRetries) {
                    val baseBackoffMs = config.baseRetryDelayMs
                    addToRecordHistory("resolve RETRY ${attemptNo} cause=Timeout delay=${baseBackoffMs}ms msg=${e.message}")
                    delay(baseBackoffMs)
                }
            } catch (e: Exception) {
                lastError = e
                logger.error("resolve.unexpected attempt={} sid={} msg={}", attempt + 1, sessionId.take(8), e.message, e)
                if (shouldRetryError(e) && attempt < config.maxRetries) {
                    val backoffMs = calculateRetryDelay(attempt)
                    addToRecordHistory("resolve RETRY ${attemptNo} cause=Unexpected delay=${backoffMs}ms msg=${e.message}")
                    delay(backoffMs)
                } else {
                    // Non-retryable error, exit loop
                    break
                }
            }
        }

        addToRecordHistory("resolve FAIL after ${config.maxRetries + 1} attempts: ${lastError?.message}")
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

        val sid = context.sessionId.take(8)
        logger.info(
            "agent.start sid={} step={} url={} instr='{}' attempt={} maxSteps={} maxRetries={}",
            sid,
            context.stepNumber,
            context.targetUrl,
            Strings.compactWhitespaces(overallGoal, 100),
            attempt + 1,
            config.maxSteps,
            config.maxRetries
        )

        // agent general guide
        val systemMsg = PromptBuilder().buildOperatorSystemPrompt()
        var consecutiveNoOps = 0
        var step = 0

        try {
            while (step < config.maxSteps) {
                step++
                val stepStartTime = Instant.now()
                val stepContext = context.copy(stepNumber = step, actionType = "step")

                val messages = AgentMessageList()
                messages.addSystem(systemMsg)

                val driver = requireNotNull(activeDriver)
                val url = driver.url()
                if (url.isBlank() || url == "about:blank") {
                    driver.navigateTo(AppConstants.SEARCH_ENGINE_URL)
                }

                val settleMs = action.domSettleTimeoutMs?.toLong()?.coerceAtLeast(0L) ?: config.domSettleTimeoutMs
                if (settleMs > 0) {
                    // High Priority #3: Improved DOM settle detection
                    pageStateTracker.waitForDOMSettle(settleMs, config.domSettleCheckIntervalMs)
                }

                // Extract nodes each step
                val browserUseState = getBrowserUseState()

                // Medium Priority #10: Detect if page state hasn't changed
                val unchangedCount = pageStateTracker.checkStateChange(browserUseState)
                if (unchangedCount >= 3) {
                    logger.info(
                        "loop.warn sid={} step={} unchangedSteps={}",
                        sid,
                        step,
                        unchangedCount
                    )
                    consecutiveNoOps++
                }

                logger.info(
                    "step.exec sid={} step={}/{} noOps={} dom={}",
                    sid,
                    step,
                    config.maxSteps,
                    consecutiveNoOps,
                    DomDebug.summarize(browserUseState.domState)
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

                messages.add(SimpleMessage("user", promptBuilder.buildOverallGoalMessage(overallGoal), name = "overallGoal"))
                messages.addUser(promptBuilder.buildHistoryMessage(history))
                if (screenshotB64 != null) {
                    messages.addUser("[Current page screenshot provided as base64 image]")
                }

                val stepAction = try {
                    // Use overload supplying extracted elements to avoid re-extraction
                    tta.generate(messages, browserUseState, screenshotB64)
                } catch (e: Exception) {
                    logger.error("action.gen.fail sid={} msg={}", context.sessionId.take(8), e.message, e)
                    consecutiveFailureCounter.incrementAndGet()
                    null
                }

                if (stepAction == null) {
                    consecutiveNoOps++
                    val stop = handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    if (stop) break
                    continue
                }

                // Check for task completion
                if (shouldTerminate(stepAction)) {
                    handleTaskCompletion(stepAction, step, stepContext)
                    break
                }

                val hasModelToolCall = stepAction.toolCall != null
                if (!hasModelToolCall) {
                    consecutiveNoOps++
                    val stop = handleConsecutiveNoOps(consecutiveNoOps, step, stepContext)
                    if (stop) break
                    continue
                }

                // Reset consecutive no-ops counter when we have a valid action
                consecutiveNoOps = 0

                // Execute the tool call with enhanced error handling
                val result = doExecuteToolCall(stepAction, step, stepContext)
                if (result != null) {
                    val observe = result.action.observeElement
                    val record = AgentState(
                        step,
                        action = observe?.toolCall?.method ?: "no-op",
                        description = observe?.description ?: "no-op (consecutive: ${0})",
                        currentPageContentSummary = observe?.currentPageContentSummary,
                        actualLastActionImpact = observe?.actualLastActionImpact,
                        expectedNextActionImpact = observe?.expectedNextActionImpact,
                    )

                    addToHistory(record)
                    updatePerformanceMetrics(step, stepStartTime, true)
                    logger.info("step.done sid={} step={} summary={}", sid, step, result.summary)
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
            logger.info(
                "agent.done sid={} steps={} dur={}",
                sid,
                step,
                executionTime.toString()
            )

            val summary = generateFinalSummary(overallGoal, context)
            val ok = summary.state != ResponseState.OTHER
            return ActResult(success = ok, message = summary.content, action = overallGoal)
        } catch (e: Exception) {
            val executionTime = Duration.between(startTime, Instant.now())
            logger.error(
                "agent.fail sid={} steps={} dur={} err={}",
                sid,
                step,
                executionTime.toString(),
                e.message,
                e
            )
            throw classifyError(e, step)
        }
    }

    // Enhanced helper methods for improved functionality

    override fun toString(): String {
        return history.lastOrNull()?.toString() ?: "(no history)"
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
     * Captures screenshot with retry mechanism
     */
    private suspend fun captureScreenshotWithRetry(context: ExecutionContext): String? {
        return try {
            val screenshot = safeScreenshot(context)
            if (screenshot != null) {
                logger.info("screenshot.ok sid={} step={} size={} ", context.sessionId.take(8), context.stepNumber, screenshot.length)
            } else {
                logger.info("screenshot.null sid={} step={}", context.sessionId.take(8), context.stepNumber)
            }
            screenshot
        } catch (e: Exception) {
            logger.error("screenshot.fail sid={} msg={}", context.sessionId.take(8), e.message, e)
            null
        }
    }

    /**
     * Executes tool call with enhanced error handling and validation
     */
    private suspend fun doExecuteToolCall(action: ActionDescription, step: Int, context: ExecutionContext): ActionExecuteResult? {
        val toolCall = action.toolCall ?: return null

        if (config.enablePreActionValidation && !actionValidator.validateToolCall(toolCall)) {
            logger.info("tool.validate.fail sid={} step={} tool={} args={}",
                context.sessionId.take(8), context.stepNumber, toolCall.method, toolCall.arguments)
            addToHistory("#$step validation-failed ${toolCall.method}")
            return null
        }

        return try {
            logger.info("tool.exec sid={} step={} tool={} args={}",
                context.sessionId.take(8), context.stepNumber, toolCall.method, toolCall.arguments)

            val instructionResult = doExecute(action)
            consecutiveFailureCounter.set(0) // Reset on success

            val summary = "${toolCall.method} executed successfully"
            ActionExecuteResult(action, instructionResult, success = true, summary)
        } catch (e: Exception) {
            val failures = consecutiveFailureCounter.incrementAndGet()
            logger.error("tool.exec.fail sid={} step={} failures={} msg={}",
                context.sessionId.take(8), context.stepNumber, failures, e.message, e)

            if (failures >= 3) {
                throw PerceptiveAgentError.PermanentError("Too many consecutive failures at step $step", e)
            }

            null
        }
    }

    /**
     * Handles consecutive no-op scenarios with intelligent backoff
     */
    private suspend fun handleConsecutiveNoOps(consecutiveNoOps: Int, step: Int, context: ExecutionContext): Boolean {
        addToHistory("#$step no-op (consecutive: $consecutiveNoOps)")
        logger.info("noop sid={} step={} consecutive={}", context.sessionId.take(8), step, consecutiveNoOps)

        if (consecutiveNoOps >= config.consecutiveNoOpLimit) {
            logger.info("noop.stop sid={} step={} limit={}", context.sessionId.take(8), step, config.consecutiveNoOpLimit)
            // Signal caller to stop loop gracefully
            return true
        }

        val delayMs = calculateConsecutiveNoOpDelay(consecutiveNoOps)
        delay(delayMs)
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
    private fun shouldTerminate(action: ActionDescription): Boolean {
        return action.isComplete
    }

    /**
     * Handles task completion
     */
    private fun handleTaskCompletion(action: ActionDescription, step: Int, context: ExecutionContext) {
        logger.info("task.complete sid={} step={} complete={}",
            context.sessionId.take(8), step, action.isComplete)

        addToHistory("#$step complete: taskComplete=${action.isComplete}")
    }

    /**
     * Updates performance metrics
     */
    private fun updatePerformanceMetrics(step: Int, stepStartTime: Instant, success: Boolean) {
        val stepTime = Duration.between(stepStartTime, Instant.now()).toMillis()
        stepExecutionTimes[step] = stepTime
        // Update counters
        performanceMetrics.totalSteps += 1
        if (success) performanceMetrics.successfulActions += 1 else performanceMetrics.failedActions += 1
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
            if (history.size > config.maxHistorySize) {
                val toRemove = history.size - config.maxHistorySize + 10
                _history.subList(0, toRemove).clear()
            }

            // Clear validation cache periodically
            if (validationCache.size > 1000) {
                validationCache.clear()
            }

            logger.info("mem.cleanup sid={} step={}", context.sessionId.take(8), context.stepNumber)
        } catch (e: Exception) {
            logger.error("mem.cleanup.fail sid={} msg={}", context.sessionId.take(8), e.message, e)
        }
    }

    /**
     * history management
     */
    private fun addToHistoryCore(h: AgentState) {
        _history.add(h)
        if (history.size > config.maxHistorySize * 2) {
            // Remove the oldest entries to prevent memory issues
            _history.subList(0, config.maxHistorySize).clear()
        }
    }

    private fun addToHistory(entry: AgentState) = addToHistoryCore(entry)
    private fun addToHistory(entry: String) = addToHistoryCore(AgentState(0, action = entry))

    private fun addToRecordHistory(entry: String) {
        _recordHistory.add(entry)
    }

    /**
     * Gets current URL with error handling
     */
    private suspend fun getCurrentUrl(): String {
        val driver = activeDriver
        return runCatching { driver.currentUrl() }.getOrNull().orEmpty()
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
            logger.error("agent.summary.fail sid={} msg={}", context.sessionId.take(8), e.message, e)
            ModelResponse("Failed to generate summary: ${e.message}", ResponseState.OTHER)
        }
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
                logger.info(
                    "url.block.scheme sid={} scheme={} url={}",
                    uuid.toString().take(8),
                    scheme ?: "null",
                    url.take(50)
                )
                return false
            }

            // Additional safety checks
            val host = uri.host?.lowercase() ?: return false

            // Medium Priority #12: Configurable localhost blocking
            if (!config.allowLocalhost) {
                val dangerousPatterns = listOf("localhost", "127.0.0.1", "0.0.0.0", "::1")
                if (dangerousPatterns.any { host.contains(it) }) {
                    logger.info(
                        "url.block.localhost sid={} host={} reason=localhost_blocked",
                        uuid.toString().take(8),
                        host
                    )
                    return false
                }
            }

            // Medium Priority #12: Validate port with configurable whitelist
            val port = uri.port
            if (port != -1 && !config.allowedPorts.contains(port)) {
                logger.info("url.block.port sid={} port={} host={} allowedPorts={}",
                    uuid.toString().take(8), port, host, config.allowedPorts)
                return false
            }

            // Cap length of selector in actions (if we pass it) — handled elsewhere
            true
        }.getOrDefault(false)
    }

    /**
     * Enhanced screenshot capture with comprehensive error handling
     */
    private suspend fun safeScreenshot(context: ExecutionContext): String? {
        return runCatching {
            slogger.info("Attempting to capture screenshot", context)
            val driver = requireNotNull(activeDriver)
            val screenshot = driver.captureScreenshot()
            if (screenshot != null) {
                slogger.info("Screenshot captured successfully", context, mapOf("size" to screenshot.length))
            } else {
                slogger.info("Screenshot capture returned null", context)
            }
            screenshot
        }.onFailure { e ->
            slogger.logError("Screenshot capture failed", e, context.sessionId)
        }.getOrNull()
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
            slogger.info("Persisting execution transcript", context, mapOf("path" to log.toString()))

            val sb = StringBuilder()
            sb.appendLine("SESSION_ID: ${uuid}")
            sb.appendLine("TIMESTAMP: ${Instant.now()}")
            sb.appendLine("INSTRUCTION: $instruction")
            sb.appendLine("RESPONSE_STATE: ${finalResp.state}")
            sb.appendLine("EXECUTION_HISTORY:")
            history.forEach { sb.appendLine(it) }
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
            slogger.info(
                "Transcript persisted successfully", context,
                mapOf("lines" to history.size + 10, "path" to log.toString())
            )
        }.onFailure { e ->
            slogger.logError("Failed to persist transcript", e, context.sessionId)
        }
    }

    private fun buildSummaryPrompt(goal: String): Pair<String, String> {
        val system = "你是总结助理，请基于执行轨迹对原始目标进行总结，输出 JSON。"
        val user = buildString {
            appendLine("原始目标：$goal")
            appendLine("执行轨迹(按序)：")
            history.forEach { appendLine(it) }
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
            slogger.info("Generating final summary", context)

            val response = tta.chatModel.callUmSm(user, system)

            slogger.info(
                "Summary generated successfully", context, mapOf(
                    "responseLength" to response.content.length,
                    "responseState" to response.state
                )
            )

            response
        } catch (e: Exception) {
            slogger.logError("Summary generation failed", e, context.sessionId)
            ModelResponse(
                "Failed to generate summary: ${e.message}",
                ResponseState.OTHER
            )
        }
    }
}
