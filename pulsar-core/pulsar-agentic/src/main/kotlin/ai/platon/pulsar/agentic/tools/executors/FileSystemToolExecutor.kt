package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.agentic.tools.ActionValidator
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor.Companion.norm
import ai.platon.pulsar.skeleton.ai.ToolCall
import kotlin.reflect.KClass

class FileSystemToolExecutor : AbstractToolExecutor() {

    override val domain = "fs"

    override val targetClass: KClass<*> = AgentFileSystem::class

    @Throws(IllegalArgumentException::class)
    override fun toExpression(tc: ToolCall): String {
        return Companion.toExpression(tc)
    }

    /**
     * Execute fs.* expressions against a FileSystem target using named args.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any {
        require(objectName == "fs") { "Object must be fs" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is AgentFileSystem) { "Target must be a FileSystem" }

        val fs = target

        return when (functionName) {
            // fs.writeString(filename: String, content: String)
            "writeString" -> {
                validateArgs(args, allowed = setOf("filename", "content"), required = setOf("filename"), functionName)
                fs.writeString(
                    paramString(args, "filename", functionName)!!,
                    paramString(args, "content", functionName, required = false, default = "") ?: ""
                )
            }
            // fs.readString(filename: String [, external: Boolean])
            "readString" -> {
                validateArgs(args, allowed = setOf("filename", "external"), required = setOf("filename"), functionName)
                fs.readString(
                    paramString(args, "filename", functionName)!!,
                    paramBool(args, "external", functionName, required = false, default = false) ?: false
                )
            }
            // fs.replaceContent(filename: String, oldStr: String, newStr: String)
            "replaceContent" -> {
                validateArgs(args, allowed = setOf("filename", "oldStr", "newStr"), required = setOf("filename", "oldStr", "newStr"), functionName)
                fs.replaceContent(
                    paramString(args, "filename", functionName)!!,
                    paramString(args, "oldStr", functionName)!!,
                    paramString(args, "newStr", functionName)!!
                )
            }
            else -> throw IllegalArgumentException("Unsupported fs method: $functionName(${args.keys})")
        }
    }

    companion object {

        @Deprecated("Not used anymore")
        fun toExpression(tc: ToolCall): String {
            ActionValidator().validateToolCall(tc)

            val arguments = tc.arguments
            val expression = when (tc.method) {
                // Filesystem-level operations
                "writeString" -> arguments["filename"]?.let { f ->
                    val c = arguments["content"] ?: ""
                    "fs.writeString(${f.norm()}, ${c.norm()})"
                }
                "readString" -> arguments["filename"]?.let { f ->
                    // optional external flag if provided
                    val external = arguments["external"]
                    if (external == null) "fs.readString(${f.norm()})" else "fs.readString(${f.norm()}, ${external})"
                }
                "replaceContent" -> arguments["filename"]?.let { f ->
                    val oldStr = arguments["oldStr"] ?: return@let null
                    val newStr = arguments["newStr"] ?: return@let null
                    "fs.replaceContent(${f.norm()}, ${oldStr.norm()}, ${newStr.norm()})"
                }
                else -> null
            }

            return expression ?: throw IllegalArgumentException("Illegal tool call | $tc")
        }
    }
}
