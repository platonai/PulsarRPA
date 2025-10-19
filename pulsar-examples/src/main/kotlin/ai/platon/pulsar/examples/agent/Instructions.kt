package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import kotlinx.coroutines.runBlocking

class InstructionsExample {
    val session = AgenticContexts.getOrCreateSession()
    val driver = session.context.launchDefaultBrowser().newDriver()

    suspend fun run() {
        driver.navigateTo("https://news.ycombinator.com/news")
        session.act("search for 'browser'")
    }
}

fun main() = runBlocking { InstructionsExample().run() }
