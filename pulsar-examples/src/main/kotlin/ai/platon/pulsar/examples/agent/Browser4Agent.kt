package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problem = """
go to https://news.ycombinator.com/news , read top 3 articles and give me a summary
        """.trimIndent()

    agent.resolve(problem)
}
