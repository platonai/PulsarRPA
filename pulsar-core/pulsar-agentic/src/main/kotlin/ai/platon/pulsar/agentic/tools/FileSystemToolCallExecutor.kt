package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.common.FileSystem
import ai.platon.pulsar.agentic.tools.ToolCallExecutor.Companion.norm
import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall

class FileSystemToolCallExecutor {
    private val logger = getLogger(this)

    suspend fun execute(expression: String, fs: FileSystem): Any? {
        return try {
            val r = execute0(expression, fs)
            when (r) {
                is Unit -> null
                else -> r
            }
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            null
        }
    }

    private suspend fun execute0(expression: String, fs: FileSystem): Any? {
        // Extract function name and arguments from the expression string
        val (objectName, functionName, args) = SimpleKotlinParser().parseFunctionExpression(expression) ?: return null

        return doExecute(objectName, functionName, args, fs)
    }

    /**
     * Extract function name and arguments from the expression string
     * */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, fs: FileSystem
    ): Any? {
        require(objectName == "browser") { "Object must be a Browser" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }

        // Handle browser-level expressions
        when (functionName) {
//            "write" -> {
//                fs.writeString()
//            }
//            "read" -> {
//                fs.readString()
//            }
//            "replace" -> {
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
                "write" -> arguments["tabId"]?.let { "fs.write(${it.norm()})" }
                else -> null
            }
        }
    }
}
