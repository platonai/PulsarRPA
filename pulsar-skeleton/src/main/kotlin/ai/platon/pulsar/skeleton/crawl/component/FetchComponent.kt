package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.model.GoraWebPage
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.common.persist.ext.loadEventHandlers
import ai.platon.pulsar.skeleton.common.persist.ext.options
import ai.platon.pulsar.skeleton.crawl.CoreMetrics
import ai.platon.pulsar.skeleton.crawl.GlobalEventHandlers
import ai.platon.pulsar.skeleton.crawl.common.FetchEntry
import ai.platon.pulsar.skeleton.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.skeleton.crawl.protocol.ProtocolNotFound
import ai.platon.pulsar.skeleton.crawl.protocol.ProtocolOutput
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
    private val abnormalPage get() = GoraWebPage.NIL.takeIf { !isActive }

    /**
     * Fetch an url
     *
     * @param url The url of web page to fetch
     * @return The fetch result
     */
    @Throws(Exception::class)
    fun fetch(url: String) =
        abnormalPage ?: fetchContent(GoraWebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

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
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND) }
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
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND) }
        } finally {
            onFetched(page)
        }
    }

    private fun onWillFetch(page: WebPage) {
        try {
            GlobalEventHandlers.pageEventHandlers?.loadEventHandlers?.onWillFetch?.invoke(page)
            // The more specific handlers has the opportunity to override the result of more general handlers.
            page.loadEventHandlers?.onWillFetch?.invoke(page)
        } catch (e: Throwable) {
            logger.warn("Failed to invoke onWillFetch | ${page.configuredUrl}", e)
        }
    }

    private fun onFetched(page: WebPage) {
        try {
            GlobalEventHandlers.pageEventHandlers?.loadEventHandlers?.onFetched?.invoke(page)
            // The more specific handlers has the opportunity to override the result of more general handlers.
            page.loadEventHandlers?.onFetched?.invoke(page)
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

        return page
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }

    companion object {
        fun updateStatus(page: WebPage, protocolStatus: ProtocolStatus) {
            page.protocolStatus = protocolStatus
            ++page.fetchCount
        }
    }
}
