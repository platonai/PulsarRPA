package ai.platon.pulsar.agentic

import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ActionOptions
import kotlinx.coroutines.delay

/**
 * A reasoning agent that uses [observe -> act -> observe -> act -> ...] pattern to resolve browser use problems.
 * */
class ObserveActBrowserAgent constructor(
    session: AgenticSession, maxSteps: Int = 100, config: AgentConfig = AgentConfig(maxSteps = maxSteps)
) : BrowserPerceptiveAgent(session, maxSteps, config) {
    private val logger = getLogger(this)

    override suspend fun step(action: ActionOptions, ctxIn: ExecutionContext, noOpsIn: Int): StepProcessingResult {
        var consecutiveNoOps = noOpsIn

        val context = prepareStep(action, ctxIn, consecutiveNoOps)
        require(context.step == ctxIn.step + 1) { "Required: context.step == ctxIn.step + 1" }
        require(context.agentState.prevState == context.baseContext.get()?.agentState) {
            "Required: context.agentState.prevState == context.baseContext.get()?.agentState"
        }

        // Execute the tool call with enhanced error handling
        val actResult = act(action)

        if (shouldTerminate(actResult)) {
            onTaskCompletion(actResult, context)
            return StepProcessingResult(context, consecutiveNoOps, true)
        }

        if (actResult.success) {
            // stateManager.updateAgentState(context, actResult.detail!!)
            updateTodo(context, actResult)
            updatePerformanceMetrics(context.step, context.timestamp, true)

            val tcResult = actResult.detail?.toolCallResult
            val method = actResult.detail?.actionDescription?.toolCall?.method
            val preview = tcResult?.evaluate?.preview
            logger.info("🏁 step.done sid={} step={} method={} result={}", context.sid, context.step, method, preview)
        } else {
            consecutiveNoOps++
            val stop = handleConsecutiveNoOps(consecutiveNoOps, context)
            updatePerformanceMetrics(context.step, context.timestamp, false)
            if (stop) return StepProcessingResult(context, consecutiveNoOps, true)
        }

        delay(calculateAdaptiveDelay())
        return StepProcessingResult(context, consecutiveNoOps, false)
    }
}
