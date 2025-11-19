package ai.platon.pulsar.examples.agent

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.test.TestResourceUtil
import org.slf4j.LoggerFactory

internal class RPACrawler(val session: AgenticSession = AgenticContexts.createSession()) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val searchBoxSelector = ".form input[type=text]"
    private val searchBoxSubmit = ".form input[type=submit]"

    val fieldSelectors = mutableMapOf(
        "title" to "#productTitle",
        "reviews" to "#acrCustomerReviewText",
        "prodDetails" to "#prodDetails"
    )

    fun options(args: String): LoadOptions {
        val options = session.options(args)

        val be = options.eventHandlers.browseEventHandlers

        be.onDocumentFullyLoaded.addLast { page, driver ->

        }

        be.onDidInteract.addLast { page, driver ->
            logger.info("Did the interaction")
        }

        return options
    }

    private suspend fun interact(selector: String, driver: WebDriver) {
        val searchBoxSelector = ".form input[type=text]"

        if (driver.exists(selector)) {
            driver.click(selector)
            val text = driver.selectFirstTextOrNull(selector) ?: "no-text"
            driver.type(searchBoxSelector, text.substring(1, 4))
            logger.info("{} clicked", selector)
        }
    }
}

/**
 * Demonstrates how to use RPA for Web scraping.
 * */
fun main() {
    PulsarSettings.withDefaultBrowser()

    val url = TestResourceUtil.PRODUCT_DETAIL_URL
    val args = "-refresh -parse"
    val crawler = RPACrawler()
    val fields = crawler.session.scrape(url, crawler.options(args), crawler.fieldSelectors)
    println(fields)
}
