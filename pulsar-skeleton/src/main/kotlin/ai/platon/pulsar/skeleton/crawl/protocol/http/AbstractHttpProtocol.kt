package ai.platon.pulsar.skeleton.crawl.protocol.http

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.skeleton.common.MimeTypeResolver
import ai.platon.pulsar.skeleton.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.skeleton.crawl.protocol.Protocol
import ai.platon.pulsar.skeleton.crawl.protocol.ProtocolOutput
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import crawlercommons.robots.BaseRobotRules
import org.slf4j.LoggerFactory
import java.net.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractHttpProtocol : Protocol {
    private val log = LoggerFactory.getLogger(AbstractHttpProtocol::class.java)
    protected val closed = AtomicBoolean()

    val isActive get() = !closed.get() && AppContext.isActive
    override val supportParallel: Boolean = true

    /**
     * The max retry time
     */
    private var fetchMaxRetry = 3

    /**
     * The configuration
     */
    override lateinit var conf: ImmutableConfig

    private lateinit var mimeTypeResolver: MimeTypeResolver

    private lateinit var robots: HttpRobotRulesParser

    /**
     * Set up the protocol.
     * Sometimes the protocol can not to be constructed with parameters, so it need a secondary setup.
     * */
    override fun configure(conf1: ImmutableConfig) {
        conf = conf1
        fetchMaxRetry = conf1.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
        mimeTypeResolver = MimeTypeResolver(conf1)
        robots = HttpRobotRulesParser(conf1)
    }

    override fun reset() {
        // reset proxy, user agent, etc
    }

    override fun getResponses(pages: Collection<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        return pages.takeIf { isActive }
            ?.mapNotNull {
                it.runCatching { getResponse(it, false) }
                    .onFailure { warnInterruptible(this, it) }
                    .getOrNull()
            }
            ?: listOf()
    }

    override fun getProtocolOutput(page: WebPage): ProtocolOutput {
        return try {
            getProtocolOutputWithRetry(page)
        } catch (e: Throwable) {
            // log.warn("Unexpected exception", e)
            warnUnexpected(this, e)
            ProtocolOutput(ProtocolStatus.failed(e))
        }
    }

    @Throws(Exception::class)
    override suspend fun getProtocolOutputDeferred(page: WebPage): ProtocolOutput {
        val startTime = Instant.now()
        val response = getResponseDeferred(page, false)
            ?: return ProtocolOutput(ProtocolStatus.retry(RetryScope.CRAWL, "Null response from protocol"))
        setResponseTime(startTime, page, response)
        return getOutputWithHttpCodeTranslated(page.url, response)
    }

    /**
     * Retrieves the protocol output with retry logic. Retries are only performed if the retry scope is RetryScope.PROTOCOL.
     *
     * @param page The web page for which the protocol output is to be retrieved.
     * @return The protocol output containing the status and response data.
     */
    private fun getProtocolOutputWithRetry(page: WebPage): ProtocolOutput {
        val startTime = Instant.now()
        var response: Response?
        var retry = false
        var lastThrowable: Throwable? = null
        var i = 0

        // Calculate the maximum number of retries, ensuring it does not exceed the maximum retry guard value
        val maxTry = fetchMaxRetry.coerceAtMost(MAX_REY_GUARD)
        do {
            if (i > 0) {
                log.info("Protocol retry: {}/{} | {}", i, maxTry, page.url)
            }

            try {
                // TODO: FETCH_PROTOCOL does not work if the response is a ForwardingResponse
                // Fetch the response and determine if a retry is necessary
                response = getResponse(page, false)
                retry = response == null || shouldRetry(response)
            } catch (e: IllegalApplicationStateException) {
                log.warn(e.message)
                response = null
                lastThrowable = e
            } catch (e: Exception) {
                response = null
                lastThrowable = e
                // log.warn(e.stringify("[Unexpected]"))
                warnUnexpected(this, e)
            } catch (t: Throwable) {
                response = null
                lastThrowable = t
                // log.warn(t.stringify("[Unexpected]"))
                warnUnexpected(this, t)
            }
        } while (retry && ++i < maxTry && isActive)

        // If the system is no longer active, return a canceled status
        if (!isActive) {
            return ProtocolOutput(ProtocolStatus.failed(ProtocolStatusCodes.CANCELED))
        }

        // If the response is null, return a failed response
        if (response == null) {
            return getFailedResponse(lastThrowable, i, maxTry)
        }

        // Set the response time and return the translated protocol output
        setResponseTime(startTime, page, response)
        return getOutputWithHttpCodeTranslated(page.url, response)
    }

    private fun shouldRetry(response: Response): Boolean {
        return response !is ForwardingResponse && response.protocolStatus.isRetry(RetryScope.PROTOCOL)
    }

    private fun getOutputWithHttpCodeTranslated(url: String, response: Response): ProtocolOutput {
        var u = URI(url).toURL()
        val httpCode = response.httpCode
        val pageDatum = response.pageDatum
        val content = pageDatum.content
        // bytes = bytes == null ? EMPTY_CONTENT : bytes;
        val contentType = response.getHeader(HttpHeaders.CONTENT_TYPE)
        pageDatum.contentType = resolveMimeType(contentType, url, content)

        val headers = pageDatum.headers
        val finalProtocolStatus = if (httpCode >= ProtocolStatusCodes.INCOMPATIBLE_CODE_START) {
            response.protocolStatus
        } else {
            ProtocolStatusTranslator.translateHttpCode(httpCode)
        }

        when (httpCode) {
            in 300..399 -> {
                // handle redirect
                // some broken servers, such as MS IIS, use lowercase header name...
                val redirect = response.getHeader("Location") ?: response.getHeader("location") ?: ""
                u = URL(u, redirect)
                finalProtocolStatus.args[ProtocolStatus.ARG_REDIRECT_TO_URL] = u.toString()
            }
        }

        return ProtocolOutput(pageDatum, headers, finalProtocolStatus)
    }

    private fun resolveMimeType(contentType: String?, url: String, data: ByteArray?): String? {
        return mimeTypeResolver.autoResolveContentType(contentType, url, data)
    }

    private fun getFailedResponse(lastThrowable: Throwable?, tryCount: Int, maxRry: Int): ProtocolOutput {
        val code = when (lastThrowable) {
            is ConnectException -> ProtocolStatusCodes.REQUEST_TIMEOUT
            is SocketTimeoutException -> ProtocolStatusCodes.REQUEST_TIMEOUT
            is UnknownHostException -> ProtocolStatusCodes.UNKNOWN_HOST
            else -> ProtocolStatusCodes.EXCEPTION
        }
        val protocolStatus = ProtocolStatus.failed(
            code,
            "exception", lastThrowable,
            "retry", tryCount,
            "maxRetry", maxRry
        )
        return ProtocolOutput(null, MultiMetadata(), protocolStatus)
    }

    private fun setResponseTime(startTime: Instant, page: WebPage, response: Response) {
        val pageFetchMode = page.fetchMode
        val elapsedTime = if (pageFetchMode == FetchMode.BROWSER) {
            val requestTime = response.getHeader(HttpHeaders.Q_REQUEST_TIME)?.toLongOrNull() ?: -1
            val responseTime = response.getHeader(HttpHeaders.Q_RESPONSE_TIME)?.toLongOrNull() ?: -1
            if (requestTime > 0 && responseTime > 0) {
                Duration.ofMillis(responseTime - requestTime)
            } else {
                // Non-positive means an invalid response time which indicates a bug
                Duration.ZERO
            }
        } else {
            Duration.between(startTime, Instant.now())
        }
        // TODO: update in FetchComponent?
        page.metadata[Name.RESPONSE_TIME] = elapsedTime.toString()
    }

    /**
     * Get the protocol response for the given page.
     *
     * @param page The page to get the response for.
     * @param followRedirects Whether to follow redirects.
     * @return The response for the given page.
     * @throws Exception If an error occurs while getting the response.
     */
    @Throws(Exception::class)
    abstract fun getResponse(page: WebPage, followRedirects: Boolean): Response?

    @Throws(Exception::class)
    abstract suspend fun getResponseDeferred(page: WebPage, followRedirects: Boolean): Response?

    override fun getRobotRules(page: WebPage): BaseRobotRules {
        return robots.getRobotRulesSet(this, page.url)
    }

    override fun close() {
        closed.set(true)
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    companion object {
        private const val MAX_REY_GUARD = 10
    }
}
