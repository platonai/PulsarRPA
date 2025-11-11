package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

class AgentToolExecutor : AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "agent"

    override val targetClass: KClass<*> = PerceptiveAgent::class

    companion object {
        private val objectMapper = ObjectMapper()
    }

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

        // Helper: coerce schema parameter to Map<String,String>, only accept Map or JSON object string
        fun coerceSchema(raw: Any?, functionName: String): ExtractionSchema {
            if (raw == null) throw IllegalArgumentException("Missing parameter 'schema' for $functionName")
            return when (raw) {
                is ExtractionSchema -> raw
                is Map<*, *> -> {
                    ExtractionSchema.fromMap(raw)
                }
                is String -> {
                    ExtractionSchema.parse(raw)
                }
                else -> throw IllegalArgumentException("Parameter 'schema' must be ExtractionSchema or JSON object string; actual='${raw::class.simpleName}'")
            }
        }

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
            // agent.extract(instruction: String) OR agent.extract(instruction: String, schema: Map<String,String>)
            "extract" -> {
                return if ("schema" in args) {
                    validateArgs(args, allowed = setOf("instruction", "schema"), required = setOf("instruction", "schema"), functionName)
                    val instruction = paramString(args, "instruction", functionName)!!
                    val schema = coerceSchema(args["schema"], functionName)
                    agent.extract(instruction, schema)
                } else {
                    validateArgs(args, allowed = setOf("instruction"), required = setOf("instruction"), functionName)
                    agent.extract(paramString(args, "instruction", functionName)!!)
                }
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
