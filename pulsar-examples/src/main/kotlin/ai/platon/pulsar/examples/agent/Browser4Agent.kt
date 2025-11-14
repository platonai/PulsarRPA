package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class Browser4Agent {
    val agent = AgenticContexts.getOrCreateAgent()

    suspend fun run() {
        val problems = """
            go to amazon.com, search for pens to draw on whiteboards, compare the first 4 ones, write the result to a markdown file.

go to https://moonshotai.github.io/Kimi-K2/thinking.html , extract content and give me a summary, write the result to a markdown file.

go to https://moonshotai.github.io/Kimi-K2/thinking.html , extract content and give me a summary
go to https://news.ycombinator.com/item?id=19553941 , extract content and give me a summary
go to https://news.ycombinator.com/news , search for browser and read top 5 articles and give me a summary
打开百度查找厦门岛旅游景点，给出一个总结
go to https://news.ycombinator.com/news , open the 4-th articles in new tab
go to https://news.ycombinator.com/news , read top 3 articles and give me a summary
        """.split("\n").filter { it.isNotBlank() }

        val problem = problems[0]
        agent.resolve(problem)

        agent.processTrace.forEach { println("""🚩$it""") }
    }
}

suspend fun main() = Browser4Agent().run()
