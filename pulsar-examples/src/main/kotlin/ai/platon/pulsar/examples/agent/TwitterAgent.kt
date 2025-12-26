package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    // Please run OpenChrome.kt to open the default Chrome browser and login Twitter first.

    val agent = AgenticContexts.getOrCreateAgent()
    val task = "Open X (Twitter) and summarize Elon Musk's latest tweets."
    val history = agent.run(task)
    println(history.finalResult)
}
