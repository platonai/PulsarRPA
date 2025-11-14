package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.tools.AgentToolManager
import ai.platon.pulsar.common.getLogger
import kotlin.reflect.KClass

class SystemToolExecutor(
    val agentToolManager: AgentToolManager
) : AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "system"

    override val targetClass: KClass<*> = SystemToolExecutor::class

    fun help(domain: String, method: String): String {
        return agentToolManager.help(domain, method)
    }

    /**
     * Execute system.* expressions with named args.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "system") { "Object must be an System" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }

        return when (functionName) {
            "help" -> {
                validateArgs(args, allowed = setOf("domain", "method"), required = setOf("domain", "method"), functionName)
                help(args["domain"]!! as String, args["method"]!! as String)
            }

            else -> throw IllegalArgumentException("Unsupported system method: $functionName(${args.keys})")
        }
    }
}
