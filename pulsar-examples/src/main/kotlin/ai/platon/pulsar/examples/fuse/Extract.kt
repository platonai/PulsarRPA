package ai.platon.pulsar.examples.fuse

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema

class Extract {
    private val logger = getLogger(this)

    private var stepNo = 0
    private val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        // Use local mock site instead of external site so actions are deterministic.
        val url = "https://news.ycombinator.com/news"

        val driver = session.getOrCreateBoundDriver()
        val agent = session.companionAgent

        driver.open(url)

        var action = "提取列表页数据：文章标题，评论信息"
        var schema = """
{"fields": [{"name": "articles", "type": "array", "description": "文章列表", "arrayElements": {"name": "article", "type": "object", "objectMemberProperties": [{"name": "title", "type": "string", "description": "文章标题", "required": true}, {"name": "comments", "type": "string", "description": "评论数", "required": true}]}}]}
        """.trimIndent()

        var extractResult = agent.extract(action, ExtractionSchema.parse(schema))
        result("extract result", extractResult)

        action = "提取前三页列表页数据：文章标题，评论信息"
        val result = agent.resolve(action)
        result("action result", result)
    }

    private fun result(label: String, value: Any?) {
        val text = Strings.compactLog(value?.toString(), 2000)

        val e = """🟢"""
        printlnPro("$e [RESULT ${stepNo}] $label => $text")
    }
}

suspend fun main() = Extract().run()
