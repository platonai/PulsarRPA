package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.common.FileSystem
import ai.platon.pulsar.agentic.tools.ActionValidator
import ai.platon.pulsar.agentic.tools.BasicToolCallExecutor.Companion.norm
import ai.platon.pulsar.agentic.tools.executors.SystemToolExecutor
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import kotlin.reflect.KClass

class FileSystemToolExecutor : AbstractToolExecutor() {

    override val domain = "fs"

    override val targetClass: KClass<*> = FileSystem::class

    @Throws(IllegalArgumentException::class)
    override fun toExpression(tc: ToolCall): String {
        return Companion.toExpression(tc)
    }

    /**
     * Execute fs.* expressions against a FileSystem target.
     */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "fs") { "Object must be fs" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is FileSystem) { "Target must be a FileSystem" }

        val fs = target
        val arg0 = args["0"]?.toString()
        val arg1 = args["1"]?.toString()
        val arg2 = args["2"]?.toString()
        // val arg3 = args["3"]?.toString()

        return when (functionName) {
            // fs.writeString(filename: String, content: String)
            "writeString" -> if (arg0 != null) fs.writeString(arg0, arg1 ?: "") else null

            // fs.readString(filename: String [, externalFile: Boolean])
            "readString" -> if (arg0 != null) {
                val external = arg1?.let { it.equals("true", true) || it == "1" } ?: false
                fs.readString(arg0, external)
            } else null

            // fs.replaceContent(filename: String, oldStr: String, newStr: String)
            "replaceContent" -> if (arg0 != null && arg1 != null && arg2 != null) {
                fs.replaceContent(arg0, arg1, arg2)
            } else null

            else -> null
        }
    }

    companion object {

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
