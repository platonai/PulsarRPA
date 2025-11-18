package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problem = "打开 https://www.amazon.com/b?node=172282，向下滚动10次"
    agent.resolve(problem)

    val maxSteps = 100
    var i = 0
    while (i++ < maxSteps) {
        // TODO: interface/API call with remote context management is under deployment
        val result = agent.act("向下滚动 10 次")

        if (result.message == "completed") {
            break
        }
    }
}
