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

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.persist.ext.loadEvent
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.common.FetchEntry
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.ProtocolNotFound
import ai.platon.pulsar.crawl.protocol.ProtocolOutput
import ai.platon.pulsar.crawl.protocol.http.ProtocolStatusTranslator
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The fetch component is the portal to fetch the content of pages.
 */
open class FetchComponent(
    val coreMetrics: CoreMetrics? = null,
    val protocolFactory: ProtocolFactory,
    val immutableConfig: ImmutableConfig,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(FetchComponent::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get() && AppContext.isActive
    private val abnormalPage get() = WebPage.NIL.takeIf { !isActive }

    /**
     * Fetch an url
     *
     * @param url The url of web page to fetch
     * @return The fetch result
     */
    @Throws(Exception::class)
    fun fetch(url: String) = abnormalPage ?: fetchContent(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    /**
     * Fetch an url
     *
     * @param url The url of web page to fetch
     * @param options The options
     * @return The fetch result
     */
    @Throws(Exception::class)
    fun fetch(url: String, options: LoadOptions) = abnormalPage ?: fetchContent0(FetchEntry(url, options))

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    @Throws(Exception::class)
    fun fetchContent(page: WebPage) = abnormalPage ?: fetchContent0(FetchEntry(page, page.options))

    /**
     * Fetch a page
     *
     * @param fetchEntry The fetch entry
     * @return The fetch result
     */
    @Throws(Exception::class)
    fun fetchContent(fetchEntry: FetchEntry) = abnormalPage ?: fetchContent0(fetchEntry)

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    @Throws(Exception::class)
    suspend fun fetchContentDeferred(page: WebPage) = abnormalPage ?: fetchContentDeferred0(page)

    /**
     * Fetch a page
     *
     * @param fetchEntry The fetch entry
     * @return The fetched webpage
     */
    @Throws(Exception::class)
    protected fun fetchContent0(fetchEntry: FetchEntry): WebPage {
        val page = fetchEntry.page
        require(page.isNotInternal) { "Internal page ${page.url}" }

        coreMetrics?.markFetchTaskStart()
        onWillFetch(page)

        return try {
            val protocol = protocolFactory.getProtocol(page)
            processProtocolOutput(page, protocol.getProtocolOutput(page))
        } catch (e: ProtocolNotFound) {
            logger.warn(e.message)
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND, CrawlStatus.STATUS_UNFETCHED) }
        } finally {
            onFetched(page)
        }
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    @Throws(Exception::class)
    protected suspend fun fetchContentDeferred0(page: WebPage): WebPage {
        return try {
            onWillFetch(page)

            coreMetrics?.markFetchTaskStart()
            val protocol = protocolFactory.getProtocol(page)
            processProtocolOutput(page, protocol.getProtocolOutputDeferred(page))
        } catch (e: ProtocolNotFound) {
            logger.warn(e.message)
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND, CrawlStatus.STATUS_UNFETCHED) }
        } finally {
            onFetched(page)
        }
    }

    private fun onWillFetch(page: WebPage) {
        try {
            page.loadEvent?.onWillFetch?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke onWillFetch | ${page.configuredUrl}", e)
        }
    }

    private fun onFetched(page: WebPage) {
        try {
            page.loadEvent?.onFetched?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke onFetched | ${page.configuredUrl}", e)
        }
    }

    protected fun processProtocolOutput(page: WebPage, output: ProtocolOutput): WebPage {
        val protocolStatus = output.protocolStatus
        if (protocolStatus.isCanceled) {
            page.isCanceled = true
            return page
        }

        val pageDatum = output.pageDatum

        if (pageDatum == null) {
            logger.warn("No content | {}", page.configuredUrl)
        }
        page.isFetched = true

        page.headers.putAll(output.headers.asMultimap())

        val crawlStatus = ProtocolStatusTranslator.translateToCrawlStatus(protocolStatus, page)

        when (crawlStatus) {
            CrawlStatus.STATUS_FETCHED,
            CrawlStatus.STATUS_REDIR_TEMP,
            CrawlStatus.STATUS_REDIR_PERM -> updateFetchedPage(page, pageDatum, protocolStatus, crawlStatus)
            else -> updateFetchedPage(page, null, protocolStatus, crawlStatus)
        }

        coreMetrics?.let { logMetrics(crawlStatus, page, it) }

        return page
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }

    private fun updateFetchedPage(
        page: WebPage, pageDatum: PageDatum?,
        protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus,
    ): WebPage {
        val pageExt = WebPageExt(page)
        updateStatus(page, protocolStatus, crawlStatus)

        pageDatum?.also {
            page.location = it.location
            page.proxy = it.proxyEntry?.agentIp
            val trace = it.activeDOMStatTrace
            if (trace != null) {
                page.activeDOMStatus = trace.status
                page.activeDOMStatTrace = mapOf(
                    "initStat" to trace.initStat, "initD" to trace.initD,
                    "lastStat" to trace.lastStat, "lastD" to trace.lastD
                )
            }

            it.pageCategory?.let { page.setPageCategory(it) }
            it.htmlIntegrity?.let { page.htmlIntegrity = it }
            it.lastBrowser?.let { page.lastBrowser = it }

            if (protocolStatus.isSuccess) {
                // good! persists content for only success pages
                pageExt.updateContent(it)
            }
        }

        updateMarks(page)

        return page
    }

    private fun logMetrics(crawlStatus: CrawlStatus, page: WebPage, metrics: CoreMetrics) {
        val url = page.url
        when (crawlStatus) {
            CrawlStatus.STATUS_REDIR_PERM,
            CrawlStatus.STATUS_REDIR_TEMP -> metrics.trackMoved(url)
            CrawlStatus.STATUS_GONE -> metrics.trackHostUnreachable(url)
        }

        if (crawlStatus.isFetched) {
            metrics.trackSuccess(page)
        } else if (crawlStatus.isFailed) {
            metrics.trackFailedUrl(url)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FetchComponent::class.java)

        fun updateStatus(page: WebPage, protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus) {
            page.crawlStatus = crawlStatus
            page.protocolStatus = protocolStatus
            page.updateFetchCount()
        }

        fun updateMarks(page: WebPage) {
            val marks = page.marks
            marks.putIfNotNull(Mark.FETCH, marks[Mark.GENERATE])
        }
    }
}
