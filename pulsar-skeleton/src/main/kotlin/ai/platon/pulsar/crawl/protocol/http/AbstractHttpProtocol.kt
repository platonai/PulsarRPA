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
package ai.platon.pulsar.crawl.protocol.http


import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Protocol
import ai.platon.pulsar.crawl.protocol.ProtocolOutput
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import crawlercommons.robots.BaseRobotRules
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import java.net.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractHttpProtocol: Protocol {
    private val log = LoggerFactory.getLogger(AbstractHttpProtocol::class.java)
    protected val closed = AtomicBoolean()
    val isActive get() = !closed.get()
    /**
     * The max retry time
     */
    private var fetchMaxRetry = 3
    /**
     * The configuration
     */
    private lateinit var conf: ImmutableConfig

    private lateinit var mimeTypeResolver: MimeTypeResolver

    private lateinit var robots: HttpRobotRulesParser

    override fun getConf(): ImmutableConfig = conf

    override fun setConf(jobConf: ImmutableConfig) {
        conf = jobConf
        fetchMaxRetry = jobConf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
        mimeTypeResolver = MimeTypeResolver(jobConf)
        robots = HttpRobotRulesParser(jobConf)
    }

    override fun reset() {
        // reset proxy, user agent, etc
    }

    override fun getResponses(pages: Collection<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        return pages.takeIf { isActive }
                ?.mapNotNull { it.runCatching { getResponse(it, false) }
                        .onFailure { log.warn(it.message) }.getOrNull() }
                ?: listOf()
    }

    override fun getProtocolOutput(page: WebPage): ProtocolOutput {
        return try {
            getProtocolOutputWithRetry(page)
        } catch (e: Throwable) {
            log.warn("Unexpected exception", e)
            ProtocolOutput(ProtocolStatus.failed(e))
        }
    }

    override suspend fun getProtocolOutputDeferred(page: WebPage): ProtocolOutput {
        val startTime = Instant.now()
        val response = getResponseDeferred(page, false)
                ?:return ProtocolOutput(ProtocolStatus.retry(RetryScope.CRAWL))
        setResponseTime(startTime, page, response)
        return getOutputWithHttpStatusTransformed(page.url, response)
    }

    private fun getProtocolOutputWithRetry(page: WebPage): ProtocolOutput {
        val startTime = Instant.now()
        var response: Response?
        var retry = false
        var lastThrowable: Throwable? = null
        var i = 0
        val maxTry = fetchMaxRetry.coerceAtMost(MAX_REY_GUARD)
        do {
            if (i > 0) {
                log.info("Protocol retry: {}/{} | {}", i, maxTry, page.url)
            }
            try {
                // TODO: FETCH_PROTOCOL does not work if the response is a ForwardingResponse
                response = getResponse(page, false)
                retry = response == null || shouldRetry(response)
            } catch (e: Throwable) {
                response = null
                lastThrowable = e
                log.warn("Unexpected protocol exception", e)
            }
        } while (retry && ++i < maxTry && !closed.get())

        if (response == null) {
            return getFailedResponse(lastThrowable, i, maxTry)
        }

        setResponseTime(startTime, page, response)
        return getOutputWithHttpStatusTransformed(page.url, response)
    }

    private fun shouldRetry(response: Response): Boolean {
        return response !is ForwardingResponse && response.status.isRetry(RetryScope.PROTOCOL)
    }

    /**
     * TODO: do not translate status code, they are just OK to handle in FetchComponent
     */
    @Throws(MalformedURLException::class)
    private fun getOutputWithHttpStatusTransformed(url: String, response: Response): ProtocolOutput {
        var u = URL(url)
        val httpCode = response.httpCode
        val pageDatum = response.pageDatum
        val content = pageDatum.content
        // bytes = bytes == null ? EMPTY_CONTENT : bytes;
        val contentType = response.getHeader(HttpHeaders.CONTENT_TYPE)
        pageDatum.resolveMimeType(contentType, url, content, mimeTypeResolver)

        val headers = pageDatum.headers
        val status: ProtocolStatus
        // got a good response
        when (httpCode) {
            200 -> return ProtocolOutput(pageDatum)
            304 -> return ProtocolOutput(pageDatum, pageDatum.headers, ProtocolStatus.STATUS_NOTMODIFIED)
            in 300..399 -> {
                // handle redirect
                // some broken servers, such as MS IIS, use lowercase header name...
                val redirect = response.getHeader("Location")?:response.getHeader("location")?:""
                u = URL(u, redirect)
                val code = when (httpCode) {
                    HttpStatus.SC_MULTIPLE_CHOICES -> ProtocolStatus.MOVED
                    HttpStatus.SC_MOVED_PERMANENTLY, HttpStatus.SC_USE_PROXY -> ProtocolStatus.MOVED
                    HttpStatus.SC_MOVED_TEMPORARILY, HttpStatus.SC_SEE_OTHER, HttpStatus.SC_TEMPORARY_REDIRECT -> ProtocolStatus.TEMP_MOVED
                    else -> ProtocolStatus.MOVED
                }
                // handle redirection in the higher layer.
                // page.getMetadata().set(ARG_REDIRECT_TO_URL, url.toString());
                status = ProtocolStatus.failed(code, ARG_HTTP_CODE, httpCode, ProtocolStatus.ARG_REDIRECT_TO_URL, u)
            }
            HttpStatus.SC_BAD_REQUEST -> {
                log.warn("400 Bad request | {}", u)
                status = ProtocolStatus.failed(ProtocolStatusCodes.GONE, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_UNAUTHORIZED -> { // requires authorization, but no valid auth provided.
                status = ProtocolStatus.failed(ProtocolStatusCodes.ACCESS_DENIED, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_NOT_FOUND -> { // GONE
                status = ProtocolStatus.failed(ProtocolStatusCodes.NOTFOUND, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_REQUEST_TIMEOUT -> { // TIMEOUT
                status = ProtocolStatus.failed(ProtocolStatusCodes.REQUEST_TIMEOUT, ARG_HTTP_CODE, httpCode)
            }
            HttpStatus.SC_GONE -> { // permanently GONE
                status = ProtocolStatus.failed(ProtocolStatusCodes.GONE, ARG_HTTP_CODE, httpCode)
            }
            else -> {
                status = response.status
            }
        }

        return ProtocolOutput(pageDatum, headers, status)
    }

    private fun getFailedResponse(lastThrowable: Throwable?, tryCount: Int, maxRry: Int): ProtocolOutput {
        val code = when (lastThrowable) {
            is ConnectException -> ProtocolStatus.REQUEST_TIMEOUT
            is SocketTimeoutException -> ProtocolStatus.REQUEST_TIMEOUT
            is UnknownHostException -> ProtocolStatus.UNKNOWN_HOST
            else -> ProtocolStatus.EXCEPTION
        }
        val protocolStatus = ProtocolStatus.failed(code,
                "exception", lastThrowable,
                "retry", tryCount,
                "maxRetry", maxRry)
        return ProtocolOutput(null, MultiMetadata(), protocolStatus)
    }

    private fun setResponseTime(startTime: Instant, page: WebPage, response: Response) {
        val pageFetchMode = page.fetchMode
        val elapsedTime = if (pageFetchMode == FetchMode.BROWSER) {
            val requestTime = response.getHeader(HttpHeaders.Q_REQUEST_TIME)?.toLongOrNull()?:-1
            val responseTime = response.getHeader(HttpHeaders.Q_RESPONSE_TIME)?.toLongOrNull()?:-1
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
        private const val ARG_HTTP_CODE = ProtocolStatus.ARG_HTTP_CODE
    }
}
