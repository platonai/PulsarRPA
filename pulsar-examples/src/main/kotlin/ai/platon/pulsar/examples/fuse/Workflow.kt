// kotlin
package ai.platon.pulsar.examples.fuse

import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.test.server.DemoSiteStarter

suspend fun main() {
    val session = AgenticContexts.getOrCreateSession()

    // Use local mock site instead of external site so actions are deterministic.
    val url = "http://localhost:18080/generated/tta/act/act-demo.html"
    // one more short wait after potential start (shorter, less verbose)
    val starter = DemoSiteStarter()
    starter.start(url)
    session.registerClosable(starter)

    val agent = session.companionAgent
    val driver = session.getOrCreateBoundDriver()
    var page = session.open(url)
    var document = session.parse(page)
    var fields = session.extract(document, mapOf("title" to "#title"))
    var result = agent.act("search for 'browser' (RESULTS will display in the same page)")
    var content = driver.selectFirstTextOrNull("body")
    content = driver.selectFirstTextOrNull("body")
    var history = agent.run("find the search box, type 'web scraping' and submit the form (RESULTS will display in the same page)")
    page = session.capture(driver)
    document = session.parse(page)
    fields = session.extract(document, mapOf("title" to "#title"))

    // 2) Click a link by visible text (Show/Ask HN like titles in mock page)
    result = agent.act("click the first link that contains 'Show HN' or 'Ask HN'")

    content = driver.selectFirstTextOrNull("body")

    // 3) Scroll to bottom (triggers infinite scroll loading extra items on mock page)
    history = agent.run("scroll to the bottom of the page and wait for new content to load")

    // 4) Open the first comment thread
    history = agent.run("goto https://www.amazon.com/dp/B08PP5MSVB , search for 'calabi-yau' and submit the form")

    // 5) Navigate back
    result = agent.act("navigate back")

    // 5b) Navigate forward
    result = agent.act("navigate forward")

    // 7) Extract specific data after interactions
    history = agent.run("goto https://en.cppreference.com/index.html , extract first 10 articles with there titles and hrefs from the main list")

    // 5) Navigate back
    result = agent.act("navigate back")

    page = session.capture(driver)
    document = session.parse(page)
    fields = session.extract(document, mapOf("titles" to ".athing .title a"))

    // add more action examples here

    page = session.open(url)
    document = session.parse(page)
    fields = session.extract(document, mapOf("title" to "#title"))

    // Print final values so variables are referenced (avoid unused warnings in IDE/build)

    agent.processTrace.forEach { println("""🚩$it""") }

    session.context.close()
}
