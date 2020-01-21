package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.proxy.InternalProxyServer
import org.slf4j.LoggerFactory
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
class RobustSeleniumEngine(
        browserControl: BrowserControl,
        driverPool: WebDriverPool,
        ips: InternalProxyServer,
        fetchTaskTracker: FetchTaskTracker,
        metricsSystem: MetricsSystem,
        immutableConfig: ImmutableConfig
): SeleniumEngine(browserControl, driverPool, ips, fetchTaskTracker, metricsSystem, immutableConfig) {
    private val log = LoggerFactory.getLogger(RobustSeleniumEngine::class.java)!!

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be applied after field
     * extraction.
     * */
    override fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        val url = page.url
        val length = pageSource.length.toLong()
        val aveLength = page.aveContentBytes.toLong()
        val readableLength = StringUtil.readableByteCount(length)
        val readableAveLength = StringUtil.readableByteCount(aveLength)
        var integrity = HtmlIntegrity.OK
        var message = ""

        if (length == 0L) {
            integrity = HtmlIntegrity.EMPTY
        }

        // might be caused by web driver exception
        if (integrity.isOK && isEmptyBody(pageSource)) {
            integrity = HtmlIntegrity.EMPTY_BODY
        }

        if (integrity.isOK && length < 1_000_000 && isAmazonRobotCheck(pageSource, page)) {
            integrity = HtmlIntegrity.ROBOT_CHECK
        }

        if (integrity.isOK && length < 1_000_000 && isAmazonItemPage(page)) {
            integrity = HtmlIntegrity.TOO_SMALL
        }

        if (integrity.isOK && page.fetchCount > 0 && aveLength > 1_000_000 && length < 0.1 * aveLength) {
            integrity = HtmlIntegrity.TOO_SMALL_IN_HISTORY
        }

        val stat = task.stat
        if (integrity.isOK && status.isSuccess && stat != null) {
            val batchAveLength = stat.averagePageSize.roundToLong()
            if (stat.numSuccessTasks > 3 && batchAveLength > 10_000 && length < batchAveLength / 10) {
                val readableBatchAveLength = StringUtil.readableByteCount(batchAveLength)
                val fetchCount = page.fetchCount
                message = "retrieved: $readableLength, batch: $readableBatchAveLength" +
                        " history: $readableAveLength/$fetchCount ($integrity)"

                integrity = HtmlIntegrity.TOO_SMALL_IN_BATCH
            }
        }

        if (integrity.isOK) {
            integrity = checkHtmlIntegrity(pageSource)
        }

        if (integrity.isNotOK) {
            if (message.isEmpty()) {
                val fetchCount = page.fetchCount
                message = "retrieved: $readableLength, history: $readableAveLength/$fetchCount ($integrity)"
            }

            logIncompleteContentPage(task, message)
        }

        return integrity
    }

    private fun isAmazon(page: WebPage): Boolean {
        return page.url.contains("amazon.com")
    }

    /**
     * Site specific, should be moved to a better place
     * */
    private fun isAmazonItemPage(page: WebPage): Boolean {
        return isAmazon(page) && page.url.contains("/dp/")
    }

    private fun isAmazonRobotCheck(pageSource: String, page: WebPage): Boolean {
        //
        return isAmazon(page) && pageSource.length < 150_000 && pageSource.contains("Type the characters you see in this image")
    }

    private fun checkHtmlIntegrity(pageSource: String): HtmlIntegrity {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return HtmlIntegrity.NO_BODY_START
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return HtmlIntegrity.NO_BODY_END
        // no any link, it's broken
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return HtmlIntegrity.NO_ANCHOR

        // TODO: optimize using region match
        val bodyTag = pageSource.substring(p1, p2)
        // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return HtmlIntegrity.NO_JS_OK
        }

        return HtmlIntegrity.OK
    }

    private fun logIncompleteContentPage(task: FetchTask, message: String) {
        val proxyEntry = task.proxyEntry
        val domain = task.domain
        val link = AppPaths.symbolicLinkFromUri(task.url)

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("Page broken | {} | domain: {}({}) proxy: {} | {} | {}",
                    message, domain, count, proxyEntry.display, link, task.url)
        } else {
            log.warn("Page broken | {} | {} | {}", message, link, task.url)
        }
    }
}
