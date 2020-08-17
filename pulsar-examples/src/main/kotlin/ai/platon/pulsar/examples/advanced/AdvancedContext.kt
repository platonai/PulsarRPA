package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.Manual

fun main() = withContext("classpath:pulsar-beans/app-context.xml") {
    System.setProperty("use_proxy", "no")
    Manual(it).runAll()
}
