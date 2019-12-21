/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.common.Urls.getURLOrNull
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.crawl.protocol.Content
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.ProtocolOutput
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.Name
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 17-5-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * Fetch component
 */
@Component
open class FetchComponent(
        val fetchTaskTracker: FetchTaskTracker,
        val protocolFactory: ProtocolFactory,
        val immutableConfig: ImmutableConfig
) : AutoCloseable {

    /**
     * Fetch a url
     *
     * @param url The url to fetch
     * @return The fetch result
     */
    fun fetch(url: String): WebPage {
        return fetchContent(WebPage.newWebPage(url, false))
    }

    /**
     * Fetch a url
     *
     * @param url     The url to fetch
     * @param options The options
     * @return The fetch result
     */
    fun fetch(url: String, options: LoadOptions): WebPage {
        return fetchContent(createFetchEntry(url, options))
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    fun fetchContent(page: WebPage): WebPage {
        return fetchContentInternal(page)
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * If response is null, block and wait for a response
     * @return The fetch result
     */
    protected fun fetchContentInternal(page: WebPage): WebPage {
        val url = page.url
        val u = getURLOrNull(url) ?: return WebPage.NIL
        val protocol = protocolFactory.getProtocol(page)

        if (protocol == null) {
            LOG.warn("No protocol found for {}", url)
            updateStatus(page, CrawlStatus.STATUS_UNFETCHED, ProtocolStatus.STATUS_PROTO_NOT_FOUND)
            return page
        }

        val output = protocol.getProtocolOutput(page)
        return processProtocolOutput(page, output)
    }

    protected fun shouldFetch(url: String): Boolean {
        var code = 0

        if (fetchTaskTracker.isFailed(url)) {
            code = 2
        } else if (fetchTaskTracker.isTimeout(url)) {
            code = 3
        }

        if (code > 0 && LOG.isDebugEnabled) {
            LOG.debug("Not fetching page, reason #{}, url: {}", code, url)
        }

        return code == 0
    }

    protected fun processProtocolOutput(page: WebPage, output: ProtocolOutput): WebPage {
        val url = page.url
        val content = output.content
        if (content == null) {
            LOG.warn("No content for " + page.configuredUrl)
            return page
        }

        val headers = page.headers
        output.headers.asMultimap().entries().forEach { headers.put(it.key, it.value) }
        val protocolStatus = output.status
        val minorCode = protocolStatus.minorCode
        if (protocolStatus.isSuccess) {
            if (ProtocolStatus.NOTMODIFIED == minorCode) {
                updatePage(page, null, protocolStatus, CrawlStatus.STATUS_NOTMODIFIED)
            } else {
                updatePage(page, content, protocolStatus, CrawlStatus.STATUS_FETCHED)
                fetchTaskTracker.trackSuccess(page)
            }
            return page
        }

        if (LOG.isTraceEnabled) {
            LOG.trace("Fetch failed, status: {}, url: {}", protocolStatus, page.configuredUrl)
        }

        when (minorCode) {
            ProtocolStatus.MOVED, ProtocolStatus.TEMP_MOVED -> {
                val crawlStatus = handleTmpMove(page, protocolStatus)
                updatePage(page, content, protocolStatus, crawlStatus)
            }
            ProtocolStatus.RETRY, ProtocolStatus.BLOCKED, ProtocolStatus.CANCELED -> updatePage(page, null, protocolStatus, CrawlStatus.STATUS_RETRY)
            ProtocolStatus.REQUEST_TIMEOUT, ProtocolStatus.THREAD_TIMEOUT, ProtocolStatus.WEB_DRIVER_TIMEOUT, ProtocolStatus.DOCUMENT_READY_TIMEOUT -> {
                fetchTaskTracker.trackTimeout(url)
                updatePage(page, null, protocolStatus, CrawlStatus.STATUS_RETRY)
            }
            ProtocolStatus.UNKNOWN_HOST, ProtocolStatus.GONE, ProtocolStatus.NOTFOUND -> {
                fetchTaskTracker.trackHostGone(url)
                fetchTaskTracker.trackFailed(url)
                updatePage(page, null, protocolStatus, CrawlStatus.STATUS_GONE)
            }
            ProtocolStatus.ACCESS_DENIED, ProtocolStatus.ROBOTS_DENIED -> {
                fetchTaskTracker.trackFailed(url)
                updatePage(page, null, protocolStatus, CrawlStatus.STATUS_GONE)
            }
            ProtocolStatus.WOULDBLOCK -> fetchTaskTracker.trackFailed(url)
            ProtocolStatus.EXCEPTION -> {
                fetchTaskTracker.trackFailed(url)
                LOG.warn("Fetch failed, protocol status: {}", protocolStatus)
                fetchTaskTracker.trackFailed(url)
                LOG.warn("Unknown ProtocolStatus: $protocolStatus")
                updatePage(page, null, protocolStatus, CrawlStatus.STATUS_RETRY)
            }
            else -> {
                fetchTaskTracker.trackFailed(url)
                LOG.warn("Unknown ProtocolStatus: $protocolStatus")
                updatePage(page, null, protocolStatus, CrawlStatus.STATUS_RETRY)
            }
        }

        return page
    }

    private fun handleTmpMove(page: WebPage, protocolStatus: ProtocolStatus): CrawlStatus {
        val crawlStatus: CrawlStatus
        val url = page.url
        val minorCode = protocolStatus.minorCode
        val temp: Boolean
        if (minorCode == ProtocolStatus.MOVED) {
            crawlStatus = CrawlStatus.STATUS_REDIR_PERM
            temp = false
        } else {
            crawlStatus = CrawlStatus.STATUS_REDIR_TEMP
            temp = true
        }

        val newUrl = protocolStatus.getArgOrDefault(ProtocolStatus.ARG_REDIRECT_TO_URL, "")
        if (newUrl.isNotEmpty()) {
            // handleRedirect(url, newUrl, temp, PROTOCOL_REDIR, fetchTask.getPage());
            val reprUrl = URLUtil.chooseRepr(url, newUrl, temp)
            if (reprUrl.length >= AppConstants.SHORTEST_VALID_URL_LENGTH) {
                page.reprUrl = reprUrl
            }
        }
        return crawlStatus
    }

    fun createFetchEntry(originalUrl: String, options: LoadOptions): WebPage {
        val page = WebPage.newWebPage(originalUrl, options.shortenKey, options.volatileConfig)
        page.fetchMode = options.fetchMode
        page.options = options.toString()
        return page
    }

    fun initFetchEntry(page: WebPage, options: LoadOptions): WebPage {
        page.volatileConfig = options.volatileConfig
        page.fetchMode = options.fetchMode
        page.options = options.toString()
        return page
    }

    private fun updatePage(page: WebPage, content: Content?, protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus) {
        updateStatus(page, crawlStatus, protocolStatus)
        if (content != null) {
            updateContent(page, content)
        }
        updateFetchTime(page)
        updateMarks(page)
    }

    override fun close() {}

    companion object {
        @JvmField
        val LOG = LoggerFactory.getLogger(FetchComponent::class.java)

        @JvmStatic
        fun updateStatus(page: WebPage, crawlStatus: CrawlStatus?, protocolStatus: ProtocolStatus?) {
            page.crawlStatus = crawlStatus
            if (protocolStatus != null) {
                page.protocolStatus = protocolStatus
            }
            page.increaseFetchCount()
        }

        @JvmStatic
        fun updateMarks(page: WebPage) {
            val marks = page.marks
            marks.putIfNotNull(Mark.FETCH, marks[Mark.GENERATE])
        }

        @JvmStatic
        fun updateContent(page: WebPage, content: Content) {
            updateContent(page, content, null)
        }

        private fun updateContent(page: WebPage, content: Content, contentTypeHint: String?) {
            var contentType = contentTypeHint

            page.location = content.baseUrl
            page.setContent(content.content)
            if (contentType != null) {
                content.contentType = contentType
            } else {
                contentType = content.contentType
            }

            if (contentType != null) {
                page.contentType = contentType
            } else {
                LOG.error("Failed to determine content type!")
            }
        }

        @JvmStatic
        @JvmOverloads
        fun updateFetchTime(page: WebPage, newFetchTime: Instant = Instant.now()) {
            val lastFetchTime = page.fetchTime
            if (lastFetchTime.isBefore(newFetchTime)) {
                page.prevFetchTime = lastFetchTime
            }
            page.fetchTime = newFetchTime
            page.putFetchTimeHistory(newFetchTime)
        }

        fun getFetchCompleteReport(page: WebPage): String {
            val bytes = page.contentBytes
            if (bytes < 0) {
                return ""
            }

            val responseTime = page.metadata[Name.RESPONSE_TIME]
            val proxy = page.metadata[Name.PROXY]
            val jsData = page.browserJsData
            var jsSate = ""
            if (jsData != null) {
                val (ni, na, nnm, nst) = jsData.lastStat
                jsSate = String.format(" i/a/nm/st:%d/%d/%d/%d", ni, na, nnm, nst)
            }

            val redirected = page.url != page.baseUrl
            val url = if (redirected) page.baseUrl else page.url
            val mark = page.pageCategory.symbol()
            val fmt = "Fetched %s %s in %8s" + (if (proxy == null) "%s" else "%26s") + ", fc:%2d %24s | %s"
            return String.format(fmt,
                    mark,
                    StringUtil.readableByteCount(bytes.toLong(), 7, false),
                    DateTimeUtil.readableDuration(responseTime),
                    if (proxy == null) "" else " via $proxy",
                    page.fetchCount,
                    jsSate,
                    if (redirected) "[R] $url" else url
            )
        }

        fun getBatchCompleteReport(pages: Collection<WebPage>, startTime: Instant): StringBuilder {
            val elapsed = DateTimeUtil.elapsedTime(startTime)
            val message = String.format("Fetched total %d pages in %s:\n", pages.size, DateTimeUtil.readableDuration(elapsed))
            val sb = StringBuilder(message)
            val i = AtomicInteger()
            pages.forEach { sb.append(i.incrementAndGet()).append(".\t").append(getFetchCompleteReport(it)).append('\n') }
            return sb
        }
    }
}
