package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class ProblemResolver {
    val session = AgenticContexts.getOrCreateSession(spa = true)

    suspend fun run() {
        val driver = session.createBoundDriver()
        driver.navigateTo("https://news.ycombinator.com/news")
        val agent = session.resolve("find articles about browser and give me a summary")
        // val agent = session.resolve("read top 3 articles and give me a summary")

        println("Result: ")
        agent.history.forEach { println("- $it") }

        readln()
    }
}

suspend fun main() = ProblemResolver().run()
