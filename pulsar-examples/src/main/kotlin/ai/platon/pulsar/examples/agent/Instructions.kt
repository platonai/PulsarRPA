package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class InstructionsExample {
    val session = AgenticContexts.getOrCreateSession(spa = true)
    val driver = session.context.launchDefaultBrowser().newDriver()

    suspend fun run() {
        driver.navigateTo("https://news.ycombinator.com/news")
        session.bindDriver(driver)
        session.act("search for 'browser'")
    }
}

suspend fun main() = InstructionsExample().run()
