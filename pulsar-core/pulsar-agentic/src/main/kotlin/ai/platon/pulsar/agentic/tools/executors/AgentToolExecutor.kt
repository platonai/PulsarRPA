package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.tools.ActionValidator
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor.Companion.norm
import ai.platon.pulsar.agentic.tools.executors.SystemToolExecutor
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.ToolCall

class AgentToolExecutor: AbstractToolExecutor() {
    private val logger = getLogger(this)

    @Throws(IllegalArgumentException::class)
    override suspend fun toExpression(tc: ToolCall): String {
        return Companion.toExpression(tc) ?: throw IllegalArgumentException("Unknown Tool call $tc")
    }

    /**
     * Execute agent.* expressions against a PerceptiveAgent target.
     */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "agent") { "Object must be an Agent" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is PerceptiveAgent) { "Target must be a PerceptiveAgent" }

        val agent = target
        val arg0 = args["0"]?.toString()

        return when (functionName) {
            // Minimal supported agent API (string-based overloads)
            "act" -> if (!arg0.isNullOrBlank()) agent.act(arg0) else null
            "observe" -> if (!arg0.isNullOrBlank()) agent.observe(arg0) else emptyList<Any>()
            "extract" -> if (!arg0.isNullOrBlank()) agent.extract(arg0) else null
            "resolve" -> if (!arg0.isNullOrBlank()) agent.resolve(arg0) else null
            // Signal completion; no operation on the agent itself
            "done" -> true
            else -> {
                logger.warn("Unsupported agent method: {}({})", functionName, args)
                null
            }
        }
    }

    companion object {

        fun toExpression(tc: ToolCall): String? {
            ActionValidator().validateToolCall(tc)

            val arguments = tc.arguments
            return when (tc.method) {
                "act" -> arguments["action"]?.let { "agent.act(${it.norm()})" }
                    ?: arguments["0"]?.let { "agent.act(${it.norm()})" }
                "observe" -> arguments["instruction"]?.let { "agent.observe(${it.norm()})" }
                    ?: arguments["0"]?.let { "agent.observe(${it.norm()})" }
                "extract" -> arguments["instruction"]?.let { "agent.extract(${it.norm()})" }
                    ?: arguments["0"]?.let { "agent.extract(${it.norm()})" }
                "resolve" -> arguments["action"]?.let { "agent.resolve(${it.norm()})" }
                    ?: arguments["0"]?.let { "agent.resolve(${it.norm()})" }
                "done" -> "agent.done()"
                else -> null
            }
        }
    }
}
