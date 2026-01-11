package ai.platon.pulsar.examples.experimental

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val task = """
        1. go to https://weibo.com/u/1239003334
        2. search for "编程"
        3. extract all the posts related to "编程"
        4. write the result to a markdown file
        5. summarize the result in 500 words
        """.trimIndent()

    AgenticContexts.run(task)
}
