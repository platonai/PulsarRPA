package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.ToolCall

class AgentToolCallExecutor: AbstractToolCallExecutor() {
    private val logger = getLogger(this)

    /**
     * Extract function name and arguments from the expression string
     * */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "browser") { "Object must be a Browser" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is PerceptiveAgent) { "Target must be a PerceptiveAgent" }

        val arg0 = args["0"]?.toString()
        val arg1 = args["1"]?.toString()
        val arg2 = args["2"]?.toString()
        val arg3 = args["3"]?.toString()

//        when (functionName) {
//            "act" -> agent.act(objectName, args)
//            "observe" -> agent.observe(objectName, args)
//            "extract" -> agent.extract(objectName, args)
//        }

        return null
    }

    companion object {

        fun toolCallToExpression(tc: ToolCall): String? {
            ActionValidator().validateToolCall(tc)

            val arguments = tc.arguments
            return when (tc.method) {
//                "act" -> arguments["todo"]?.let { "agent.act(${it.norm()})" }
//                "observe" -> arguments["todo"]?.let { "agent.act(${it.norm()})" }
//                "extract" -> arguments["todo"]?.let { "agent.act(${it.norm()})" }
                else -> null
            }
        }
    }
}
