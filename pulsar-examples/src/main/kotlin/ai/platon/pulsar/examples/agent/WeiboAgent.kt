package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class WeiboAgent {
    val agent = AgenticContexts.getOrCreateAgent()

    suspend fun run() {
        val problem = "打开微博首页，搜索智能体，给出一个总结"
        agent.resolve(problem)
    }
}

suspend fun main() = WeiboAgent().run()
