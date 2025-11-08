package ai.platon.pulsar.examples.fuse

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ActionOptions
import ai.platon.pulsar.test.server.DemoSiteStarter

class FusedActs {

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

        val agent = session.companionAgent
        val driver = session.getOrCreateBoundDriver()

        step("Open URL: $url")
        var page = session.open(url)
        result("page", page)

        step("Parse the page into a lightweight local DOM")
        var document = session.parse(page)
        result("document", document)

        step("Extract fields (title) with CSS selector (no LLM required)")
        var fields = session.extract(document, mapOf("title" to "#title"))
        result("fields", fields)

        // Basic action examples (natural language instructions) - now operate on local mock page
        step("Action: search for 'browser'")
        var actOptions = ActionOptions("search for 'browser'")
        var result = agent.act(actOptions)
        result("action result", result)

        step("Capture body text in the live DOM after search (snippet)")
        var content = driver.selectFirstTextOrNull("body")
        result("body snippet", content?.take(160))

        step("Action: click the 3rd link")
        actOptions = ActionOptions("click the 3rd link")
        result = agent.act(actOptions)
        result("action result", result)

        step("Capture body text in the live DOM after clicking 3rd link (snippet)")
        content = driver.selectFirstTextOrNull("body")
        result("body snippet", content?.take(160))

        step("Action: go back")
        actOptions = ActionOptions("go back")
        result = agent.act(actOptions)
        result("action result", result)

        step("Action: open the 4th link in new tab")
        actOptions = ActionOptions("open the 4th link in new tab")
        result = agent.act(actOptions)
        result("action result", result)

        step("Action: switch to the newly open tab")
        actOptions = ActionOptions("switch to the newly open tab")
        result = agent.act(actOptions)
        result("action result", result)

        // More typical session.act() examples

        // 1) Use the page's search box (enter text and submit)
        step("Action: find the search box, type 'web scraping' and submit the form")
        actOptions = ActionOptions("find the search box, type 'web scraping' and submit the form")
        result = agent.resolve(actOptions)
        result("action result", result)

        step("Captures the live page and parse after search form submission")
        page = session.capture(driver)
        result("page", page)
        document = session.parse(page)
        result("document", document)

        step("Extract title after search form submission")
        fields = session.extract(document, mapOf("title" to "#title"))
        result("fields", fields)

        // 2) Click a link by visible text (Show/Ask HN like titles in mock page)
        step("Action: click the first link that contains 'Show HN' or 'Ask HN'")
        actOptions = ActionOptions("click the first link that contains 'Show HN' or 'Ask HN'")
        result = agent.act(actOptions)
        result("action result", result)

        step("Capture body text after clicking Show/Ask HN link (snippet)")
        content = driver.selectFirstTextOrNull("body")
        result("body snippet", content?.take(160))

        // 3) Scroll to bottom (triggers infinite scroll loading extra items on mock page)
        step("Action: scroll to the bottom of the page and wait for new content to load")
        actOptions = ActionOptions("scroll to the bottom of the page and wait for new content to load")
        result = agent.resolve(actOptions)
        result("action result", result)

        // 4) Open the first comment thread
        step("Action: open the first comment thread on the page")
        actOptions = ActionOptions("open the first comment thread on the page")
        result = agent.act(actOptions)
        result("action result", result)

        // 5) Navigate back
        step("Action: navigate back")
        actOptions = ActionOptions("navigate back")
        result = agent.act(actOptions)
        result("action result", result)

        // 5b) Navigate forward
        step("Action: navigate forward")
        actOptions = ActionOptions("navigate forward")
        result = agent.act(actOptions)
        result("action result", result)

        // 6) Take a screenshot
        step("Action: take a full-page screenshot and save it")
        actOptions = ActionOptions("take a full-page screenshot and save it")
        result = agent.resolve(actOptions)
        result("action result", result)

        // 7) Extract specific data after interactions
        step("Action: extract article titles and their hrefs from the main list")
        actOptions = ActionOptions("extract article titles and their hrefs from the main list")
        result = agent.resolve(actOptions)
        result("action result", result)

        step("Captures the live page as a local copy and parse for titles")
        page = session.capture(driver)
        document = session.parse(page)
        fields = session.extract(document, mapOf("titles" to ".athing .title a"))
        result("fields", fields)

        // add more action examples here

        step("Re-open the original URL and re-parse")
        page = session.open(url)
        document = session.parse(page)
        fields = session.extract(document, mapOf("title" to "#title"))
        result("fields", fields)

        // Print final values so variables are referenced (avoid unused warnings in IDE/build)
        step("Summary outputs")
        logger.info("Final extracted fields keys: ${fields.keys}")
        logger.info("Sample page content snippet: ${content?.take(120)}")
        logger.info("Last action result: $agent")
        logger.info("Agent trace: {}", agent.processTrace.joinToString("\n"))

        session.context.close()
    }

    private fun step(label: String) {
        logger.info("[STEP ${++stepNo}] $label")
    }

    private fun result(label: String, value: Any?) {
        val text = Strings.compactLog(value?.toString())
        logger.info("[RESULT ${stepNo}] $label => $text")
    }

}

suspend fun main() = FusedActs().run()
