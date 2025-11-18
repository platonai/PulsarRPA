package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.browser.BrowserProfileMode

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent(profileMode = BrowserProfileMode.SYSTEM_DEFAULT)

    val problem = "打开 amazon.com，搜索 ai agents，对搜索结果给出一个总结"
    agent.resolve(problem)
}
