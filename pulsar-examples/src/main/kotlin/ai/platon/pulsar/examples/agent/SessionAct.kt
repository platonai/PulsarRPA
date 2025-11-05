package ai.platon.pulsar.examples.agent

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
    private fun step(label: String) {
        logger.info("[STEP ${++stepNo}] $label")
    }

    private fun result(label: String, value: Any?) {
        val text = if (value is PerceptiveAgent) {
            value.processTrace.joinToString("\n") { Strings.compactLog(it, 200) }
        } else {
            Strings.compactLog(value?.toString(), 2000)
        }

        logger.info("[RESULT ${stepNo}] $label => $text")
    }

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
        agent.act(actOptions)
        result("action result", agent)

        var text = driver.selectFirstPropertyValueOrNull("#searchBox", "value")
        println("Input value of search box: $text")

        AgenticContexts.close()
    }
}

suspend fun main() = SessionAct().run()
