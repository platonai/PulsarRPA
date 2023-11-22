package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val scripts = listOf("custom-js/custom-scripts.js").map { ResourceLoader.readString(it) }

    val session = PulsarContexts.createSession()
    val options = session.options("-refresh")

    val be = options.event.browseEvent
    be.onWillNavigate.addFirst { _, driver ->
        println("onWillNavigate " + driver.navigateHistory.history.joinToString { it.url })
        scripts.forEach { driver.addInitScript(it) }
    }
    be.onFeatureComputed.addLast { _, driver ->
        var result = driver.evaluate("__custom_utils__.minus(10, 3)")
        println(result)

        result = driver.evaluate("__pulsar_utils__.add(10, 3)")
        println(result)
//        require(result == 7)

        driver.evaluate("__custom_utils__.addCustomEventListeners()")
    }

    session.load("https://www.amazon.com/dp/B0C1H26C46", options)

    readLine()
}
