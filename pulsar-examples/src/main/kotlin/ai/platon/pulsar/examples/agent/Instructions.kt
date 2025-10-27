package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class InstructionsExample {
    val session = AgenticContexts.getOrCreateSession(spa = true)

    suspend fun run() {
        val driver = session.createBoundDriver()
        driver.navigateTo("https://news.ycombinator.com/news")
        session.act("click the second article")
    }
}

suspend fun main() = InstructionsExample().run()
