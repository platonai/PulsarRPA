package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val maxSteps = 100
    var i = 0
    while (i++ < maxSteps) {
        val result = agent.act("打开 https://www.amazon.com/b?node=172282，严格遵循向下滚动10次的指令，每次滚动报告当前滚动位置和当前的屏幕截图中的内容")

        if (result.isComplete) {
            break
        }
    }
}
