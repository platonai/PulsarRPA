package ai.platon.pulsar.examples.sites.spa.api

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.AbstractEmulateEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private class APIFetcherHandler(
    val initPageNumber: Int,
    val exportDirectory: Path
): AbstractEmulateEventHandler() {
    private val logger = getLogger(this)

    override var verbose = true

    val headersString = """
            accept: application/json, text/plain, */*
            accept-encoding: gzip, deflate, br
            accept-language: zh-CN,zh;q=0.9,en;q=0.8
            origin: https://scope.wemixnetwork.com
            referer: https://scope.wemixnetwork.com/
            sec-ch-ua: " Not A;Brand";v="99", "Chromium";v="100", "Google Chrome";v="100"
            sec-ch-ua-mobile: ?0
            sec-ch-ua-platform: "Linux"
            sec-fetch-dest: empty
            sec-fetch-mode: cors
            sec-fetch-site: same-site
            user-agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36
        """.trimIndent()

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
         ajaxFetch(driver)
        return null
    }

    private suspend fun ajaxFetch(driver: WebDriver) {
        try {
            ajaxFetch0(driver)
        } catch (e: Exception) {
            logger.warn(e.stringify())
        }
    }

    private suspend fun ajaxFetch0(driver: WebDriver) {
        val headers = headersString.split("\n").map { it.trim() }
            .map { it.split(": ") }.associate { it[0] to it[1] }

        val session = driver.newSession()
            .headers(headers)
            .proxy("127.0.0.1", 33857)

        val u = "https://scopi.wemixnetwork.com/api/v1/chain/1003/account/0xcb7615cb4322cddc518f670b4da042dbefc69500/tx"
        IntRange(1, 100).forEach { i ->
            try {
                val pageNo = initPageNumber + i
                val json = session.newRequest()
                    .url(u)
                    .data("page", "$pageNo")
                    .data("pagesize", "20")
                    .execute()
                    .body()

                println(json)
                export(pageNo, json, true)
            } catch (e: Exception) {
                logger.warn(e.stringify("$i.\t"))
            }
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

        val eventHandler = APIFetcherHandler(initPageNumber, reportDirectory)
        val options = session.options("-refresh").apply { addEventHandler(eventHandler) }
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
