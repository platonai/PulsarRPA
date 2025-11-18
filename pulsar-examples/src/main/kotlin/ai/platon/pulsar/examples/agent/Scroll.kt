package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problem = "打开 https://www.amazon.com/b?node=172282，向下滚动10次"
    agent.resolve(problem)
}
