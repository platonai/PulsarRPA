package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.TcEvaluate
import ai.platon.pulsar.skeleton.ai.ToolCall
import kotlin.reflect.KClass

interface ToolExecutor {

    val domain: String
    val targetClass: KClass<*>

    suspend fun execute(tc: ToolCall, target: Any): TcEvaluate
    suspend fun execute(expression: String, target: Any): TcEvaluate
    fun toExpression(tc: ToolCall): String
}

abstract class AbstractToolExecutor: ToolExecutor {

    private val logger = getLogger(this)
    private val parser = SimpleKotlinParser()

    @Deprecated("Not used anymore")
    override fun toExpression(tc: ToolCall): String {
        throw NotImplementedError()
    }

    override suspend fun execute(tc: ToolCall, target: Any): TcEvaluate {
        val objectName = tc.domain
        val functionName = tc.method
        val args = tc.arguments
        val pseudoExpression = tc.pseudoExpression

        return try {
            val r = execute(objectName, functionName, args, target)

            val className = if (r == null) "null" else r::class.qualifiedName
            val value = if (r == Unit) null else r
            TcEvaluate(value = value, className = className, expression = pseudoExpression)
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", pseudoExpression, e.brief())
            TcEvaluate(pseudoExpression, e)
        }
    }

    override suspend fun execute(expression: String, target: Any): TcEvaluate {
        val (objectName, functionName, args) = parser.parseFunctionExpression(expression)
            ?: return TcEvaluate(expression = expression, cause = IllegalArgumentException("Illegal expression"))

        val tc = ToolCall(objectName, functionName, args)
        return execute(tc, target)
    }

    abstract suspend fun execute(objectName: String, functionName: String, args: Map<String, Any?>, target: Any): Any?
}
