package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.common.FileSystem
import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.agentic.tools.executors.AgentToolCallExecutor
import ai.platon.pulsar.agentic.tools.executors.BrowserToolCallExecutor
import ai.platon.pulsar.agentic.tools.executors.FileSystemToolCallExecutor
import ai.platon.pulsar.agentic.tools.executors.WebDriverToolCallExecutor
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.TcEvaluation
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
    fun eval(expression: String, driver: WebDriver): TcEvaluation {
        return eval(expression, mapOf("driver" to driver))
    }

    fun eval(expression: String, browser: Browser): TcEvaluation {
        return eval(expression, mapOf("browser" to browser))
    }

    fun eval(expression: String, agent: PerceptiveAgent): TcEvaluation {
        return eval(expression, mapOf("agent" to agent))
    }

    fun eval(expression: String, variables: Map<String, Any>): TcEvaluation {
        return try {
            variables.forEach { (key, value) -> engine.put(key, value) }
            val any = engine.eval(expression)
            TcEvaluation(
                value = any,
                className = any::class.qualifiedName,
                expression = expression
            )
        } catch (e: Exception) {
            logger.warn("Error eval expression: {} - {}", expression, e.stackTraceToString())
            TcEvaluation(expression, e)
        }
    }

    suspend fun execute(expression: String, browser: Browser, session: AgenticSession): TcEvaluation {
        return BrowserToolCallExecutor().execute(expression, browser, session)
    }

    suspend fun execute(toolCall: ToolCall, target: Any): TcEvaluation {
        val expression = toolCallToExpression(toolCall)
            ?: return TcEvaluation(toolCall.pseudoExpression, IllegalStateException("Illegal expression"))

        return try {
            execute(expression, target)
        } catch (e: Exception) {
            logger.warn("Error executing TOOL CALL: {} - {}", toolCall, e.brief())
            TcEvaluation(expression, e)
        }
    }

    suspend fun execute(expression: String, target: Any): TcEvaluation {
        return when (target) {
            is WebDriver -> WebDriverToolCallExecutor().execute(expression, target)
            is Browser -> BrowserToolCallExecutor().execute(expression, target)
            is FileSystem -> FileSystemToolCallExecutor().execute(expression, target)
            is PerceptiveAgent -> AgentToolCallExecutor().execute(expression, target)
            else -> {
                logger.warn("Error executing expression: {}", expression)
                TcEvaluation(expression, UnsupportedOperationException("Unknown target ${target::class}"))
            }
        }
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
                "fs" -> FileSystemToolCallExecutor.toolCallToExpression(tc)
                // Fix: route agent domain to AgentToolCallExecutor
                "agent" -> AgentToolCallExecutor.toolCallToExpression(tc)
                else -> null
            }
        }
    }
}
