package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.tools.ToolCallExecutor.Companion.norm
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.TcEvaluation
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver

class BrowserToolCallExecutor: AbstractToolCallExecutor() {
    private val logger = getLogger(this)

    suspend fun execute(expression: String, browser: Browser, session: AgenticSession): TcEvaluation {
        if (expression.contains("switchTab")) {
            val driver = execute(expression, browser)
            if (driver is WebDriver) {
                session.bindDriver(driver)

                // document.visibilityState should be visible after bringToFront()
                // val isVisible = driver.evaluateValue("document.visibilityState == \"visible\"")
                // require(isVisible)
                // require(driver == browser.frontDriver)
            }

            return driver
        }

        return TcEvaluation(expression, IllegalArgumentException("Unknown expression: $expression, domain: browser"))
    }

    /**
     * Extract function name and arguments from the expression string
     * */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun doExecute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "browser") { "Object must be a Browser" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        val browser = requireNotNull(target as AbstractBrowser) { "Target must be Browser" }

        // Handle browser-level expressions
        if (functionName == "switchTab") {
            val tabId =
                args["0"]?.toString() ?: return buildErrorResponse("tab_not_found", "Missing tabId parameter", browser)

            val driver = if (tabId.toIntOrNull() != null) {
                browser.findDriverById(tabId.toInt())
            } else {
                browser.drivers[tabId]
            }

            if (driver == null || driver !is AbstractWebDriver) {
                return null
            }

            driver.bringToFront()
            logger.info("Switched to tab {} (driver {}/{})", tabId, driver.id, driver.guid)
            return driver
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
