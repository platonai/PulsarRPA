package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problem = "打开 amazon.com，搜索 ai agents，对搜索结果给出一个总结"
    agent.resolve(problem)
}
