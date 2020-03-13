package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
class RobustBrowserEmulator(
        privacyContextManager: PrivacyContextManager,
        fetchTaskTracker: FetchTaskTracker,
        metricsSystem: MetricsSystem,
        immutableConfig: ImmutableConfig
): BrowserEmulator(privacyContextManager, fetchTaskTracker, metricsSystem, immutableConfig) {
    companion object {
        private const val SMALL_CONTENT_LIMIT = 1_000_000 / 2 // 500KiB
    }

    private val log = LoggerFactory.getLogger(RobustBrowserEmulator::class.java)!!

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    override fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        val url = page.url
        val length = pageSource.length.toLong()
        val aveLength = page.aveContentBytes.toLong()
        var integrity = HtmlIntegrity.OK

        if (length == 0L) {
            integrity = HtmlIntegrity.EMPTY_0B
        } else if (length == 39L) {
            integrity = HtmlIntegrity.EMPTY_39B
        }

        // might be caused by web driver exception
        if (integrity.isOK && isBlankBody(pageSource)) {
            integrity = HtmlIntegrity.EMPTY_BODY
        }

        if (integrity.isOK && length < SMALL_CONTENT_LIMIT && isRobotCheck(pageSource, page)) {
            integrity = HtmlIntegrity.ROBOT_CHECK
        }

        if (integrity.isOK && isTooSmall(pageSource, page)) {
            integrity = HtmlIntegrity.TOO_SMALL
        }

        if (integrity.isOK && page.fetchCount > 0 && aveLength > SMALL_CONTENT_LIMIT && length < 0.1 * aveLength) {
            integrity = HtmlIntegrity.TOO_SMALL_IN_HISTORY
        }

        val stat = task.stat
        if (integrity.isOK && status.isSuccess && stat != null) {
            val batchAveLength = stat.averagePageSize.roundToLong()
            if (stat.numSuccessTasks > 3 && batchAveLength > 10_000 && length < batchAveLength / 10) {
                integrity = HtmlIntegrity.TOO_SMALL_IN_BATCH

                if (log.isInfoEnabled) {
                    val readableLength = Strings.readableBytes(length)
                    val readableAveLength = Strings.readableBytes(aveLength)
                    val readableBatchAveLength = Strings.readableBytes(batchAveLength)
                    val fetchCount = page.fetchCount
                    val message = "retrieved: $readableLength, batch: $readableBatchAveLength" +
                            " history: $readableAveLength/$fetchCount ($integrity)"
                    log.info(message)
                }
            }
        }

        if (integrity.isOK) {
            integrity = checkHtmlIntegrity(pageSource)
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

    private fun isTooSmall(pageSource: String, page: WebPage): Boolean {
        val length = pageSource.length
        return if (isAmazonItemPage(page)) {
            length < SMALL_CONTENT_LIMIT / 2
        } else {
            length < 100
        }
    }

    private fun isRobotCheck(pageSource: String, page: WebPage): Boolean {
        if (isAmazon(page)) {
            return pageSource.length < 150_000 && pageSource.contains("Type the characters you see in this image")
        }

        return false
    }

    private fun checkHtmlIntegrity(pageSource: String): HtmlIntegrity {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return HtmlIntegrity.OTHER
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return HtmlIntegrity.OTHER
        // no any link, it's broken
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return HtmlIntegrity.NO_ANCHOR

        // TODO: optimize using region match
        val bodyTag = pageSource.substring(p1, p2)
        // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return HtmlIntegrity.NO_JS_OK_FLAG
        }

        return HtmlIntegrity.OK
    }
}
