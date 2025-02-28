package ai.platon.pulsar.skeleton.crawl.protocol.http

import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.ProtocolStatus.ARG_HTTP_CODE
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes.REQUEST_TIMEOUT
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil
import org.apache.http.HttpStatus

object ProtocolStatusTranslator {

    fun translateHttpCode(httpCode: Int): ProtocolStatus {
        return when (httpCode) {
            200 -> ProtocolStatus.STATUS_SUCCESS
            304 -> ProtocolStatus.STATUS_NOTMODIFIED
            in 300..399 -> {
                // handle redirect
                // some broken servers, such as MS IIS, use lowercase header name...
                val code = when (httpCode) {
                    HttpStatus.SC_MULTIPLE_CHOICES -> ProtocolStatus.MOVED_PERMANENTLY
                    HttpStatus.SC_MOVED_PERMANENTLY, HttpStatus.SC_USE_PROXY -> ProtocolStatus.MOVED_PERMANENTLY
                    HttpStatus.SC_MOVED_TEMPORARILY, HttpStatus.SC_SEE_OTHER, HttpStatus.SC_TEMPORARY_REDIRECT -> ProtocolStatus.MOVED_TEMPORARILY
                    else -> ProtocolStatus.MOVED_PERMANENTLY
                }
                // handle redirection in the higher layer.
                // page.getMetadata().set(ARG_REDIRECT_TO_URL, url.toString());
                ProtocolStatus.failed(code, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_BAD_REQUEST -> {
                ProtocolStatus.failed(ProtocolStatusCodes.GONE, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_UNAUTHORIZED -> { // requires authorization, but no valid auth provided.
                ProtocolStatus.failed(ProtocolStatusCodes.UNAUTHORIZED, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_NOT_FOUND -> { // GONE
                ProtocolStatus.failed(ProtocolStatusCodes.NOT_FOUND, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_REQUEST_TIMEOUT -> { // TIMEOUT
                ProtocolStatus.failed(REQUEST_TIMEOUT, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_GONE -> { // permanently GONE
                ProtocolStatus.failed(ProtocolStatusCodes.GONE, ARG_HTTP_CODE, httpCode)
            }
            else -> {
                ProtocolStatus.failed(ProtocolStatus.EXCEPTION, ARG_HTTP_CODE, httpCode)
            }
        }
    }

    fun translateToCrawlStatus(protocolStatus: ProtocolStatus, page: WebPage): CrawlStatus {
        return when (protocolStatus.minorCode) {
            ProtocolStatus.SUCCESS_OK -> CrawlStatus.STATUS_FETCHED
            ProtocolStatus.NOT_MODIFIED -> CrawlStatus.STATUS_NOTMODIFIED
            ProtocolStatus.CANCELED -> CrawlStatus.STATUS_UNFETCHED

            ProtocolStatus.MOVED_PERMANENTLY,
            ProtocolStatus.MOVED_TEMPORARILY -> handleMoved(page, protocolStatus)

            ProtocolStatus.UNAUTHORIZED,
            ProtocolStatus.ROBOTS_DENIED,
            ProtocolStatus.UNKNOWN_HOST,
            ProtocolStatus.GONE,
            ProtocolStatus.NOT_FOUND -> CrawlStatus.STATUS_GONE

            ProtocolStatus.EXCEPTION,
            ProtocolStatus.RETRY,
            ProtocolStatus.BLOCKED -> CrawlStatus.STATUS_RETRY

            ProtocolStatus.REQUEST_TIMEOUT,
            ProtocolStatus.THREAD_TIMEOUT,
            ProtocolStatus.WEB_DRIVER_TIMEOUT,
            ProtocolStatus.SCRIPT_TIMEOUT -> CrawlStatus.STATUS_RETRY

            else -> CrawlStatus.STATUS_RETRY
        }
    }

    private fun handleMoved(page: WebPage, protocolStatus: ProtocolStatus): CrawlStatus {
        val url = page.url
        val minorCode = protocolStatus.minorCode

        val temp: Boolean
        val crawlStatus = if (minorCode == ProtocolStatus.MOVED_PERMANENTLY) {
            temp = false
            CrawlStatus.STATUS_REDIR_PERM
        } else {
            temp = true
            CrawlStatus.STATUS_REDIR_TEMP
        }

        val newUrl = protocolStatus.getArgOrElse(ProtocolStatus.ARG_REDIRECT_TO_URL, "")
        if (UrlUtils.isStandard(newUrl)) {
            val reprUrl = InternalURLUtil.chooseRepr(url, newUrl, temp)
            if (UrlUtils.isStandard(reprUrl)) {
                page.reprUrl = reprUrl
            }
        }

        return crawlStatus
    }
}
