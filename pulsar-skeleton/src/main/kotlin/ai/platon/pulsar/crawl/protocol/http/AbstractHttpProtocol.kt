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
import ai.platon.pulsar.common.MimeUtil
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.protocol.Content
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
import org.apache.commons.lang3.math.NumberUtils
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import java.net.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractHttpProtocol: Protocol {
    private val log = LoggerFactory.getLogger(AbstractHttpProtocol::class.java)
    protected val closed = AtomicBoolean()
    val isClosed get() = closed.get()
    /**
     * The max retry time
     */
    private var fetchMaxRetry = 3
    /**
     * The configuration
     */
    private lateinit var conf: ImmutableConfig

    private lateinit var mimeTypes: MimeUtil

    private lateinit var robots: HttpRobotRulesParser

    override fun getConf(): ImmutableConfig {
        return conf
    }

    override fun setConf(jobConf: ImmutableConfig) {
        conf = jobConf
        fetchMaxRetry = jobConf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
        mimeTypes = MimeUtil(jobConf)
        robots = HttpRobotRulesParser(jobConf)
    }

    override fun reset() {
        // reset proxy, user agent, etc
    }

    override fun getResponses(pages: Collection<WebPage>, volatileConfig: VolatileConfig): Collection<Response> {
        return if (closed.get()) {
            emptyList()
        } else pages.mapNotNull { getResponseNoexcept(it.url, it, false) }
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
        return try {
            val startTime = Instant.now()
            val response = getResponseDeferred(page.url, page, false)
                    ?:return ProtocolOutput(ProtocolStatus.retry(RetryScope.CRAWL))
            setResponseTime(startTime, page, response)
            val location = page.location?:page.url
            getOutputWithHttpStatusTransformed(page.url, location, response)
        } catch (e: Throwable) {
            log.warn("Unexpected exception", e)
            ProtocolOutput(ProtocolStatus.failed(e))
        }
    }

    private fun getProtocolOutputWithRetry(page: WebPage): ProtocolOutput {
        val startTime = Instant.now()
        var response: Response?
        var retry = false
        var lastThrowable: Throwable? = null
        var i = 0
        val maxTry = getMaxRetry(page)
        do {
            if (i > 0) {
                log.info("Protocol retry: {}/{} | {}", i, maxTry, page.url)
            }
            try { // TODO: FETCH_PROTOCOL does not work if the response is forwarded
                response = getResponse(page.url, page, false)
                retry = response == null || response.status.isRetry(RetryScope.PROTOCOL)
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
        // page.baseUrl/page.location is the last working address, and page.url is the permanent internal key for the page
        val location = page.location?:page.url
        return getOutputWithHttpStatusTransformed(page.url, location, response)
    }

    /**
     * TODO: do not translate status code, they are just OK to handle in FetchComponent
     */
    @Throws(MalformedURLException::class)
    private fun getOutputWithHttpStatusTransformed(url: String, location: String, response: Response): ProtocolOutput {
        var u = URL(url)
        val httpCode = response.httpCode
        val bytes = response.content
        // bytes = bytes == null ? EMPTY_CONTENT : bytes;
        val contentType = response.getHeader(HttpHeaders.CONTENT_TYPE)
        val content = Content(url, location, bytes, contentType, response.headers, mimeTypes)
        val headers = response.headers
        val status: ProtocolStatus
        when (httpCode) {
            200 -> {
                // got a good response
                return ProtocolOutput(content, headers)
            }
            304 -> {
                return ProtocolOutput(content, headers, ProtocolStatus.STATUS_NOTMODIFIED)
            }
            in 300..399 -> { // handle redirect
                var redirect = response.getHeader("Location")
                // some broken servers, such as MS IIS, use lowercase header name...
                if (redirect == null) {
                    redirect = response.getHeader("location")
                }
                if (redirect == null) {
                    redirect = ""
                }
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
        return ProtocolOutput(content, headers, status)
    }

    private fun getMaxRetry(page: WebPage): Int {
        // Never retry if fetch mode is crowd sourcing
        val maxRry = 1.takeIf { page.fetchMode == FetchMode.CROWD_SOURCING } ?: fetchMaxRetry
        return maxRry.coerceAtMost(MAX_REY_GUARD)
    }

    private fun getFailedResponse(lastThrowable: Throwable?, tryCount: Int, maxRry: Int): ProtocolOutput {
        val code = if (lastThrowable is ConnectException) {
            ProtocolStatus.REQUEST_TIMEOUT
        } else if (lastThrowable is SocketTimeoutException) {
            ProtocolStatus.REQUEST_TIMEOUT
        } else if (lastThrowable is UnknownHostException) {
            ProtocolStatus.UNKNOWN_HOST
        } else {
            ProtocolStatus.EXCEPTION
        }
        val protocolStatus = ProtocolStatus.failed(code,
                "exception", lastThrowable,
                "retry", tryCount,
                "maxRetry", maxRry)
        return ProtocolOutput(null, MultiMetadata(), protocolStatus)
    }

    private fun setResponseTime(startTime: Instant, page: WebPage, response: Response) {
        val elapsedTime: Duration
        val pageFetchMode = page.fetchMode
        elapsedTime = if (pageFetchMode == FetchMode.CROWD_SOURCING || pageFetchMode == FetchMode.BROWSER) {
            val requestTime = NumberUtils.toLong(response.getHeader(HttpHeaders.Q_REQUEST_TIME), -1)
            val responseTime = NumberUtils.toLong(response.getHeader(HttpHeaders.Q_RESPONSE_TIME), -1)
            if (requestTime > 0 && responseTime > 0) {
                Duration.ofMillis(responseTime - requestTime)
            } else {
                // -1 hour means an invalid response time which indicates a bug
                Duration.ofHours(-1)
            }
        } else {
            Duration.between(startTime, Instant.now())
        }
        page.metadata[Name.RESPONSE_TIME] = elapsedTime.toString()
    }

    @Throws(Exception::class)
    abstract fun getResponse(url: String, page: WebPage, followRedirects: Boolean): Response?

    @Throws(Exception::class)
    abstract suspend fun getResponseDeferred(url: String, page: WebPage, followRedirects: Boolean): Response?

    private fun getResponseNoexcept(url: String, page: WebPage, followRedirects: Boolean): Response? {
        return try {
            getResponse(url, page, followRedirects)
        } catch (e: Exception) {
            null
        }
    }

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
