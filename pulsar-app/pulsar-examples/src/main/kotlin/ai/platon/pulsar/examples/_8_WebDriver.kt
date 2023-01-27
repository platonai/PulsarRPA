package ai.platon.pulsar.examples

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.PulsarContexts.createSession
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.session.PulsarSession
import org.slf4j.LoggerFactory

internal class WebDriverDemo(private val session: PulsarSession = createSession()) {
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
            fieldSelectors.values.forEach { interact1(it, driver) }
        }

        be.onDocumentActuallyReady.addLast { page, driver ->
            interact2(driver)
        }

        be.onDidInteract.addLast { page, driver ->
            logger.info("Did the interaction")
        }

        return options
    }

    private suspend fun interact1(selector: String, driver: WebDriver) {
        if (driver.exists(selector)) {
            println("click $selector ...")
            driver.click(selector)

            println("select first text by $selector ...")
            var text = driver.firstText(selector) ?: "no-text"
            text = text.substring(1, 4)

            println("type $text in $searchBoxSelector ...")
            driver.type(searchBoxSelector, text)
        }
    }

    private suspend fun interact2(driver: WebDriver) {
        val selector = "#productTitle"

        println("bring the page to front ...")
        driver.bringToFront()

        println("scroll to the bottom of the page ...")
        driver.scrollToBottom()
        println("bounding box of body: " + driver.boundingBox("body"))

        println("scroll to the middle of the page ...")
        driver.scrollToMiddle(0.5f)

        println("click $selector ...")
        driver.click(selector)

        println("query text of $selector ...")
        var text = driver.firstText(selector) ?: "no-text"
        text = text.substring(1, 4)
        println("type `$text` in $searchBoxSelector")
        driver.type(searchBoxSelector, text)

        println("capture screenshot over $selector ...")
        driver.captureScreenshot(selector)

        println("evaluate 1 + 1 ...")
        val result = driver.evaluate("1 + 1")
        require(result is Int)
        println("evaluate 1 + 1 returns $result")

        println("wheel down for 5 times ...")
        driver.mouseWheelDown(5, delayMillis = 2000)

        println("scroll to top ...")
        driver.mouseWheelDown(5, delayMillis = 2000)
        driver.scrollToTop()

        println("search ...")
        text = "Vincent Willem van Gogh"
        println("type `$text` in $searchBoxSelector")
        driver.type(searchBoxSelector, text)
        driver.click(searchBoxSubmit)
        val url = driver.currentUrl()
        println("page navigated to $url")
    }
}

/**
 * Demonstrates how to use web driver to interact with the page.
 * */
fun main() {
    // Set the system to work with single page applications (SPA)
    BrowserSettings.withSPA()

    val url = "https://www.amazon.com/dp/B09V3KXJPB"
    val args = "-refresh -parse"
    val session = createSession()
    val crawler = WebDriverDemo(session)
    session.load(url, crawler.options(args))
}
