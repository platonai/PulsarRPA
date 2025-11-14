package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import kotlin.reflect.KClass

class WebDriverExToolExecutor: AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "driverEx"

    // target must be AbstractWebDriver (was Browser before, incorrect)
    override val targetClass: KClass<*> = AbstractWebDriver::class

    /**
     * Execute driverEx.* expressions with named args.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "driverEx") { "Object must be a driverEx" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        val driver = requireNotNull(target as? AbstractWebDriver) { "Target must be AbstractWebDriver" }

        return when (functionName) {
            "extract" -> {
                validateArgs(args, allowed = setOf("selectors"), required = setOf("selectors"), functionName)
                val selectors = paramStringList(args, "selectors", functionName, required = true)
                // simple behavior: union selection by comma
                val union = selectors.joinToString(",")
                driver.selectTextAll(union)
            }
            else -> throw IllegalArgumentException("Unsupported driverEx method: $functionName(${args.keys})")
        }
    }

    companion object {

        fun toExpression(tc: ToolCall): String {
            val arguments = tc.arguments

            throw IllegalArgumentException("Illegal tool call | $tc")
        }
    }
}
