package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class ProblemResolver {
    val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        val driver = session.createBoundDriver()
        driver.navigateTo("https://news.ycombinator.com/news")
        val problems = """
search for browser and read top 5 articles and give me a summary
open the 4-th articles in new tab
read top 3 articles and give me a summary
        """.split("\n").map { it.trim() }

        val problem = problems[0]
        val agent = session.resolve(problem)

        agent.processTrace.forEach { println(it) }
    }
}

suspend fun main() = ProblemResolver().run()
