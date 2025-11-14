package ai.platon.pulsar.examples.fuse

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.test.server.DemoSiteStarter

class SummarizePage {
    private val logger = getLogger(this)

    private var stepNo = 0
    private val session = AgenticContexts.getOrCreateSession()

    suspend fun run() {
        // Use local mock site instead of external site so actions are deterministic.
        val url = "http://localhost:18080/generated/tta/act/act-demo.html"
        // one more short wait after potential start (shorter, less verbose)
        val starter = DemoSiteStarter()
        starter.start(url)
        session.registerClosable(starter)

        val driver = session.getOrCreateBoundDriver()
        val agent = session.companionAgent

        step("Open URL: $url")
        var page = session.open(url)
        result("page", page)

        printlnPro(driver.url())

        // Basic action examples (natural language instructions) - now operate on local mock page.
        step("Action: search for 'browser'")
        var actOptions = ActionOptions("search for 'browser'")
        var result = agent.act(actOptions)
        result("action result", result)

        var text = driver.selectFirstPropertyValueOrNull("#searchBox", "value")
        println("Input value of search box: $text")

        step("Action: Extract the full text of the webpage")
        actOptions = ActionOptions("Extract the full text of the webpage")
        result = agent.act(actOptions)
        result("action result", result)

        step("Action: Summarize the full text of the webpage")
        val response = session.chat("Summarize the full text of the webpage: \n\n\n" + result.tcEvalValue)
        result("action result", response)

        AgenticContexts.close()
    }

    private fun step(label: String) = logger.info("[STEP ${++stepNo}] $label")

    private fun result(label: String, value: Any?) {
        val text = Strings.compactLog(value?.toString(), 2000)

        val e = """🟢"""
        printlnPro("$e [RESULT ${stepNo}] $label => $text")
    }
}

suspend fun main() = SummarizePage().run()
