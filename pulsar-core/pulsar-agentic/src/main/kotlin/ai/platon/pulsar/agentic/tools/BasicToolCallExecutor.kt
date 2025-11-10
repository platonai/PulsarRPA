package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.tools.executors.BrowserToolExecutor
import ai.platon.pulsar.agentic.tools.executors.ToolExecutor
import ai.platon.pulsar.agentic.tools.executors.WebDriverToolExecutor
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
    val toolExecutors: List<ToolExecutor> = listOf(WebDriverToolExecutor(), BrowserToolExecutor())
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
    }

    @Throws(UnsupportedOperationException::class)
    suspend fun execute(expression: String, target: Any): TcEvaluate {
        return toolExecutors
            .firstOrNull { it.targetClass.isSuperclassOf(target::class) }
            ?.execute(expression, target)
            ?: throw UnsupportedOperationException("❓ Unsupported target ${target::class}")
    }
}
