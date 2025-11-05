package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.common.FileSystem
import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import javax.script.ScriptEngineManager

/**
 * Executes WebDriver commands provided as string expressions.
 *
 * This class serves as a bridge between text-based automation commands and WebDriver actions.
 * It parses string commands and executes the corresponding WebDriver methods, enabling
 * script-based control of browser automation.
 *
 * ## Key Features:
 * - Supports a wide range of WebDriver commands, such as navigation, interaction, and evaluation.
 * - Provides error handling to ensure robust execution of commands.
 * - Includes a companion object for parsing function calls from string inputs.
 *
 * ## Example Usage:
 *
 * ```kotlin
 * val executor = ToolCallExecutor()
 * val result = executor.execute("driver.open('https://example.com')", driver)
 * ```
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
open class ToolCallExecutor {
    private val logger = getLogger(this)
    private val engine = ScriptEngineManager().getEngineByExtension("kts")

    /**
     * Evaluate [expression].
     *
     * Slower and unsafe.
     *
     * ```kotlin
     * eval("""driver.click("#submit")""", driver)
     * ```
     * */
    fun eval(expression: String, driver: WebDriver): Any? {
        return eval(expression, mapOf("driver" to driver))
    }

    fun eval(expression: String, browser: Browser): Any? {
        return eval(expression, mapOf("browser" to browser))
    }

    fun eval(expression: String, agent: PerceptiveAgent): Any? {
        return eval(expression, mapOf("agent" to agent))
    }

    fun eval(expression: String, variables: Map<String, Any>): Any? {
        return try {
            variables.forEach { (key, value) -> engine.put(key, value) }
            engine.eval(expression)
        } catch (e: Exception) {
            logger.warn("Error eval expression: {} - {}", expression, e.brief())
            null
        }
    }

    /**
     * Executes a WebDriver command provided as a string expression.
     *
     * Parses the command string to extract the function name and arguments, then invokes
     * the corresponding WebDriver method. For example, the string "driver.open('https://example.com')"
     * would be parsed and the driver.open() method would be called with the URL argument.
     *
     * @param expression The expression(e.g., "driver.method(arg1, arg2)").
     * @param driver The WebDriver instance to execute the command on.
     * @return The result of the command execution, or null if the command could not be executed.
     */
    suspend fun execute(expression: String, driver: WebDriver): Any? {
        return WebDriverToolCallExecutor().execute(expression, driver)
    }

    suspend fun execute(expression: String, browser: Browser): Any? {
        return BrowserToolCallExecutor().execute(expression, browser)
    }

    suspend fun execute(expression: String, browser: Browser, session: AgenticSession): Any? {
        return BrowserToolCallExecutor().execute(expression, browser, session)
    }

    suspend fun execute(expression: String, fs: FileSystem): Any? {
        return FileSystemToolCallExecutor().execute(expression, fs)
    }

    suspend fun execute(toolCall: ToolCall, driver: WebDriver): Any? {
        require(toolCall.domain == "driver") { "Tool call domain should be `driver`" }
        val expression = toolCallToExpression(toolCall)
            ?: throw IllegalArgumentException("Failed to convert to expression: $toolCall")

        return try {
            execute(expression, driver)
        } catch (e: Exception) {
            logger.warn("Error executing TOOL CALL: {} - {}", toolCall, e.brief())
            null
        }
    }

    suspend fun execute(toolCall: ToolCall, browser: Browser): Any? {
        require(toolCall.domain == "browser") { "Tool call domain should be `browser`" }
        val expression = toolCallToExpression(toolCall) ?: return null

        return try {
            execute(expression, browser)
        } catch (e: Exception) {
            logger.warn("Error executing TOOL CALL: {} - {}", toolCall, e.brief())
            null
        }
    }

    suspend fun execute(toolCall: ToolCall, agent: PerceptiveAgent): Any? {
        TODO("execute `toolCall` in browser domain")
    }

    companion object {
        /**
         * Parses a function call from a text string into its components.
         * Uses a robust state machine to correctly handle:
         * - Strings with commas and escaped quotes/backslashes
         * - Nested parentheses inside arguments
         * - Optional whitespace and trailing commas
         */
        fun parseKotlinFunctionExpression(input: String) = SimpleKotlinParser().parseFunctionExpression(input)

        // Basic string escaper to safely embed values inside Kotlin string literals
        internal fun String.esc(): String = this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        internal fun String.norm() = Strings.doubleQuote(this.esc())

        fun toolCallToExpression(tc: ToolCall): String? {
            return when (tc.domain) {
                "driver" -> WebDriverToolCallExecutor.toolCallToExpression(tc)
                "browser" -> BrowserToolCallExecutor.toolCallToExpression(tc)
                else -> null
            }
        }
    }
}
