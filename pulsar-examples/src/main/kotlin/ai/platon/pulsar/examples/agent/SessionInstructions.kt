package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlinx.coroutines.runBlocking

class SessionInstructionsExample {
    val session = AgenticContexts.createSession()
    val driver = session.context.browserFactory.launchDefaultBrowser().newDriver()

    suspend fun run() {
        PulsarSettings().withSPA()

        session.bindDriver(driver)
        val url = "https://news.ycombinator.com/news"

        val page = session.open(url)
        val document = session.parse(page)
        val fields = session.extract(document, mapOf("title" to "#title"))

        val actResult = driver.act("search for 'browser'")

        val page2 = session.attach(url, driver)
        val document2 = session.parse(page)
        val fields2 = session.extract(document, mapOf("title" to "#title"))

    }
}

fun main() = runBlocking { SessionInstructionsExample().run() }
