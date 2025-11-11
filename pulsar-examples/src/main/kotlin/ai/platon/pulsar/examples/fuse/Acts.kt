package ai.platon.pulsar.examples.fuse

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions

class Acts {
    private val logger = getLogger(this)

    private var stepNo = 0
    private val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        // Use local mock site instead of external site so actions are deterministic.
        val url = "https://news.ycombinator.com/news"

        val driver = session.getOrCreateBoundDriver()
        val agent = session.companionAgent

        driver.open(url)

        val action = "提取列表页数据：文章标题，评论数"
        val result = agent.resolve(action)
        result("action result", result)
    }

    private fun result(label: String, value: Any?) {
        val text = Strings.compactLog(value?.toString(), 2000)

        val e = """🟢"""
        printlnPro("$e [RESULT ${stepNo}] $label => $text")
    }
}

suspend fun main() = Acts().run()
