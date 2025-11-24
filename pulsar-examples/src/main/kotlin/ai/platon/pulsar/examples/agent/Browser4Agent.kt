package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val task = """
        1. go to https://news.ycombinator.com/news
        2. search for browser
        3. read top 3 articles, give me a summary for each article
        """.trimIndent()

    val history = agent.run(task)
    println(history.finalResult)
}
