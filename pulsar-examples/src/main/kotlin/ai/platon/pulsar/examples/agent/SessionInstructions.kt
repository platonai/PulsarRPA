package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.ai.WebDriverAgent
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import kotlinx.coroutines.runBlocking

class SessionInstructionsExample {
    init {
        // Single Page Application
        PulsarSettings.withSPA()
    }

    private var stepNo = 0
    private fun step(label: String) { println("[STEP ${++stepNo}] $label") }
    private fun result(label: String, value: Any?) {
        val text = if (value is WebDriverAgent) {
            value.history.lastOrNull()?.replace("\n", " ")?.take(240)
        } else {
            value?.toString()?.replace("\n", " ")?.take(240)
        }

        println("[RESULT ${stepNo}] $label => $text")
    }

    val context = AgenticContexts.create()
    val session = context.createSession()

    suspend fun run() {
        val driver = context.launchDefaultBrowser().newDriver()
        session.bindDriver(driver)

        val url = "https://www.producthunt.com/"

        step("Open URL: $url")
        var page = session.open(url)
        result("page", page)

        step("Parse opened page")
        var document = session.parse(page)
        result("document", document)

        step("Extract initial fields (title)")
        var fields = session.extract(document, mapOf("title" to "#title"))
        result("fields", fields)

        // Basic action examples (natural language instructions)
        step("Action: search for 'browser'")
        var actOptions = ActionOptions("search for 'browser'")
        var actResult = session.multiAct(actOptions)
        result("action result", actResult)

        step("Capture body text after search (snippet)")
        var content = driver.selectFirstTextOrNull("body")
        result("body snippet", content?.take(160))

        step("Action: click the 3rd link")
        actOptions = ActionOptions("click the 3rd link")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        step("Capture body text after clicking 3rd link (snippet)")
        content = driver.selectFirstTextOrNull("body")
        result("body snippet", content?.take(160))

        // More typical session.act() examples

        // 1) Use the site's search box (example: enter text and submit)
        step("Action: find the search box, type 'web scraping' and submit the form")
        actOptions = ActionOptions("find the search box, type 'web scraping' and submit the form")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        step("Re-attach current URL and parse after search form submission")
        page = session.attach(driver.currentUrl(), driver)
        result("page", page)
        document = session.parse(page)
        result("document", document)

        step("Extract title after search form submission")
        fields = session.extract(document, mapOf("title" to "#title"))
        result("fields", fields)

        // 2) Click a link by visible text
        step("Action: click the first link that contains 'Show HN' or 'Ask HN'")
        actOptions = ActionOptions("click the first link that contains 'Show HN' or 'Ask HN'")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        step("Capture body text after clicking Show/Ask HN link (snippet)")
        content = driver.selectFirstTextOrNull("body")
        result("body snippet", content?.take(160))

        // 3) Scroll to bottom
        step("Action: scroll to the bottom of the page and wait for new content to load")
        actOptions = ActionOptions("scroll to the bottom of the page and wait for new content to load")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        // 4) Open the first comment thread
        step("Action: open the first comment thread on the page")
        actOptions = ActionOptions("open the first comment thread on the page")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        // 5) Navigate back
        step("Action: navigate back")
        actOptions = ActionOptions("navigate back")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        // 5b) Navigate forward
        step("Action: navigate forward")
        actOptions = ActionOptions("navigate forward")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        // 6) Take a screenshot
        step("Action: take a full-page screenshot and save it")
        actOptions = ActionOptions("take a full-page screenshot and save it")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        // 7) Extract specific data after interactions
        step("Action: extract article titles and their hrefs from the main list")
        actOptions = ActionOptions("extract article titles and their hrefs from the main list")
        actResult = session.multiAct(actOptions)
        result("action result", actResult)

        step("Fallback: attach current URL and parse for titles")
        page = session.attach(driver.currentUrl(), driver)
        document = session.parse(page)
        fields = session.extract(document, mapOf("titles" to ".athing .title a"))
        result("fields", fields)

        // add more action examples here

        step("Re-attach original URL and re-parse")
        page = session.attach(url, driver)
        document = session.parse(page)
        fields = session.extract(document, mapOf("title" to "#title"))
        result("fields", fields)

        // Print final values so variables are referenced (avoid unused warnings in IDE/build)
        step("Summary outputs")
        println("Final extracted fields keys: ${fields?.keys}")
        println("Sample page content snippet: ${content?.take(120)}")
        println("Last action result: ${actResult}")
    }
}

fun main() = runBlocking {
    SessionInstructionsExample().run()
}
