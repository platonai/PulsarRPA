package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.tools.ToolCallExecutor.Companion.norm
import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

class AgentToolCallExecutor {
    private val logger = getLogger(this)

    suspend fun execute(expression: String, agent: PerceptiveAgent): Any? {
        return try {
            val r = execute0(expression, agent)
            when (r) {
                is Unit -> null
                else -> r
            }
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            null
        }
    }

    private suspend fun execute0(expression: String, agent: PerceptiveAgent): Any? {
        // Extract function name and arguments from the expression string
        val (objectName, functionName, args) = SimpleKotlinParser().parseFunctionExpression(expression) ?: return null

        return doExecute(objectName, functionName, args, agent)
    }

    /**
     * Extract function name and arguments from the expression string
     * */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, agent: PerceptiveAgent
    ): Any? {
        require(objectName == "browser") { "Object must be a Browser" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }

        // Handle browser-level expressions
        if (functionName == "done") {
        }

        return null
    }

    /**
     * Build a structured error response for browser operations.
     * Returns a map with error information and available tabs.
     */
    protected fun buildErrorResponse(errorType: String, message: String, browser: Browser): Map<String, Any> {
        val availableTabs = browser.drivers.keys.toList()
        return mapOf(
            "error" to errorType,
            "message" to message,
            "availableTabs" to availableTabs
        )
    }

    companion object {

        fun toolCallToExpression(tc: ToolCall): String? {
            ActionValidator().validateToolCall(tc)

            val arguments = tc.arguments
            return when (tc.method) {
                // Browser-level operations
                "switchTab" -> arguments["tabId"]?.let { "browser.switchTab(${it.norm()})" }
                else -> null
            }
        }
    }
}
