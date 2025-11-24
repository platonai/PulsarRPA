package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.agentic.ActionOptions
import ai.platon.pulsar.agentic.AgentHistory
import ai.platon.pulsar.agentic.AgentState
import ai.platon.pulsar.agentic.BrowserAgentActor
import ai.platon.pulsar.agentic.DetailedActResult
import ai.platon.pulsar.agentic.ObserveElement
import ai.platon.pulsar.agentic.ObserveOptions
import ai.platon.pulsar.agentic.ProcessTrace
import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.agentic.ToolCallResult
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.browser.driver.chrome.dom.model.TabState
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.time.Instant
import java.util.*

class AgentStateManager(
    val agent: BrowserAgentActor,
    val pageStateTracker: PageStateTracker,
) {
    private val logger = getLogger(this)
    // for non-logback logs
    val auxLogDir: Path get() = AppPaths.detectAuxiliaryLogDir().resolve("agent")

    private val _stateHistory = AgentHistory()
    private val _processTrace = mutableListOf<ProcessTrace>()
    private val config get() = agent.config

    // should be the first in context list
    private lateinit var _baseContext: ExecutionContext
    // should be the last in context list
    private var _activeContext: ExecutionContext? = null
    private val contexts: MutableList<ExecutionContext> = mutableListOf()

    val driver get() = agent.activeDriver as PulsarWebDriver
    val stateHistory: AgentHistory get() = _stateHistory
    val processTrace: List<ProcessTrace> get() = _processTrace

    suspend fun getOrCreateActiveContext(action: ActionOptions, event: String): ExecutionContext {
        if (_activeContext == null) {
            _baseContext = buildInitExecutionContext(action, event)
            setActiveContext(_baseContext)
        } else if (action.multiAct) {
            require(!action.fromResolve)
            val lastActiveContext = getActiveContext()
            val step = lastActiveContext.step + 1
            val context = buildExecutionContext(action.action, step, event = "act-$step",
                baseContext = lastActiveContext)
            setActiveContext(context)
        }

        return _activeContext!!
    }

    suspend fun getOrCreateActiveContext(options: ObserveOptions): ExecutionContext {
        if (_activeContext == null) {
//            val instruction = promptBuilder.initObserveUserInstruction(options.instruction).instruction?.content
//            baseContext = buildInitExecutionContext(options.copy(instruction = instruction), "observe")
            _baseContext = buildInitExecutionContext(options, "observe")
            setActiveContext(_baseContext)
        }
        return _activeContext!!
    }

    fun getActiveContext(): ExecutionContext {
        val context = requireNotNull(_activeContext) { "Actor not initialized, call act(action: ActionOptions) first!" }
        require(context == contexts.last()) { "Active context should be the last context in the list. Context list size: ${contexts.size}" }
        return context
    }

    fun setActiveContext(context: ExecutionContext) {
        _activeContext = context
        if (contexts.lastOrNull() == context) {
            logger.warn("Context has been already added | sid=${context.sid}")
            return
        }
        contexts.add(context)
    }

    suspend fun buildBaseExecutionContext(action: ActionOptions, event: String): ExecutionContext {
        val context = buildExecutionContext(action.action, 0, event)
        // action.setContext(context)
        _baseContext = context
        return context
    }

    suspend fun buildInitExecutionContext(action: ActionOptions, event: String): ExecutionContext {
        val context = buildExecutionContext(action.action, 1, event)
        // action.setContext(context)
        return context
    }

    suspend fun buildInitExecutionContext(
        options: ObserveOptions,
        event: String,
        baseContext: ExecutionContext? = null
    ): ExecutionContext {
        val instruction = options.instruction ?: ""
        val context = buildExecutionContext(instruction, 1, event, baseContext = baseContext)
        // options.setContext(context)
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
        val context = buildExecutionContext0(instruction, step, event, baseContext = baseContext)
        return context
    }

    suspend fun buildIndependentExecutionContext(
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
        val context = buildExecutionContext0(instruction, step, event, baseContext = baseContext)
        return context
    }

    private suspend fun buildExecutionContext0(
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
                event = event,
                targetUrl = prevAgentState?.browserUseState?.browserState?.url,
                sessionId = baseContext.sessionId,
                stepStartTime = Instant.now(),
                agentState = currentAgentState,
                config = baseContext.config,
                stateHistory = _stateHistory,
                baseContext = WeakReference(baseContext)
            )
        }

        return ExecutionContext(
            instruction = instruction,
            step = step,
            event = "init",
            sessionId = sessionId,
            agentState = currentAgentState,
            config = config,
            stateHistory = _stateHistory,
        )
    }

    suspend fun getAgentState(instruction: String, step: Int, prevAgentState: AgentState? = null): AgentState {
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

    suspend fun syncBrowserUseState(context: ExecutionContext): BrowserUseState {
        val browserUseState = getBrowserUseState()
        context.agentState.browserUseState = browserUseState
        return browserUseState
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
        val computedStep = agentState.step.takeIf { it > 0 } ?: ((stateHistory.states.lastOrNull()?.step ?: 0) + 1)

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
            thinking = observeElement.thinking

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

        val history = _stateHistory.states
        synchronized(this) {
            history.add(state)
            if (history.size > config.maxHistorySize * 2) {
                // Keep the latest maxHistorySize entries
                val remaining = history.takeLast(config.maxHistorySize)
                history.clear()
                history.addAll(remaining)
            }
            // _processTrace.add(trace)
        }
    }

    fun addTrace(
        state: AgentState?, items: Map<String, Any?> = emptyMap(), event: String? = null, message: String? = null
    ) {
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
        val path = auxLogDir.resolve("processTrace").resolve("processTrace_${AppPaths.fromNow()}.log")
        MessageWriter.writeOnce(path, processTrace.joinToString("\n") { """🚩$it""" })
    }

    fun clearUpHistory(toRemove: Int) {
        synchronized(this) {
            if (toRemove > 0) {
                val history = _stateHistory.states
                val safeToRemove = toRemove.coerceAtMost(history.size)
                if (safeToRemove > 0) {
                    val remaining = history.drop(safeToRemove)
                    history.clear()
                    history.addAll(remaining)
                }
            }
        }
    }

    fun clearHistory() {
        synchronized(this) {
            _stateHistory.states.clear()
        }
    }

    /**
     * Remove the last history entry if its step is >= provided step. Used for rollback on errors.
     */
    fun removeLastIfStep(step: Int) {
        synchronized(this) {
            val history = _stateHistory.states
            val last = history.lastOrNull()
            if (last != null && last.step >= step) {
                history.removeAt(history.size - 1)
            }
        }
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
        // Add timeout to prevent hanging on DOM snapshot operations
        return withTimeout(30_000) {
            val baseState = driver.domService.getBrowserUseState(snapshotOptions = snapshotOptions)
            injectTabsInfo(baseState)
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
