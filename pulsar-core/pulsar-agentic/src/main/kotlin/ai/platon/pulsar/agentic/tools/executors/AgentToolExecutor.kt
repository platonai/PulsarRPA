package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import kotlin.reflect.KClass

class AgentToolExecutor : AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "agent"

    override val targetClass: KClass<*> = PerceptiveAgent::class

    /**
     * Execute agent.* expressions against a PerceptiveAgent target using named args.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "agent") { "Object must be an Agent" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }

        val agent = requireNotNull(target as? PerceptiveAgent) { "Target must be a PerceptiveAgent" }

        return when (functionName) {
            // agent.act(action: String)
            "act" -> {
                validateArgs(args, allowed = setOf("action"), required = setOf("action"), functionName)
                agent.act(paramString(args, "action", functionName)!!)
            }
            // agent.observe(instruction: String)
            "observe" -> {
                validateArgs(args, allowed = setOf("instruction"), required = setOf("instruction"), functionName)
                agent.observe(paramString(args, "instruction", functionName)!!)
            }
            // agent.extract(instruction: String)
            "extract" -> {
                validateArgs(args, allowed = setOf("instruction"), required = setOf("instruction"), functionName)
                agent.extract(paramString(args, "instruction", functionName)!!)
            }
            // agent.resolve(problem: String)
            "resolve" -> {
                validateArgs(args, allowed = setOf("problem"), required = setOf("problem"), functionName)
                agent.resolve(paramString(args, "problem", functionName)!!)
            }
            // Signal completion; just return true
            "done" -> {
                validateArgs(args, allowed = emptySet(), required = emptySet(), functionName)
                true
            }
            else -> {
                throw IllegalArgumentException("Unsupported agent method: $functionName(${args.keys})")
            }
        }
    }
}
