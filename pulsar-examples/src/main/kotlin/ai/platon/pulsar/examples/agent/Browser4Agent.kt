package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

suspend fun main() {
    val agent = AgenticContexts.getOrCreateAgent()

    val problems = """
打开百度查找厦门岛旅游景点，给出一个总结
go to https://news.ycombinator.com/news , read top 3 articles and give me a summary
go to amazon.com, search for pens to draw on whiteboards, compare the first 4 ones (read detail), write the result to a markdown file.
go to https://news.ycombinator.com/item?id=19553941 , extract content and give me a summary
go to https://moonshotai.github.io/Kimi-K2/thinking.html , extract content and give me a summary, write the result to a markdown file.
go to https://moonshotai.github.io/Kimi-K2/thinking.html , extract content and give me a summary
go to https://news.ycombinator.com/news , search for browser and read top 5 articles and give me a summary
go to https://news.ycombinator.com/news , open the 4-th articles in new tab
        """.lines().filter { it.isNotBlank() }

    val problem = problems[0]
    agent.resolve(problem)
}
