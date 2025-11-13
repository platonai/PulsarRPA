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

    override suspend fun processSingleStep(
        action: ActionOptions,
        ctxIn: ExecutionContext,
        noOpsIn: Int
    ): StepProcessingResult {
        return processSingleStep1(action, ctxIn, noOpsIn)

        // return super.processSingleStep(action, ctxIn, noOpsIn)
    }

    private suspend fun processSingleStep1(
        action: ActionOptions,
        ctxIn: ExecutionContext,
        noOpsIn: Int
    ): StepProcessingResult {
        val context = ctxIn
        var consecutiveNoOps = noOpsIn
        val step = context.step
        val nextStep = step + 1

        prepareStep(action, ctxIn, nextStep)

        // Execute the tool call with enhanced error handling
        val actResult = act(action)

        val actionDescription = actResult.detail?.actionDescription
        if (shouldTerminate(actionDescription)) {
            onTaskCompletion(actionDescription!!, context)
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
            updatePerformanceMetrics(step, context.timestamp, false)
            if (stop) return StepProcessingResult(context, consecutiveNoOps, true)
        }

        delay(calculateAdaptiveDelay())
        return StepProcessingResult(context, consecutiveNoOps, false)
    }

    private suspend fun processSingleStep2(
        action: ActionOptions,
        ctxIn: ExecutionContext,
        noOpsIn: Int
    ): StepProcessingResult {
        val context = ctxIn
        var consecutiveNoOps = noOpsIn
        val step = context.step
        val nextStep = step + 1

        prepareStep(action, ctxIn, nextStep)

        // Generate the action for this step
        val observeResults = observe(context.createObserveActOptions(resolve = true))

        observeResults.forEach { result ->
            val actionDescription = result.actionDescription ?: return@forEach

            consecutiveNoOps = 0

            // Check for task completion
            if (shouldTerminate(actionDescription)) {
                onTaskCompletion(actionDescription, context)
                return@forEach
            }

            if (actionDescription.toolCall == null) {
                consecutiveNoOps++
                val stop = handleConsecutiveNoOps(consecutiveNoOps, context)
                if (stop) return@forEach
                return@forEach
            }

            // Reset consecutive no-ops counter when we have a valid action
            consecutiveNoOps = 0

            //**
            // II - Execute Tool Call
            //**

            // Execute the tool call with enhanced error handling
            val actResult = act(result)

            if (actResult.success) {
                // updateAgentState(context.agentState, actResult)
                stateManager.updateAgentState(context.agentState, actResult.detail!!)
                updateTodo(context, actResult)
                updatePerformanceMetrics(context.step, context.timestamp, true)
                val preview = actResult.detail?.toolCallResult?.evaluate?.preview
                logger.info("🏁 step.done sid={} step={} result={}", context.sid, context.step, preview)
            } else {
                consecutiveNoOps++
                val stop = handleConsecutiveNoOps(consecutiveNoOps, context)
                updatePerformanceMetrics(step, context.timestamp, false)
                if (stop) return StepProcessingResult(context, consecutiveNoOps, true)
            }
        }

        delay(calculateAdaptiveDelay())
        return StepProcessingResult(context, consecutiveNoOps, false)
    }
}
