package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.proxy.InternalProxyServer
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
        internalProxyServer: InternalProxyServer,
        fetchTaskTracker: FetchTaskTracker,
        metricsSystem: MetricsSystem,
        immutableConfig: ImmutableConfig
): SeleniumEngine(browserControl, driverPool, internalProxyServer, fetchTaskTracker, metricsSystem, immutableConfig) {

    override fun handleTimeout(startTime: Long,
                              pageSource: String, status: ProtocolStatus, page: WebPage, driverConfig: DriverConfig): ProtocolStatus {
        val length = pageSource.length
        // The javascript set data-error flag to indicate if the vision information of all DOM nodes are calculated
        var newStatus = status
        val integrity = checkHtmlIntegrity(pageSource)
        if (integrity.first) {
            newStatus = ProtocolStatus.STATUS_SUCCESS
        }

        if (newStatus.isSuccess) {
            log.info("HTML is integral but {} occurs after {} with {} | {}",
                    status.minorName, DateTimeUtil.elapsedTime(startTime),
                    StringUtil.readableByteCount(length.toLong()), page.url)
        } else {
            log.warn("[INCOMPLETE CONTENT] {} {} | {}", status.minorName, integrity.second, page.url)
            handleWebDriverTimeout(page.url, startTime, pageSource, driverConfig)
        }

        return newStatus
    }

    override fun handleIncompleteContent(task: FetchTask, driver: ManagedWebDriver, e: IncompleteContentException) {
        val proxyEntry = driver.proxyEntry
        val domain = task.domain

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("[INCOMPLETE CONTENT] - driver: {} domain: {}({}) proxy: {} - {}",
                    driver, domain, count, proxyEntry.display, StringUtil.simplifyException(e))
        } else {
            log.warn("[INCOMPLETE CONTENT] - {}", StringUtil.simplifyException(e))
        }

        if (log.isInfoEnabled) {
            val path = AppFiles.export(e.status, e.content, task.page)
            log.info("Incomplete page is exported to $path")
        }

        // delete all cookie, change proxy, and retry
        log.info("Deleting all cookies under {}", domain)
        driver.deleteAllCookiesSilently()
    }

    override fun checkContentIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask) {
        val url = page.url
        val length = pageSource.length.toLong()
        val aveLength = page.aveContentBytes.toLong()
        val readableLength = StringUtil.readableByteCount(length)
        val readableAveLength = StringUtil.readableByteCount(aveLength)

        var message = "Retrieved: (only) $readableLength, " +
                "history average: $readableAveLength, " +
                "might be caused by network/proxy | $url"

        if (length < 100 && pageSource.indexOf("<html") != -1 && pageSource.lastIndexOf("</html>") != -1) {
            throw IncompleteContentException(message, status, pageSource)
        }

        if (page.fetchCount > 0 && aveLength > 100_1000 && length < 0.1 * aveLength) {
            throw IncompleteContentException(message, status, pageSource)
        }

        val stat = task.stat
        if (status.isSuccess && stat != null) {
            val batchAveLength = stat.averagePageSize.roundToLong()
            if (stat.numSuccessTasks > 3 && batchAveLength > 10_000 && length < batchAveLength / 10) {
                val readableBatchAveLength = StringUtil.readableByteCount(batchAveLength)

                message = "Retrieved: (only) $readableLength, " +
                        "history average: $readableAveLength, " +
                        "batch average: $readableBatchAveLength" +
                        "might be caused by network/proxy | $url"

                throw IncompleteContentException(message, status, pageSource)
            }
        }
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
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return false to "NO_JS_OK"
        }

        return true to "OK"
    }
}
