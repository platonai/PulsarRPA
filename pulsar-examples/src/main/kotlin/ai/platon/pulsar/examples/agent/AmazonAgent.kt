package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problem = "打开 amazon.com，搜索 calabi-yau，打开前5个产品，对产品给出一个总结"
    agent.resolve(problem)
}
