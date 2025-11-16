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

        // Execute the tool call with enhanced error handling
        val actResult = act(action)

        if (shouldTerminate(actResult)) {
            onTaskCompletion(actResult, context)
            return StepProcessingResult(context, consecutiveNoOps, true)
        }

        if (actResult.success) {
            stateManager.updateAgentState(context.agentState, actResult.detail!!)
            updateTodo(context, actResult)
            updatePerformanceMetrics(context.step, context.timestamp, true)
            val preview = actResult.detail?.toolCallResult?.evaluate?.preview
            logger.info("🏁 step.done sid={} step={} result={}", context.sid, context.step, preview)
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
