package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()
    val problem = "go to https://news.ycombinator.com/news"
    agent.resolve(problem)
}
