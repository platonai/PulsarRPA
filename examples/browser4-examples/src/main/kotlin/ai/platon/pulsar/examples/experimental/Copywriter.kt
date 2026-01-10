package ai.platon.pulsar.examples.experimental

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val task = """
        1. go to https://www.amazon.com/
        2. search for pens to draw on whiteboards
        3. compare the first 4 ones
        4. write the result to a markdown file
        """.trimIndent()

    AgenticContexts.run(task)
}
