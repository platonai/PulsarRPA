package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.skeleton.ai.PerceptiveAgent
import ai.platon.pulsar.test.server.DemoSiteStarter
import kotlinx.coroutines.runBlocking

class SessionAct {
    private val logger = getLogger(this)

    private var stepNo = 0
    private fun step(label: String) { logger.info("[STEP ${++stepNo}] $label") }
    private fun result(label: String, value: Any?) {
        val text = if (value is PerceptiveAgent) {
            Strings.compactLog(value.processTrace.lastOrNull())
        } else {
            Strings.compactLog(value?.toString())
        }

        logger.info("[RESULT ${stepNo}] $label => $text")
    }

    private val session = AgenticContexts.getOrCreateSession(spa = true)

    suspend fun run() {
        // Use local mock site instead of external site so actions are deterministic.
        val url = "http://localhost:18080/generated/tta/act/act-demo.html"
        // one more short wait after potential start (shorter, less verbose)
        val starter = DemoSiteStarter()
        starter.start(url)
        session.registerClosable(starter)

        val driver = session.createBoundDriver()

        step("Open URL: $url")
        var page = session.open(url)
        result("page", page)

        printlnPro(driver.url())

        // Basic action examples (natural language instructions) - now operate on local mock page
        step("Action: search for 'browser'")
        var actOptions = ActionOptions("search for 'browser'")
        var agent = session.act(actOptions)
        result("action result", agent)

        AgenticContexts.close()
    }
}

suspend fun main() = SessionAct().run()
