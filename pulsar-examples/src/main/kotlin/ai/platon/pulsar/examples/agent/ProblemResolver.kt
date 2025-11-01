package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class ProblemResolver {
    val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        val driver = session.createBoundDriver()
        driver.navigateTo("https://news.ycombinator.com/news")
        val agent = session.resolve("open the 4-th articles in new tab")
        // val agent = session.resolve("read top 3 articles and give me a summary")

        readln()
    }
}

suspend fun main() = ProblemResolver().run()
