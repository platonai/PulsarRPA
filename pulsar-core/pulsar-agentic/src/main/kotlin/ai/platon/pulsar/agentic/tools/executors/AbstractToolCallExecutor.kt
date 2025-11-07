package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.TcEvaluation

abstract class AbstractToolCallExecutor {

    private val logger = getLogger(this)
    private val parser = SimpleKotlinParser()

    suspend fun execute(expression: String, target: Any): TcEvaluation {
        return try {
            val (objectName, functionName, args) = parser.parseFunctionExpression(expression)
                ?: return TcEvaluation(expression = expression, cause = IllegalArgumentException("Illegal expression"))

            val r = doExecute(objectName, functionName, args, target)
            // Convert kotlin.Unit to null to match expected behavior
            val value = if (r == Unit) null else r
            TcEvaluation(value = value, expression = expression)
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            TcEvaluation(expression, e)
        }
    }

    abstract suspend fun doExecute(objectName: String, functionName: String, args: Map<String, Any?>, target: Any): Any?
}
