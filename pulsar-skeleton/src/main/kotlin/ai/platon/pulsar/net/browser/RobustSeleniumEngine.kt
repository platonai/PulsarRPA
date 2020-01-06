package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.files.ext.export
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
     * Timeout when browse the page, it can be caused by javascript execution timeout, or sub resource loading timeout,
     * the page can be good in such case, if so, the status can be modified to be success.
     * */
    override fun handleBrowseTimeout(startTime: Long, pageSource: String, status: ProtocolStatus, page: WebPage, driverConfig: DriverConfig): ProtocolStatus {
        val length = pageSource.length
        var newStatus = status
        val result = checkHtmlIntegrity(pageSource)
        if (result.first) {
            newStatus = ProtocolStatus.STATUS_SUCCESS
        }

        if (newStatus.isSuccess) {
            log.info("DOM is good though {} after {} with {} | {}",
                    status.minorName, DateTimeUtil.elapsedTime(startTime),
                    StringUtil.readableByteCount(length.toLong()), page.url)
        } else {
            val link = AppPaths.symbolicLinkFromUri(page.url)
            log.info("DOM is bad {} after {} with {} | file://{}",
                    status.minorName, DateTimeUtil.elapsedTime(startTime),
                    StringUtil.readableByteCount(length.toLong()), link)
        }

        return newStatus
    }

    override fun checkPageSource(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): Int {
        val url = page.url
        val length = pageSource.length.toLong()
        val aveLength = page.aveContentBytes.toLong()
        val readableLength = StringUtil.readableByteCount(length)
        val readableAveLength = StringUtil.readableByteCount(aveLength)
        var code = 0

        var message = "Retrieved: (only) $readableLength, " +
                "history average: $readableAveLength, " +
                "might be caused by network/proxy | $url"

        if (length < 100 && pageSource.indexOf("<html") != -1 && pageSource.lastIndexOf("</html>") != -1) {
            code = 1
            handleIncompleteContent(task, message)
            // throw IncompleteContentException(message, status, pageSource)
        }

        if (code == 0 && page.fetchCount > 0 && aveLength > 100_1000 && length < 0.1 * aveLength) {
            handleIncompleteContent(task, message)
            code = 2
        }

        val stat = task.stat
        if (code == 0 && status.isSuccess && stat != null) {
            val batchAveLength = stat.averagePageSize.roundToLong()
            if (stat.numSuccessTasks > 3 && batchAveLength > 10_000 && length < batchAveLength / 10) {
                val readableBatchAveLength = StringUtil.readableByteCount(batchAveLength)

                message = "Retrieved: (only) $readableLength, " +
                        "history average: $readableAveLength, " +
                        "batch average: $readableBatchAveLength" +
                        "might be caused by network/proxy | $url"

                handleIncompleteContent(task, message)

                code = 3
                // throw IncompleteContentException(message, status, pageSource)
            }
        }

        if (code != 0) {
            log.info(message)
        }

        return code
    }

    private fun checkHtmlIntegrity(pageSource: String): Pair<Boolean, String> {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return false to "NO_BODY_START"
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return false to "NO_BODY_END"
        // no any link, it's incomplete
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return false to "NO_ANCHOR"

        // TODO: optimization using region match
        val bodyTag = pageSource.substring(p1, p2)
        // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return false to "NO_JS_OK"
        }

        return true to "OK"
    }
}
