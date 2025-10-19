package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.skeleton.PulsarSettings

class InstructionsExample {
    init {
        PulsarSettings.withSPA()
    }

    val session = AgenticContexts.getOrCreateSession()
    val driver = session.context.launchDefaultBrowser().newDriver()

    suspend fun run() {
        driver.navigateTo("https://news.ycombinator.com/news")
        session.bindDriver(driver)
        session.act("search for 'browser'")
    }
}

suspend fun main() = InstructionsExample().run()
