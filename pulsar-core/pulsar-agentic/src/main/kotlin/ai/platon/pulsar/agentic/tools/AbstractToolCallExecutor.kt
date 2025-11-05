package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger

abstract class AbstractToolCallExecutor {

    private val logger = getLogger(this)
    private val parser = SimpleKotlinParser()

    suspend fun execute(expression: String, target: Any): Any? {
        return try {
            val (objectName, functionName, args) = parser.parseFunctionExpression(expression) ?: return null

            val r = doExecute(objectName, functionName, args, target)
            when (r) {
                is Unit -> null
                else -> r
            }
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            null
        }
    }

    abstract suspend fun doExecute(objectName: String, functionName: String, args: Map<String, Any?>, target: Any): Any?
}
