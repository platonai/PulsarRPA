package ai.platon.pulsar.examples

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.PulsarContexts.createSession
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.session.PulsarSession
import org.slf4j.LoggerFactory

internal class RPACrawler(private val session: PulsarSession = createSession()) {
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

        val be = options.event.browseEvent

        be.onDocumentActuallyReady.addLast { page, driver ->
            fieldSelectors.values.forEach { interact(it, driver) }
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
            val text = driver.firstText(selector) ?: "no-text"
            driver.type(searchBoxSelector, text.substring(1, 4))
            logger.info("{} clicked", selector)
        }
    }
}

/**
 * Demonstrates how to use RPA for Web scraping.
 * */
fun main() {
    val url = "https://www.amazon.com/dp/B09V3KXJPB"
    val args = "-refresh -parse"
    val session = createSession()
    val crawler = RPACrawler(session)
    val fields = session.scrape(url, crawler.options(args), crawler.fieldSelectors)
    println(fields)
}
