package ai.platon.pulsar.examples.llm

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.test.server.DemoSiteStarter

class SessionAct {
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

        // Basic action examples (natural language instructions) - now operate on local mock page
        step("Action: search for 'browser'")
        var actOptions = ActionOptions("search for 'browser'")
        var result = agent.act(actOptions)
        result("action result", result)

        var text = driver.selectFirstPropertyValueOrNull("#searchBox", "value")
        println("Input value of search box: $text")

        step("Action: 提取网页全文")
        actOptions = ActionOptions("提取网页全文")
        result = agent.act(actOptions)
        result("action result", result)

        step("Action: 总结网页全文")
        actOptions = ActionOptions("总结网页全文: \n\n\n" + result.tcEvalValue)
        result = agent.act(actOptions)
        result("action result", result)

        step("Action: 根据截图总结网页全文")
        actOptions = ActionOptions("根据截图总结网页全文")
        result = agent.act(actOptions)
        result("action result", result)

        AgenticContexts.close()
    }

    private fun step(label: String) = logger.info("[STEP ${++stepNo}] $label")

    private fun result(label: String, value: Any?) {
        val text = Strings.compactLog(value?.toString(), 2000)

        printlnPro("[RESULT ${stepNo}] $label => $text")
    }
}

suspend fun main() = SessionAct().run()
