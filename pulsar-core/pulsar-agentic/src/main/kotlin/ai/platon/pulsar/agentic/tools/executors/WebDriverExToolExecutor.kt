package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import kotlin.reflect.KClass

class WebDriverExToolExecutor: AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "driverEx"

    override val targetClass: KClass<*> = Browser::class

    @Throws(IllegalArgumentException::class)
    override fun toExpression(tc: ToolCall): String {
        return Companion.toExpression(tc)
    }

    /**
     * Extract function name and arguments from the expression string
     * */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "driverEx") { "Object must be a driverEx" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        val driver = requireNotNull(target as AbstractWebDriver) { "Target must be AbstractWebDriver" }

        // Handle browser-level expressions
        if (functionName == "extract") {
            val selectors = (args["selectors"] as? List<*>)
                ?.filterIsInstance<String>()
                ?.joinToString() ?: return null
            val fields = driver.selectTextAll(selectors)
        }

        return null
    }

    companion object {

        fun toExpression(tc: ToolCall): String {
            val arguments = tc.arguments

            throw IllegalArgumentException("Illegal tool call | $tc")
        }
    }
}
