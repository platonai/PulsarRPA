package ai.platon.pulsar.agentic.ai.support

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser

class BrowserToolCallExecutor {
    private val logger = getLogger(this)

    suspend fun execute(expression: String, browser: Browser): Any? {
        return try {
            val r = execute0(expression, browser)
            when (r) {
                is Unit -> null
                else -> r
            }
        } catch (e: Exception) {
            logger.warn("Error executing expression: {} - {}", expression, e.brief())
            null
        }
    }

    private suspend fun execute0(command: String, browser: Browser): Any? {
        // Extract function name and arguments from the command string
        val (objectName, functionName, args) = SimpleKotlinParser().parseFunctionExpression(command) ?: return null

        return doExecute(objectName, functionName, args, browser)
    }

    /**
     * Extract function name and arguments from the command string
     * */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun doExecute(
        objectName: String,
        functionName: String,
        args: Map<String, Any?>,
        browser: Browser
    ): Any? {
        // Handle browser-level commands
        if (objectName == "browser" && functionName == "switchTab") {
            val tabId = args["0"]?.toString()?.toIntOrNull()
                ?: return buildErrorResponse("tab_not_found", "Missing tabId parameter", browser)

            val targetDriver = browser.drivers.values.filterIsInstance<PulsarWebDriver>().find { it.id == tabId }
            if (targetDriver != null) {
                targetDriver.bringToFront()
                logger.info("Switched to tab {} (driver {}/{})", tabId, targetDriver.id, targetDriver.guid)
                return targetDriver.id
            } else {
                return buildErrorResponse("tab_not_found", "Tab with id '$tabId' not found", browser)
            }
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
}
