package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.tools.executors.AgentToolExecutor
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall

class SystemToolExecutor: AbstractToolExecutor() {
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
        require(objectName == "system") { "Object must be an System" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }

        val arg0 = args["0"]?.toString()

        return when (functionName) {
            // Minimal supported agent API (string-based overloads)
            "help" -> {

            }
            else -> {
                logger.warn("Unsupported system method: {}({})", functionName, args)
                null
            }
        }
    }

    companion object {
        fun toExpression(tc: ToolCall): String? {
            val arguments = tc.arguments
            return when (tc.method) {
                "help" -> "system.help()"
                else -> null
            }
        }
    }
}
