package ai.platon.pulsar.examples.todo

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.browser.BrowserProfileMode
import ai.platon.pulsar.skeleton.PulsarSettings
import com.google.common.collect.Iterators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class ParallelAgents {
    val profileMode = BrowserProfileMode.TEMPORARY
    init {
        PulsarSettings.withTemporaryBrowser()
    }
    val agents = IntRange(0, 5).map { AgenticContexts.createAgent(profileMode = profileMode) }
    val iterator = Iterators.cycle(agents)
    val scope = CoroutineScope(Dispatchers.Default)

    suspend fun run() {
        val problems = """
            打开百度查找厦门岛旅游景点，给出一个总结

go to https://news.ycombinator.com/news , read top 3 articles and give me a summary
            go to amazon.com, search for pens to draw on whiteboards, compare the first 4 ones (read detail), write the result to a markdown file.

go to https://news.ycombinator.com/item?id=19553941 , extract content and give me a summary
go to https://moonshotai.github.io/Kimi-K2/thinking.html , extract content and give me a summary, write the result to a markdown file.


go to https://moonshotai.github.io/Kimi-K2/thinking.html , extract content and give me a summary
go to https://news.ycombinator.com/news , search for browser and read top 5 articles and give me a summary
go to https://news.ycombinator.com/news , open the 4-th articles in new tab
        """.split("\n").filter { it.isNotBlank() }

        val jobs = problems.map { scope.launch { iterator.next().resolve(it) } }
        jobs.joinAll()
    }
}

suspend fun main() = ParallelAgents().run()
