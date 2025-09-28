package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import kotlinx.coroutines.runBlocking

class InstructionsExample {
    val context = AgenticContexts.create()
    val driver = context.browserFactory.launchDefaultBrowser().newDriver()

    suspend fun run() {
        driver.navigateTo("https://news.ycombinator.com/news")
        driver.act("search for 'browser'")
    }
}

fun main() = runBlocking { InstructionsExample().run() }
