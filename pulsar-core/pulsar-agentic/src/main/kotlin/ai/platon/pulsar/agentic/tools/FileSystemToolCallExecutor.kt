package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.common.FileSystem
import ai.platon.pulsar.agentic.tools.ToolCallExecutor.Companion.norm
import ai.platon.pulsar.skeleton.ai.ToolCall

class FileSystemToolCallExecutor : AbstractToolCallExecutor() {
    /**
     * Extract function name and arguments from the expression string
     * */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "browser") { "Object must be a Browser" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is FileSystem) { "Target must be a FileSystem" }

        val arg0 = args["0"]?.toString()
        val arg1 = args["1"]?.toString()
        val arg2 = args["2"]?.toString()
        val arg3 = args["3"]?.toString()
        // Handle browser-level expressions
        when (functionName) {
//            "writeString" -> {
//                fs.writeString()
//            }
//            "readString" -> {
//                fs.readString()
//            }
//            "replaceContent" -> {
//                fs.replaceContent()
//            }
        }

        return null
    }

    companion object {

        fun toolCallToExpression(tc: ToolCall): String? {
            ActionValidator().validateToolCall(tc)

            val arguments = tc.arguments
            return when (tc.method) {
                // Filesystem-level operations
                "writeString" -> arguments["todo"]?.let { "fs.writeString(${it.norm()})" }
                "readString" -> arguments["todo"]?.let { "fs.readString(${it.norm()})" }
                "replaceContent" -> arguments["todo"]?.let { "fs.replaceContent(${it.norm()})" }
                else -> null
            }
        }
    }
}
