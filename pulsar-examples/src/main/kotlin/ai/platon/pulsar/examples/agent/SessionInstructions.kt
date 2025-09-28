package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlinx.coroutines.runBlocking

class SessionInstructionsExample {
    init {
        // Single Page Application
        PulsarSettings().withSPA()
    }

    val session = AgenticContexts.createSession()

    suspend fun run() {
        val driver = session.context.browserFactory.launchDefaultBrowser().newDriver()
        session.bindDriver(driver)

        val url = "https://news.ycombinator.com/news"

        val page = session.open(url)
        val document = session.parse(page)
        val fields = session.extract(document, mapOf("title" to "#title"))

        val actResult = session.act("search for 'browser'")

        val page2 = session.attach(url, driver)
        val document2 = session.parse(page2)
        val fields2 = session.extract(document, mapOf("title" to "#title"))

    }
}

fun main() = runBlocking { SessionInstructionsExample().run() }
