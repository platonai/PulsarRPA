package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import kotlinx.coroutines.runBlocking

class SessionInstructionsExample {
    init {
        // Single Page Application
        PulsarSettings.withSPA()
    }

    val context = AgenticContexts.create()
    val session = context.createSession()

    suspend fun run() {
        val driver = context.launchDefaultBrowser().newDriver()
        session.bindDriver(driver)

        val url = "https://www.producthunt.com/"

        var page = session.open(url)
        var document = session.parse(page)
        var fields = session.extract(document, mapOf("title" to "#title"))

        // Basic action examples (natural language instructions)
        var actOptions = ActionOptions("search for 'browser'")
        var actResult = session.act(actOptions)
        var content = driver.selectFirstTextOrNull("body")

        actOptions = ActionOptions("click the 3rd link")
        actResult = session.act(actOptions)
        content = driver.selectFirstTextOrNull("body")

        // More typical session.act() examples

        // 1) Use the site's search box (example: enter text and submit)
        // The agent may find the search input and submit button automatically.
        actOptions = ActionOptions("find the search box, type 'web scraping' and submit the form")
        actResult = session.act(actOptions)
        // re-parse and extract after the action
        page = session.attach(driver.currentUrl(), driver)
        document = session.parse(page)
        fields = session.extract(document, mapOf("title" to "#title"))

        // 2) Click a link by visible text
        actOptions = ActionOptions("click the first link that contains 'Show HN' or 'Ask HN'")
        actResult = session.act(actOptions)
        content = driver.selectFirstTextOrNull("body")

        // 3) Scroll to bottom to load more content (useful for infinite-scroll pages)
        actOptions = ActionOptions("scroll to the bottom of the page and wait for new content to load")
        actResult = session.act(actOptions)

        // 4) Open the first comment thread (if any)
        actOptions = ActionOptions("open the first comment thread on the page")
        actResult = session.act(actOptions)

        // 5) Navigate back and forward
        actOptions = ActionOptions("navigate back")
        actResult = session.act(actOptions)

        actOptions = ActionOptions("navigate forward")
        actResult = session.act(actOptions)

        // 6) Take a screenshot (agent may expose a screenshot action)
        actOptions = ActionOptions("take a full-page screenshot and save it")
        actResult = session.act(actOptions)

        // 7) Extract specific data after interactions
        actOptions = ActionOptions("extract article titles and their hrefs from the main list")
        actResult = session.act(actOptions)
        // you can still use the session extractor as a fallback
        page = session.attach(driver.currentUrl(), driver)
        document = session.parse(page)
        fields = session.extract(document, mapOf("titles" to ".athing .title a"))

        // add more action examples here


        page = session.attach(url, driver)
        document = session.parse(page)
        fields = session.extract(document, mapOf("title" to "#title"))

        // Print final values so variables are referenced (avoid unused warnings in IDE/build)
        println("Final extracted fields keys: ${fields?.keys}")
        println("Sample page content snippet: ${content?.take(120)}")
        println("Last action result: ${actResult}")
    }
}

fun main() = runBlocking {
    SessionInstructionsExample().run()
}
