package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts

class ProblemResolver {
    val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        val problems = """
go to amazon.com and search for pens to draw on whiteboards
go to https://news.ycombinator.com/news , search for browser and read top 5 articles and give me a summary
打开百度查找厦门岛旅游景点，给出一个总结
go to https://news.ycombinator.com/news , open the 4-th articles in new tab
go to https://news.ycombinator.com/news , read top 3 articles and give me a summary
        """.split("\n").map { it.trim() }.filter { it.length > 10 }

        val problem = problems[0]
        val agent = session.resolve(problem)

        agent.processTrace.forEach { println(it) }
    }
}

suspend fun main() = ProblemResolver().run()
