package ai.platon.pulsar.examples.sites.spa

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.AbstractWebPageWebDriverHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.random.Random

private class PaginateHandler(
    val initPageNumber: Int,
    val exportDirectory: Path
) : AbstractWebPageWebDriverHandler() {
    private val logger = getLogger(this)

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        return onAfterCheckDOMState0(page, driver)
    }

    private suspend fun onAfterCheckDOMState0(page: WebPage, driver: WebDriver): Any? {
        driver.waitForSelector("#tab-transactions")
        driver.click("#tab-transactions")

        pageDownTo(driver)

        logger.info("Report to: file://$exportDirectory")
        IntRange(1, 100).forEach { i ->
            roundGap(i)

            var text = driver.firstText(".table__list table.table__list-set tr:nth-child(25)") ?: ""
            println(text)
            if (text.length < 100) {
                delay(3000)
            }

            text = driver.outerHTML(".table__list table.table__list-set") ?: ""
            export(i, text)

            val nthChild = if (initPageNumber == 1 || i <= 6) i else 6
            val nextPageSelector = "ul.el-pager li:nth-child($nthChild)"
            driver.click(nextPageSelector)
        }

        return null
    }

    private suspend fun pageDownTo(driver: WebDriver) {
        repeat(initPageNumber / 6) { i ->
            driver.click(".btn-quicknext")
            roundGap(i)
        }
    }

    private fun prepareFiles(path: Path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path.parent)
            Files.createFile(path)
        }
    }

    private fun export(i: Int, text: String, isJson: Boolean = false) {
        val postfix = if (isJson) ".json" else ".html"
        val timestamp = System.currentTimeMillis()
        val fileName = "transaction.b${initPageNumber}.t$timestamp.p$i$postfix"
        val file = exportDirectory.resolve(fileName)
        prepareFiles(file)
        Files.writeString(file, text, StandardOpenOption.APPEND)
    }

    private suspend fun roundGap(i: Int) {
        val delaySeconds = when {
            i % 20 == 0 -> 20 + Random.nextInt(10)
            i % 100 == 0 -> 60 + Random.nextInt(10)
            else -> 3
        }
        delay(1000 * (1L + delaySeconds))
    }
}

private class WemixCrawler(
    var initPageNumber: Int = 1,
    val session: PulsarSession
) {
    private val logger = getLogger(this)

    private val url = "https://scope.wemixnetwork.com/1003/token/0xcb7615cb4322cddc518f670b4da042dbefc69500"

    val reportDirectory = AppPaths.REPORT_DIR
        .resolve("wemix")
        .resolve("b$initPageNumber")

    /**
     * Crawl a single page application
     * */
    fun crawlSPA() {
        BrowserSettings.withSPA()

        if (Files.exists(reportDirectory)) {
            return
        }

        val paginateHandler = PaginateHandler(initPageNumber, reportDirectory)
        val options = session.options("-refresh")
        options.eventHandler?.simulateEventPipelineHandler?.onAfterCheckDOMStatePipeline?.addLast(paginateHandler)
        try {
            session.load(url, options)
        } catch (e: Exception) {
            logger.warn("Unexpected exception", e)
        }
    }
}

fun main() {
    val session = PulsarContexts.createSession()

    IntRange(1, 80).forEach { i ->
        val crawler = WemixCrawler(100 * i, session)
        crawler.crawlSPA()
    }
}
