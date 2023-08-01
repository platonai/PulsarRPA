package ai.platon.pulsar.common.http

import org.springframework.lang.Nullable

/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Enumeration of HTTP status codes.
 *
 *
 * The HTTP status code series can be retrieved via [.series].
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 3.0
 * @see HttpStatus.Series
 *
 * @see [HTTP Status Code Registry](https://www.iana.org/assignments/http-status-codes)
 *
 * @see [List of HTTP status codes - Wikipedia](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)
 */
enum class HttpStatus(
        override val value: Int,
        val series: Series,
        /**
         * Return the reason phrase of this status code.
         */
        val reasonPhrase: String
) : HttpStatusCode {
    // 1xx Informational
    /**
     * `100 Continue`.
     * @see [HTTP/1.1: Semantics and Content, section 6.2.1](https://tools.ietf.org/html/rfc7231.section-6.2.1)
     */
    CONTINUE(100, Series.INFORMATIONAL, "Continue"),

    /**
     * `101 Switching Protocols`.
     * @see [HTTP/1.1: Semantics and Content, section 6.2.2](https://tools.ietf.org/html/rfc7231.section-6.2.2)
     */
    SWITCHING_PROTOCOLS(101, Series.INFORMATIONAL, "Switching Protocols"),

    /**
     * `102 Processing`.
     * @see [WebDAV](https://tools.ietf.org/html/rfc2518.section-10.1)
     */
    PROCESSING(102, Series.INFORMATIONAL, "Processing"),

    /**
     * `103 Early Hints`.
     * @see [An HTTP Status Code for Indicating Hints](https://tools.ietf.org/html/rfc8297)
     *
     * @since 6.0.5
     */
    EARLY_HINTS(103, Series.INFORMATIONAL, "Early Hints"),

    /**
     * `103 Checkpoint`.
     * @see [A proposal for supporting
     * resumable POST/PUT HTTP requests in HTTP/1.0](https://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal)
     *
     */
    @Deprecated("in favor of {@link #EARLY_HINTS} which will be returned from {@code HttpStatus.valueOf(103)}")
    CHECKPOINT(103, Series.INFORMATIONAL, "Checkpoint"),
    // 2xx Success
    /**
     * `200 OK`.
     * @see [HTTP/1.1: Semantics and Content, section 6.3.1](https://tools.ietf.org/html/rfc7231.section-6.3.1)
     */
    OK(200, Series.SUCCESSFUL, "OK"),

    /**
     * `201 Created`.
     * @see [HTTP/1.1: Semantics and Content, section 6.3.2](https://tools.ietf.org/html/rfc7231.section-6.3.2)
     */
    CREATED(201, Series.SUCCESSFUL, "Created"),

    /**
     * `202 Accepted`.
     * @see [HTTP/1.1: Semantics and Content, section 6.3.3](https://tools.ietf.org/html/rfc7231.section-6.3.3)
     */
    ACCEPTED(202, Series.SUCCESSFUL, "Accepted"),

    /**
     * `203 Non-Authoritative Information`.
     * @see [HTTP/1.1: Semantics and Content, section 6.3.4](https://tools.ietf.org/html/rfc7231.section-6.3.4)
     */
    NON_AUTHORITATIVE_INFORMATION(203, Series.SUCCESSFUL, "Non-Authoritative Information"),

    /**
     * `204 No Content`.
     * @see [HTTP/1.1: Semantics and Content, section 6.3.5](https://tools.ietf.org/html/rfc7231.section-6.3.5)
     */
    NO_CONTENT(204, Series.SUCCESSFUL, "No Content"),

    /**
     * `205 Reset Content`.
     * @see [HTTP/1.1: Semantics and Content, section 6.3.6](https://tools.ietf.org/html/rfc7231.section-6.3.6)
     */
    RESET_CONTENT(205, Series.SUCCESSFUL, "Reset Content"),

    /**
     * `206 Partial Content`.
     * @see [HTTP/1.1: Range Requests, section 4.1](https://tools.ietf.org/html/rfc7233.section-4.1)
     */
    PARTIAL_CONTENT(206, Series.SUCCESSFUL, "Partial Content"),

    /**
     * `207 Multi-Status`.
     * @see [WebDAV](https://tools.ietf.org/html/rfc4918.section-13)
     */
    MULTI_STATUS(207, Series.SUCCESSFUL, "Multi-Status"),

    /**
     * `208 Already Reported`.
     * @see [WebDAV Binding Extensions](https://tools.ietf.org/html/rfc5842.section-7.1)
     */
    ALREADY_REPORTED(208, Series.SUCCESSFUL, "Already Reported"),

    /**
     * `226 IM Used`.
     * @see [Delta encoding in HTTP](https://tools.ietf.org/html/rfc3229.section-10.4.1)
     */
    IM_USED(226, Series.SUCCESSFUL, "IM Used"),
    // 3xx Redirection
    /**
     * `300 Multiple Choices`.
     * @see [HTTP/1.1: Semantics and Content, section 6.4.1](https://tools.ietf.org/html/rfc7231.section-6.4.1)
     */
    MULTIPLE_CHOICES(300, Series.REDIRECTION, "Multiple Choices"),

    /**
     * `301 Moved Permanently`.
     * @see [HTTP/1.1: Semantics and Content, section 6.4.2](https://tools.ietf.org/html/rfc7231.section-6.4.2)
     */
    MOVED_PERMANENTLY(301, Series.REDIRECTION, "Moved Permanently"),

    /**
     * `302 Found`.
     * @see [HTTP/1.1: Semantics and Content, section 6.4.3](https://tools.ietf.org/html/rfc7231.section-6.4.3)
     */
    FOUND(302, Series.REDIRECTION, "Found"),

    /**
     * `302 Moved Temporarily`.
     * @see [HTTP/1.0, section 9.3](https://tools.ietf.org/html/rfc1945.section-9.3)
     *
     */
    @Deprecated("in favor of {@link #FOUND} which will be returned from {@code HttpStatus.valueOf(302)}")
    MOVED_TEMPORARILY(302, Series.REDIRECTION, "Moved Temporarily"),

    /**
     * `303 See Other`.
     * @see [HTTP/1.1: Semantics and Content, section 6.4.4](https://tools.ietf.org/html/rfc7231.section-6.4.4)
     */
    SEE_OTHER(303, Series.REDIRECTION, "See Other"),

    /**
     * `304 Not Modified`.
     * @see [HTTP/1.1: Conditional Requests, section 4.1](https://tools.ietf.org/html/rfc7232.section-4.1)
     */
    NOT_MODIFIED(304, Series.REDIRECTION, "Not Modified"),

    /**
     * `305 Use Proxy`.
     * @see [HTTP/1.1: Semantics and Content, section 6.4.5](https://tools.ietf.org/html/rfc7231.section-6.4.5)
     *
     */
    @Deprecated("due to security concerns regarding in-band configuration of a proxy")
    USE_PROXY(305, Series.REDIRECTION, "Use Proxy"),

    /**
     * `307 Temporary Redirect`.
     * @see [HTTP/1.1: Semantics and Content, section 6.4.7](https://tools.ietf.org/html/rfc7231.section-6.4.7)
     */
    TEMPORARY_REDIRECT(307, Series.REDIRECTION, "Temporary Redirect"),

    /**
     * `308 Permanent Redirect`.
     * @see [RFC 7238](https://tools.ietf.org/html/rfc7238)
     */
    PERMANENT_REDIRECT(308, Series.REDIRECTION, "Permanent Redirect"),
    // --- 4xx Client Error ---
    /**
     * `400 Bad Request`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.1](https://tools.ietf.org/html/rfc7231.section-6.5.1)
     */
    BAD_REQUEST(400, Series.CLIENT_ERROR, "Bad Request"),

    /**
     * `401 Unauthorized`.
     * @see [HTTP/1.1: Authentication, section 3.1](https://tools.ietf.org/html/rfc7235.section-3.1)
     */
    UNAUTHORIZED(401, Series.CLIENT_ERROR, "Unauthorized"),

    /**
     * `402 Payment Required`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.2](https://tools.ietf.org/html/rfc7231.section-6.5.2)
     */
    PAYMENT_REQUIRED(402, Series.CLIENT_ERROR, "Payment Required"),

    /**
     * `403 Forbidden`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.3](https://tools.ietf.org/html/rfc7231.section-6.5.3)
     */
    FORBIDDEN(403, Series.CLIENT_ERROR, "Forbidden"),

    /**
     * `404 Not Found`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.4](https://tools.ietf.org/html/rfc7231.section-6.5.4)
     */
    NOT_FOUND(404, Series.CLIENT_ERROR, "Not Found"),

    /**
     * `405 Method Not Allowed`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.5](https://tools.ietf.org/html/rfc7231.section-6.5.5)
     */
    METHOD_NOT_ALLOWED(405, Series.CLIENT_ERROR, "Method Not Allowed"),

    /**
     * `406 Not Acceptable`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.6](https://tools.ietf.org/html/rfc7231.section-6.5.6)
     */
    NOT_ACCEPTABLE(406, Series.CLIENT_ERROR, "Not Acceptable"),

    /**
     * `407 Proxy Authentication Required`.
     * @see [HTTP/1.1: Authentication, section 3.2](https://tools.ietf.org/html/rfc7235.section-3.2)
     */
    PROXY_AUTHENTICATION_REQUIRED(407, Series.CLIENT_ERROR, "Proxy Authentication Required"),

    /**
     * `408 Request Timeout`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.7](https://tools.ietf.org/html/rfc7231.section-6.5.7)
     */
    REQUEST_TIMEOUT(408, Series.CLIENT_ERROR, "Request Timeout"),

    /**
     * `409 Conflict`.
     * @see [HTTP/1.1: Semantics and Content, section 6.5.8](https://tools.ietf.org/html/rfc7231.section-6.5.8)
     */
    CONFLICT(409, Series.CLIENT_ERROR, "Conflict"),

    /**
     * `410 Gone`.
     * @see [
     * HTTP/1.1: Semantics and Content, section 6.5.9](https://tools.ietf.org/html/rfc7231.section-6.5.9)
     */
    GONE(410, Series.CLIENT_ERROR, "Gone"),

    /**
     * `411 Length Required`.
     * @see [
     * HTTP/1.1: Semantics and Content, section 6.5.10](https://tools.ietf.org/html/rfc7231.section-6.5.10)
     */
    LENGTH_REQUIRED(411, Series.CLIENT_ERROR, "Length Required"),

    /**
     * `412 Precondition failed`.
     * @see [
     * HTTP/1.1: Conditional Requests, section 4.2](https://tools.ietf.org/html/rfc7232.section-4.2)
     */
    PRECONDITION_FAILED(412, Series.CLIENT_ERROR, "Precondition Failed"),

    /**
     * `413 Payload Too Large`.
     * @since 4.1
     * @see [
     * HTTP/1.1: Semantics and Content, section 6.5.11](https://tools.ietf.org/html/rfc7231.section-6.5.11)
     */
    PAYLOAD_TOO_LARGE(413, Series.CLIENT_ERROR, "Payload Too Large"),

    /**
     * `413 Request Entity Too Large`.
     * @see [HTTP/1.1, section 10.4.14](https://tools.ietf.org/html/rfc2616.section-10.4.14)
     *
     */
    @Deprecated("""in favor of {@link #PAYLOAD_TOO_LARGE} which will be
	  returned from {@code HttpStatus.valueOf(413)}""")
    REQUEST_ENTITY_TOO_LARGE(413, Series.CLIENT_ERROR, "Request Entity Too Large"),

    /**
     * `414 URI Too Long`.
     * @since 4.1
     * @see [
     * HTTP/1.1: Semantics and Content, section 6.5.12](https://tools.ietf.org/html/rfc7231.section-6.5.12)
     */
    URI_TOO_LONG(414, Series.CLIENT_ERROR, "URI Too Long"),

    /**
     * `414 Request-URI Too Long`.
     * @see [HTTP/1.1, section 10.4.15](https://tools.ietf.org/html/rfc2616.section-10.4.15)
     *
     */
    @Deprecated("in favor of {@link #URI_TOO_LONG} which will be returned from {@code HttpStatus.valueOf(414)}")
    REQUEST_URI_TOO_LONG(414, Series.CLIENT_ERROR, "Request-URI Too Long"),

    /**
     * `415 Unsupported Media Type`.
     * @see [
     * HTTP/1.1: Semantics and Content, section 6.5.13](https://tools.ietf.org/html/rfc7231.section-6.5.13)
     */
    UNSUPPORTED_MEDIA_TYPE(415, Series.CLIENT_ERROR, "Unsupported Media Type"),

    /**
     * `416 Requested Range Not Satisfiable`.
     * @see [HTTP/1.1: Range Requests, section 4.4](https://tools.ietf.org/html/rfc7233.section-4.4)
     */
    REQUESTED_RANGE_NOT_SATISFIABLE(416, Series.CLIENT_ERROR, "Requested range not satisfiable"),

    /**
     * `417 Expectation Failed`.
     * @see [
     * HTTP/1.1: Semantics and Content, section 6.5.14](https://tools.ietf.org/html/rfc7231.section-6.5.14)
     */
    EXPECTATION_FAILED(417, Series.CLIENT_ERROR, "Expectation Failed"),

    /**
     * `418 I'm a teapot`.
     * @see [HTCPCP/1.0](https://tools.ietf.org/html/rfc2324.section-2.3.2)
     */
    I_AM_A_TEAPOT(418, Series.CLIENT_ERROR, "I'm a teapot"),

    /**
     * `424 Failed Dependency`.
     * @see [WebDAV](https://tools.ietf.org/html/rfc4918.section-11.4)
     */
    FAILED_DEPENDENCY(424, Series.CLIENT_ERROR, "Failed Dependency"),

    /**
     * `425 Too Early`.
     * @since 5.2
     * @see [RFC 8470](https://tools.ietf.org/html/rfc8470)
     */
    TOO_EARLY(425, Series.CLIENT_ERROR, "Too Early"),

    /**
     * `426 Upgrade Required`.
     * @see [Upgrading to TLS Within HTTP/1.1](https://tools.ietf.org/html/rfc2817.section-6)
     */
    UPGRADE_REQUIRED(426, Series.CLIENT_ERROR, "Upgrade Required"),

    /**
     * `428 Precondition Required`.
     * @see [Additional HTTP Status Codes](https://tools.ietf.org/html/rfc6585.section-3)
     */
    PRECONDITION_REQUIRED(428, Series.CLIENT_ERROR, "Precondition Required"),

    /**
     * `429 Too Many Requests`.
     * @see [Additional HTTP Status Codes](https://tools.ietf.org/html/rfc6585.section-4)
     */
    TOO_MANY_REQUESTS(429, Series.CLIENT_ERROR, "Too Many Requests"),

    /**
     * `431 Request Header Fields Too Large`.
     * @see [Additional HTTP Status Codes](https://tools.ietf.org/html/rfc6585.section-5)
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, Series.CLIENT_ERROR, "Request Header Fields Too Large"),

    /**
     * `451 Unavailable For Legal Reasons`.
     * @see [
     * An HTTP Status Code to Report Legal Obstacles](https://tools.ietf.org/html/draft-ietf-httpbis-legally-restricted-status-04)
     *
     * @since 4.3
     */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, Series.CLIENT_ERROR, "Unavailable For Legal Reasons"),
    // --- 5xx Server Error ---
    /**
     * `500 Internal Server Error`.
     * @see [HTTP/1.1: Semantics and Content, section 6.6.1](https://tools.ietf.org/html/rfc7231.section-6.6.1)
     */
    INTERNAL_SERVER_ERROR(500, Series.SERVER_ERROR, "Internal Server Error"),

    /**
     * `501 Not Implemented`.
     * @see [HTTP/1.1: Semantics and Content, section 6.6.2](https://tools.ietf.org/html/rfc7231.section-6.6.2)
     */
    NOT_IMPLEMENTED(501, Series.SERVER_ERROR, "Not Implemented"),

    /**
     * `502 Bad Gateway`.
     * @see [HTTP/1.1: Semantics and Content, section 6.6.3](https://tools.ietf.org/html/rfc7231.section-6.6.3)
     */
    BAD_GATEWAY(502, Series.SERVER_ERROR, "Bad Gateway"),

    /**
     * `503 Service Unavailable`.
     * @see [HTTP/1.1: Semantics and Content, section 6.6.4](https://tools.ietf.org/html/rfc7231.section-6.6.4)
     */
    SERVICE_UNAVAILABLE(503, Series.SERVER_ERROR, "Service Unavailable"),

    /**
     * `504 Gateway Timeout`.
     * @see [HTTP/1.1: Semantics and Content, section 6.6.5](https://tools.ietf.org/html/rfc7231.section-6.6.5)
     */
    GATEWAY_TIMEOUT(504, Series.SERVER_ERROR, "Gateway Timeout"),

    /**
     * `505 HTTP Version Not Supported`.
     * @see [HTTP/1.1: Semantics and Content, section 6.6.6](https://tools.ietf.org/html/rfc7231.section-6.6.6)
     */
    HTTP_VERSION_NOT_SUPPORTED(505, Series.SERVER_ERROR, "HTTP Version not supported"),

    /**
     * `506 Variant Also Negotiates`
     * @see [Transparent Content Negotiation](https://tools.ietf.org/html/rfc2295.section-8.1)
     */
    VARIANT_ALSO_NEGOTIATES(506, Series.SERVER_ERROR, "Variant Also Negotiates"),

    /**
     * `507 Insufficient Storage`
     * @see [WebDAV](https://tools.ietf.org/html/rfc4918.section-11.5)
     */
    INSUFFICIENT_STORAGE(507, Series.SERVER_ERROR, "Insufficient Storage"),

    /**
     * `508 Loop Detected`
     * @see [WebDAV Binding Extensions](https://tools.ietf.org/html/rfc5842.section-7.2)
     */
    LOOP_DETECTED(508, Series.SERVER_ERROR, "Loop Detected"),

    /**
     * `509 Bandwidth Limit Exceeded`
     */
    BANDWIDTH_LIMIT_EXCEEDED(509, Series.SERVER_ERROR, "Bandwidth Limit Exceeded"),

    /**
     * `510 Not Extended`
     * @see [HTTP Extension Framework](https://tools.ietf.org/html/rfc2774.section-7)
     */
    NOT_EXTENDED(510, Series.SERVER_ERROR, "Not Extended"),

    /**
     * `511 Network Authentication Required`.
     * @see [Additional HTTP Status Codes](https://tools.ietf.org/html/rfc6585.section-6)
     */
    NETWORK_AUTHENTICATION_REQUIRED(511, Series.SERVER_ERROR, "Network Authentication Required");

    /**
     * Return the HTTP status series of this status code.
     * @see HttpStatus.Series
     */
    fun series(): Series {
        return series
    }

    override fun is1xxInformational(): Boolean {
        return series() == Series.INFORMATIONAL
    }

    override fun is2xxSuccessful(): Boolean {
        return series() == Series.SUCCESSFUL
    }

    override fun is3xxRedirection(): Boolean {
        return series() == Series.REDIRECTION
    }

    override fun is4xxClientError(): Boolean {
        return series() == Series.CLIENT_ERROR
    }

    override fun is5xxServerError(): Boolean {
        return series() == Series.SERVER_ERROR
    }

    override fun isError() = is4xxClientError() || is5xxServerError()

    /**
     * Return a string representation of this status code.
     */
    override fun toString(): String {
        return value.toString() + " " + name
    }

    /**
     * Enumeration of HTTP status series.
     *
     * Retrievable via [series].
     */
    enum class Series(private val value: Int) {
        INFORMATIONAL(1),
        SUCCESSFUL(2),
        REDIRECTION(3),
        CLIENT_ERROR(4),
        SERVER_ERROR(5);

        /**
         * Return the integer value of this status series. Ranges from 1 to 5.
         */
        fun value(): Int {
            return value
        }

        companion object {
            /**
             * Return the `Series` enum constant for the supplied `HttpStatus`.
             * @param status a standard HTTP status enum constant
             * @return the `Series` enum constant for the supplied `HttpStatus`
             */
            @Deprecated("as of 5.3, in favor of invoking {@link HttpStatus#series()} directly")
            fun valueOf(status: HttpStatus): Series {
                return status.series
            }

            /**
             * Return the `Series` enum constant for the supplied status code.
             * @param statusCode the HTTP status code (potentially non-standard)
             * @return the `Series` enum constant for the supplied status code
             * @throws IllegalArgumentException if this enum has no corresponding constant
             */
            fun valueOf(statusCode: Int): Series {
                return resolve(statusCode)
                        ?: throw IllegalArgumentException("No matching constant for [$statusCode]")
            }

            /**
             * Resolve the given status code to an `HttpStatus.Series`, if possible.
             * @param statusCode the HTTP status code (potentially non-standard)
             * @return the corresponding `Series`, or `null` if not found
             * @since 5.1.3
             */
            @Nullable
            fun resolve(statusCode: Int): Series? {
                val seriesCode = statusCode / 100
                for (series in values()) {
                    if (series.value == seriesCode) {
                        return series
                    }
                }
                return null
            }
        }
    }

    companion object {
        private val VALUES: Array<HttpStatus>

        init {
            VALUES = values()
        }

        /**
         * Return the `HttpStatus` enum constant with the specified numeric value.
         * @param statusCode the numeric value of the enum to be returned
         * @return the enum constant with the specified numeric value
         * @throws IllegalArgumentException if this enum has no constant for the specified numeric value
         */
        fun valueOf(statusCode: Int): HttpStatus {
            return resolve(statusCode)
                    ?: throw IllegalArgumentException("No matching constant for [$statusCode]")
        }

        /**
         * Resolve the given status code to an `HttpStatus`, if possible.
         * @param statusCode the HTTP status code (potentially non-standard)
         * @return the corresponding `HttpStatus`, or `null` if not found
         * @since 5.0
         */
        @Nullable
        fun resolve(statusCode: Int): HttpStatus? {
            // Use cached VALUES instead of values() to prevent array allocation.
            for (status in VALUES) {
                if (status.value == statusCode) {
                    return status
                }
            }
            return null
        }
    }
}