package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.common.AgentFileSystem
import kotlin.reflect.KClass

class FileSystemToolExecutor : AbstractToolExecutor() {

    override val domain = "fs"

    override val targetClass: KClass<*> = AgentFileSystem::class

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
}
