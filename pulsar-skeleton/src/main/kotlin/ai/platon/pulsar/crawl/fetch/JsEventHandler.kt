package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay

interface JsEventHandler {
    suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any?
    suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any?
}

abstract class AbstractJsEventHandler: JsEventHandler {
    open var delayMillis = 500L
    open var verbose = false

    override suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return null
    }

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return null
    }

    protected suspend fun evaluate(driver: WebDriver, expressions: Iterable<String>): Any? {
        var value: Any? = null
        expressions.mapNotNull { it.trim().takeIf { it.isNotBlank() } }.filterNot { it.startsWith("// ") }.forEach {
//            log.takeIf { verbose }?.info("Evaluate expression >>>$it<<<")
            val v = evaluate(driver, it)
            if (v is String) {
                val s = Strings.stripNonPrintableChar(v)
//                log.takeIf { verbose }?.info("Result >>>$s<<<")
            } else if (v is Int || v is Long) {
//                log.takeIf { verbose }?.info("Result >>>$v<<<")
            }
            value = v
        }
        return value
    }

    protected suspend fun evaluate(driver: WebDriver, expression: String): Any? {
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        return driver.evaluate(expression)
    }
}

class DefaultJsEventHandler(
    val beforeComputeExpressions: Iterable<String>,
    val afterComputeExpressions: Iterable<String>
): AbstractJsEventHandler() {
    constructor(bcExpressions: String, acExpressions2: String, delimiters: String = ";"): this(
        bcExpressions.split(delimiters), acExpressions2.split(delimiters))

    override suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return evaluate(driver, beforeComputeExpressions)
    }

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return evaluate(driver, afterComputeExpressions)
    }
}