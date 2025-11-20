package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problem = "打开 amazon.com，搜索 AI robot，打开前5个产品，对产品给出一个对比，写入文件"
    agent.resolve(problem)
}
