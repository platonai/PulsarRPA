package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.agentic.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.DetailedActResult
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.browser.driver.chrome.dom.model.TabState
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ProcessTrace
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.ai.ToolCallResult
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2

class AgentStateManager(
    val agent: BrowserPerceptiveAgent,
    val pageStateTracker: PageStateTracker,
) {
    private val _stateHistory = mutableListOf<AgentState>()
    private val _processTrace = mutableListOf<ProcessTrace>()
    private val config get() = agent.config
    private val domService get() = agent.domService

    val stateHistory: List<AgentState> get() = _stateHistory
    val processTrace: List<ProcessTrace> get() = _processTrace

    suspend fun buildInitExecutionContext(options: ActionOptions): ExecutionContext {
        return buildExecutionContext(options.action, agentState = options.agentState)
    }

    suspend fun buildExecutionContext(
        /**
         * The user's instruction
         * */
        instruction: String,
        actionType: String = "",
        step: Int = 0,
        /**
         * The current agent state
         * */
        agentState: AgentState? = null,
        /**
         * A base context that the new context can inherit from
         * */
        baseContext: ExecutionContext? = null
    ): ExecutionContext {
        val sessionId = baseContext?.sessionId ?: UUID.randomUUID().toString()
        val prevAgentState = baseContext?.agentState
        val currentAgentState = agentState ?: getAgentState(instruction, step, prevAgentState)

        val bc = baseContext ?: ExecutionContext(
            instruction = instruction,
            step = 0,
            actionType = "init",
            sessionId = sessionId,
            agentState = currentAgentState,
            config = config,
        )

        return ExecutionContext(
            instruction = bc.instruction,
            step = step,
            actionType = actionType,
            targetUrl = prevAgentState?.browserUseState?.browserState?.url,
            sessionId = bc.sessionId,
            timestamp = Instant.now(),
            prevAgentState = bc.agentState,
            agentState = currentAgentState,
            config = bc.config,
        )
    }

    suspend fun getAgentState(
        instruction: String, step: Int = 0, prevAgentState: AgentState? = null
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

    fun updateAgentState(agentState: AgentState, detailedActResult: DetailedActResult) {
        val observeElement = detailedActResult.actionDescription.observeElement
        val toolCall = detailedActResult.actionDescription.toolCall
        val toolCallResult = detailedActResult.toolCallResult
        val message = detailedActResult.summary
        val success = detailedActResult.success

        updateAgentState(agentState, observeElement, toolCall, toolCallResult, message, success = success)
    }

    fun updateAgentState(
        agentState: AgentState,
        observeElement: ObserveElement? = null,
        toolCall: ToolCall? = null,
        toolCallResult: ToolCallResult? = null,
        message: String? = null,
        success: Boolean = true,
    ) {
        val computedStep = agentState.step.takeIf { it > 0 } ?: ((stateHistory.lastOrNull()?.step ?: 0) + 1)
        val descPrefix = if (success) "OK" else "FAIL"
        val descMsg = buildString {
            append(descPrefix)
            if (!message.isNullOrBlank()) {
                append(": ")
                append(Strings.compactLog(message, 200))
            }
        }

        agentState.apply {
            step = computedStep
            domain = toolCall?.domain
            method = toolCall?.method
            description = descMsg
            screenshotContentSummary = observeElement?.screenshotContentSummary
            currentPageContentSummary = observeElement?.currentPageContentSummary
            evaluationPreviousGoal = observeElement?.evaluationPreviousGoal
            nextGoal = observeElement?.nextGoal
            this.toolCallResult = toolCallResult
        }

        addToHistory(agentState)
    }

    fun addToHistory(h: AgentState) {
        val items = mapOf(
            "action" to h.method,
            "expression" to h.toolCallResult?.expression,
            "tcEvalResult" to h.toolCallResult?.evaluate?.value
        ).filterValues { it != null }
        val trace = ProcessTrace(step = h.step, items = items, message = h.toString())

        synchronized(this) {
            _stateHistory.add(h)
            if (_stateHistory.size > config.maxHistorySize * 2) {
                // Keep the latest maxHistorySize entries
                val remaining = _stateHistory.takeLast(config.maxHistorySize)
                _stateHistory.clear()
                _stateHistory.addAll(remaining)
            }
            _processTrace.add(trace)
        }
    }

    fun trace(state: AgentState?, items: Map<String, Any?>, message: String? = null) {
        val items2 = if (state != null) {
            mapOf(
                "action" to state.method,
                "expression" to state.toolCallResult?.expression,
                "tcEvalResult" to state.toolCallResult?.evaluate?.value
            ).filterValues { it != null }
        } else emptyMap()

        val items3 = items + items2
        val step = state?.step ?: 0
        val msg = message ?: state?.toString()
        val trace = ProcessTrace(step = step, items = items3, message = msg)
        _processTrace.add(trace)
    }

    fun trace(message: String) {
        trace(stateHistory.lastOrNull(), emptyMap(), message)
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
        val currentDriver = agent.activeDriver
        val browser = agent.activeDriver.browser

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
