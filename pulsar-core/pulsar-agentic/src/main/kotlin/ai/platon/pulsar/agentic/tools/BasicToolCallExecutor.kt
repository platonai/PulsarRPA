package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.common.SimpleKotlinParser
import ai.platon.pulsar.agentic.tools.executors.*
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.skeleton.ai.TcEvaluate
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import javax.script.ScriptEngineManager
import kotlin.reflect.full.isSuperclassOf

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
open class BasicToolCallExecutor(
    val toolExecutors: List<ToolExecutor>
) {
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
    fun eval(expression: String, driver: WebDriver): TcEvaluate {
        return eval(expression, mapOf("driver" to driver))
    }

    fun eval(expression: String, browser: Browser): TcEvaluate {
        return eval(expression, mapOf("browser" to browser))
    }

    fun eval(expression: String, agent: PerceptiveAgent): TcEvaluate {
        return eval(expression, mapOf("agent" to agent))
    }

    fun eval(expression: String, variables: Map<String, Any>): TcEvaluate {
        return try {
            variables.forEach { (key, value) -> engine.put(key, value) }
            val any = engine.eval(expression)
            TcEvaluate(value = any, expression = expression)
        } catch (e: Exception) {
            logger.warn("Error eval expression: {} - {}", expression, e.stackTraceToString())
            TcEvaluate(expression, e)
        }
    }

    suspend fun execute(expression: String, browser: Browser, session: AgenticSession): TcEvaluate {
        return BrowserToolExecutor().execute(expression, browser, session)
    }

    @Throws(UnsupportedOperationException::class)
    suspend fun execute(tc: ToolCall, target: Any): TcEvaluate {
        return toolExecutors
            .firstOrNull { it.targetClass.isSuperclassOf(target::class) }
            ?.execute(tc, target)
            ?: throw UnsupportedOperationException("❓ Unsupported target ${target::class}")

//        return when (target) {
//            is WebDriver -> WebDriverToolExecutor().execute(tc, target)
//            is Browser -> BrowserToolExecutor().execute(tc, target)
//            is FileSystem -> FileSystemToolExecutor().execute(tc, target)
//            is PerceptiveAgent -> AgentToolExecutor().execute(tc, target)
//            else -> throw UnsupportedOperationException("❓ Unsupported target ${target::class}")
//        }
    }

    @Throws(UnsupportedOperationException::class)
    suspend fun execute(expression: String, target: Any): TcEvaluate {
        return toolExecutors
            .firstOrNull { it.targetClass.isSuperclassOf(target::class) }
            ?.execute(expression, target)
            ?: throw UnsupportedOperationException("❓ Unsupported target ${target::class}")

//        return when (target) {
//            is WebDriver -> WebDriverToolExecutor().execute(expression, target)
//            is Browser -> BrowserToolExecutor().execute(expression, target)
//            is FileSystem -> FileSystemToolExecutor().execute(expression, target)
//            is PerceptiveAgent -> AgentToolExecutor().execute(expression, target)
//            else -> throw UnsupportedOperationException("❓ Unsupported target ${target::class}")
//        }
    }

    fun toExpression(tc: ToolCall) = Companion.toExpression(tc)

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

        fun toExpression(tc: ToolCall): String {
            return when (tc.domain) {
                "driver" -> WebDriverToolExecutor.toExpression(tc)
                "browser" -> BrowserToolExecutor.toExpression(tc)
                "fs" -> FileSystemToolExecutor.toExpression(tc)
                "agent" -> AgentToolExecutor.toExpression(tc)
                else -> throw IllegalArgumentException("⚠️ Illegal tool call | $tc")
            }
        }

        fun toExpressionOrNull(tc: ToolCall): String? = runCatching { BasicToolCallExecutor.toExpression(tc) }.getOrNull()
    }
}
