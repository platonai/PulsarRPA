package ai.platon.pulsar.protocol.browser.emulator.amazon

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.isBlankBody
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulateEventHandler
import org.slf4j.LoggerFactory
import kotlin.math.roundToLong

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class AmazonBrowserEmulateEventHandler(
        driverManager: WebDriverManager,
        messageWriter: MiscMessageWriter,
        immutableConfig: ImmutableConfig
): BrowserEmulateEventHandler(driverManager, messageWriter, immutableConfig) {
    companion object {
        private const val SMALL_CONTENT_LIMIT = 1_000_000 / 2 // 500KiB
    }

    private val log = LoggerFactory.getLogger(AmazonBrowserEmulateEventHandler::class.java)!!

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
//    @Throws(PrivacyLeakException::class)
    override fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        val length = pageSource.length.toLong()
        val aveLength = page.aveContentBytes.toLong()
        var integrity = HtmlIntegrity.OK

        if (length == 0L) {
            integrity = HtmlIntegrity.EMPTY_0B
        } else if (length == 39L) {
            integrity = HtmlIntegrity.EMPTY_39B
        }

        // might be caused by web driver exception
        // TODO: failed to figure out a html have a blank body correctly
        if (integrity.isOK && isBlankBody(pageSource)) {
            integrity = HtmlIntegrity.EMPTY_BODY
//            throw PrivacyLeakException()
        }

        if (integrity.isOK && length < SMALL_CONTENT_LIMIT && isRobotCheck(pageSource, page)) {
            integrity = HtmlIntegrity.ROBOT_CHECK
//            throw PrivacyLeakException()
        }

        if (integrity.isOK && isTooSmall(pageSource, page)) {
            integrity = HtmlIntegrity.TOO_SMALL
        }

        if (integrity.isOK && page.fetchCount > 0 && aveLength > SMALL_CONTENT_LIMIT && length < 0.1 * aveLength) {
            integrity = HtmlIntegrity.TOO_SMALL_IN_HISTORY
        }

        val stat = task.batchStat
        if (integrity.isOK && status.isSuccess && stat != null) {
            val batchAveLength = stat.bytesPerPage.roundToLong()
            if (stat.numTasksSuccess > 3 && batchAveLength > 10_000 && length < batchAveLength / 10) {
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
}
