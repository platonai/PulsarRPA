package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.agentic.BrowserAgentActor
import ai.platon.pulsar.browser.driver.chrome.dom.DomService
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.browser.driver.chrome.dom.model.TabState
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.time.Instant
import java.util.*

class AgentStateManager(
    val agent: BrowserAgentActor,
    val domService: DomService,
    val pageStateTracker: PageStateTracker,
) {
    private val _stateHistory = mutableListOf<AgentState>()
    private val _processTrace = mutableListOf<ProcessTrace>()
    private val config get() = agent.config

    val driver: WebDriver get() = agent.session.getOrCreateBoundDriver()
    val stateHistory: List<AgentState> get() = _stateHistory
    val processTrace: List<ProcessTrace> get() = _processTrace

    val auxLogDir: Path get() = AppPaths.detectAuxiliaryLogDir().resolve("agent")

    suspend fun buildBaseExecutionContext(
        action: ActionOptions,
        event: String,
        baseContext: ExecutionContext? = null
    ): ExecutionContext {
        val context = buildExecutionContext(action.action, 0, event, baseContext = baseContext)
        action.setContext(context)
        return context
    }

    suspend fun buildInitExecutionContext(
        action: ActionOptions,
        event: String,
        baseContext: ExecutionContext? = null
    ): ExecutionContext {
        val context = buildExecutionContext(action.action, 1, event, baseContext = baseContext)
        action.setContext(context)
        return context
    }

    suspend fun buildInitExecutionContext(
        options: ObserveOptions,
        event: String,
        baseContext: ExecutionContext? = null
    ): ExecutionContext {
        val instruction = options.instruction ?: ""
        val context = buildExecutionContext(instruction, 1, event, baseContext = baseContext)
        options.setContext(context)
        return context
    }

    suspend fun buildExecutionContext(
        /**
         * The user's instruction
         * */
        instruction: String,
        step: Int,
        event: String,
        /**
         * A base context that the new context can inherit from
         * */
        baseContext: ExecutionContext? = null
    ): ExecutionContext {
        // val step = (baseContext?.step ?: -1) + 1

        val sessionId = baseContext?.sessionId ?: UUID.randomUUID().toString()
        val prevAgentState = baseContext?.agentState
        val currentAgentState = getAgentState(instruction, step, prevAgentState)

        if (baseContext != null) {
            return ExecutionContext(
                instruction = baseContext.instruction,
                step = step,
                actionType = event,
                targetUrl = prevAgentState?.browserUseState?.browserState?.url,
                sessionId = baseContext.sessionId,
                timestamp = Instant.now(),
                agentState = currentAgentState,
                config = baseContext.config,
                stateHistory = _stateHistory,
                baseContext = WeakReference(baseContext)
            )
        }

        return ExecutionContext(
            instruction = instruction,
            step = step,
            actionType = "init",
            sessionId = sessionId,
            agentState = currentAgentState,
            config = config,
            stateHistory = _stateHistory,
        )
    }

    suspend fun getAgentState(
        instruction: String, step: Int, prevAgentState: AgentState? = null
    ): AgentState {
        pageStateTracker.waitForDOMSettle()

        val browserUseState = getBrowserUseState()
        val agentState = AgentState(
            instruction = instruction,
            step = step,
            browserUseState = browserUseState,
            prevState = prevAgentState
        )
        return agentState
    }

    suspend fun getBrowserUseState(): BrowserUseState {
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
        // Add timeout to prevent hanging on DOM snapshot operations
        return withTimeout(30_000) {
            val baseState = domService.getBrowserUseState(snapshotOptions = snapshotOptions)
            injectTabsInfo(baseState)
        }
    }

    fun updateAgentState(context: ExecutionContext, detailedActResult: DetailedActResult) {
        val observeElement = requireNotNull(detailedActResult.actionDescription.observeElement)
        val toolCall = requireNotNull(detailedActResult.actionDescription.toolCall)
        val toolCallResult = detailedActResult.toolCallResult
        // additional message appended to description
        val description = detailedActResult.description

        updateAgentState(context, observeElement, toolCall, toolCallResult, description)
    }

    fun updateAgentState(
        context: ExecutionContext,
        observeElement: ObserveElement,
        toolCall: ToolCall,
        toolCallResult: ToolCallResult? = null,
        description: String? = null,
        exception: Exception? = null
    ) {
        val agentState = requireNotNull(context.agentState)
        val computedStep = agentState.step.takeIf { it > 0 } ?: ((stateHistory.lastOrNull()?.step ?: 0) + 1)

        agentState.apply {
            step = computedStep
            domain = toolCall.domain
            method = toolCall.method
            this.description = description
            this.exception = exception
            screenshotContentSummary = observeElement.screenshotContentSummary
            currentPageContentSummary = observeElement.currentPageContentSummary
            evaluationPreviousGoal = observeElement.evaluationPreviousGoal
            nextGoal = observeElement.nextGoal
            this.toolCallResult = toolCallResult
        }
    }

    /**
     * Make sure add to history at every end of step
     * */
    fun addToHistory(state: AgentState) {
//        val trace = ProcessTrace(
//            state.step,
//            state.method,
//            agentState = state.toString()
//        )

        synchronized(this) {
            _stateHistory.add(state)
            if (_stateHistory.size > config.maxHistorySize * 2) {
                // Keep the latest maxHistorySize entries
                val remaining = _stateHistory.takeLast(config.maxHistorySize)
                _stateHistory.clear()
                _stateHistory.addAll(remaining)
            }
            // _processTrace.add(trace)
        }
    }

    fun addTrace(state: AgentState?, items: Map<String, Any?> = emptyMap(), event: String? = null, message: String? = null) {
        val step = state?.step ?: 0
        val msg = message ?: state?.toString()

        val isComplete = state?.isComplete == true
        val trace = if (isComplete) {
            ProcessTrace(
                step = step,
                event = event ?: state.event,
                isComplete = true,
                agentState = state.toString(),
                items = items,
                message = msg
            )
        } else {
            ProcessTrace(
                step = step,
                event = event ?: state?.event,
                method = state?.method,
                isComplete = false,
                agentState = state.toString(),
                expression = state?.toolCallResult?.actionDescription?.pseudoExpression,
                tcEvalResult = state?.toolCallResult?.evaluate?.value,
                items = items,
                message = msg
            )
        }

        _processTrace.add(trace)
    }

    fun writeProcessTrace() {
        val path = auxLogDir.resolve("processTrace_${AppPaths.fromNow()}.log")
        MessageWriter.writeOnce(path, processTrace)
    }

    fun clearUpHistory(toRemove: Int) {
        synchronized(this) {
            if (toRemove > 0) {
                val safeToRemove = toRemove.coerceAtMost(_stateHistory.size)
                if (safeToRemove > 0) {
                    val remaining = _stateHistory.drop(safeToRemove)
                    _stateHistory.clear()
                    _stateHistory.addAll(remaining)
                }
            }
        }
    }

    fun clearHistory() {
        synchronized(this) {
            _stateHistory.clear()
        }
    }

    /**
     * Remove the last history entry if its step is >= provided step. Used for rollback on errors.
     */
    fun removeLastIfStep(step: Int) {
        synchronized(this) {
            val last = _stateHistory.lastOrNull()
            if (last != null && last.step >= step) {
                _stateHistory.removeAt(_stateHistory.size - 1)
            }
        }
    }

    /**
     * Inject tabs information into BrowserUseState.
     * Collects all tabs from the current browser and marks the active tab.
     */
    private suspend fun injectTabsInfo(baseState: BrowserUseState): BrowserUseState {
        val currentDriver = this.driver
        val browser = currentDriver.browser

        // fetch all drivers
        browser.listDrivers()
        val tabs = browser.drivers.map { (tabId, driver) ->
            val url = try {
                driver.currentUrl()
            } catch (_: Exception) {
                "about:blank"
            }
            val title = try {
                driver.evaluate("document.title").toString().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
            TabState(
                id = tabId, driverId = driver.id, url = url, title = title, active = (driver == currentDriver)
            )
        }

        val activeTabId = browser.drivers.entries.find { it.value == currentDriver }?.key

        val enhancedBrowserState = baseState.browserState.copy(
            tabs = tabs, activeTabId = activeTabId
        )

        return BrowserUseState(
            browserState = enhancedBrowserState, domState = baseState.domState
        )
    }
}
