package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.TcEvaluate

abstract class AbstractToolExecutor {

    private val logger = getLogger(this)
    private val parser = SimpleKotlinParser()

    suspend fun execute(expression: String, target: Any): TcEvaluate {
        return try {
            val (objectName, functionName, args) = parser.parseFunctionExpression(expression)
                ?: return TcEvaluate(expression = expression, cause = IllegalArgumentException("Illegal expression"))

            val r = doExecute(objectName, functionName, args, target)

            val className = if (r == null) "null" else r::class.qualifiedName
            val value = if (r == Unit) null else r
            TcEvaluate(value = value, className = className, expression = expression)
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            TcEvaluate(expression, e)
        }
    }

    abstract suspend fun doExecute(objectName: String, functionName: String, args: Map<String, Any?>, target: Any): Any?
}
