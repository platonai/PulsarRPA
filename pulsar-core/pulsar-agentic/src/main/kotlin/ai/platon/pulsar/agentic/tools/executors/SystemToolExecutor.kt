package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall
import kotlin.reflect.KClass

class SystemToolExecutor: AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "system"

    override val targetClass: KClass<*> = SystemToolExecutor::class

    @Throws(IllegalArgumentException::class)
    override fun toExpression(tc: ToolCall): String {
        return Companion.toExpression(tc)
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
                validateArgs(args, allowed = emptySet(), required = emptySet(), functionName)
                mapOf(
                    "domain" to domain,
                    "methods" to listOf("help")
                )
            }
            else -> throw IllegalArgumentException("Unsupported system method: $functionName(${args.keys})")
        }
    }

    companion object {
        fun toExpression(tc: ToolCall): String {
            val arguments = tc.arguments
            val expression = when (tc.method) {
                "help" -> "system.help()"
                else -> null
            }

            return expression ?: throw IllegalArgumentException("Illegal tool call | $tc")
        }
    }
}
