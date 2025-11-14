package ai.platon.pulsar.manual

import ai.platon.pulsar.agentic.context.AgenticContexts

class ProblemResolver {
    val agent = AgenticContexts.getOrCreateAgent()

    suspend fun run() {
        val problems = """
go to https://news.ycombinator.com/news , search for browser and read top 5 articles and give me a summary
go to amazon.com and search for pens to draw on whiteboards
打开百度查找厦门岛旅游景点，给出一个总结
go to https://news.ycombinator.com/news , open the 4-th articles in new tab
go to https://news.ycombinator.com/news , read top 3 articles and give me a summary
        """.split("\n").filter { it.isNotBlank() }

        val problem = problems[0]
        agent.resolve(problem)

        agent.processTrace.forEach { println("""🚩$it""") }
    }
}

suspend fun main() = ProblemResolver().run()
