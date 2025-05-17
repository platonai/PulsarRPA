package ai.platon.pulsar.common

/**
 * Keep consistent with standard http status.
 *
 * * [Wiki HTTP status codes](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)
 * * [Mozilla Status](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
 * * [Apache httpcomponents HttpStatus](https://github.com/apache/httpcomponents-core/blob/master/httpcore5/src/main/java/org/apache/hc/core5/http/java)
 */
object ResourceStatus {
    // --- 1xx Informational ---
    /** `100 1xx Informational` (HTTP Semantics)  */
    const val SC_INFORMATIONAL: Int = 100

    /** `100 Continue` (HTTP Semantics)  */
    const val SC_CONTINUE: Int = 100

    /** `101 Switching Protocols` (HTTP Semantics) */
    const val SC_SWITCHING_PROTOCOLS: Int = 101

    /** `102 Processing` (WebDAV - RFC 2518)  */
    const val SC_PROCESSING: Int = 102

    /** `103 Early Hints (Early Hints - RFC 8297)` */
    const val SC_EARLY_HINTS: Int = 103

    // --- 2xx Success ---
    /** `2xx Success` (HTTP Semantics)  */
    const val SC_SUCCESS: Int = 200

    /** `200 OK` (HTTP Semantics)  */
    const val SC_OK: Int = 200

    /** `201 Created` (HTTP Semantics)  */
    const val SC_CREATED: Int = 201

    /** `202 Accepted` (HTTP Semantics)  */
    const val SC_ACCEPTED: Int = 202

    /** `203 Non Authoritative Information` (HTTP Semantics)  */
    const val SC_NON_AUTHORITATIVE_INFORMATION: Int = 203

    /** `204 No Content` (HTTP Semantics)  */
    const val SC_NO_CONTENT: Int = 204

    /** `205 Reset Content` (HTTP Semantics)  */
    const val SC_RESET_CONTENT: Int = 205

    /** `206 Partial Content` (HTTP Semantics)  */
    const val SC_PARTIAL_CONTENT: Int = 206

    /**
     * `207 Multi-Status` (WebDAV - RFC 2518)
     * or
     * `207 Partial Update OK` (HTTP/1.1 - draft-ietf-http-v11-spec-rev-01?)
     */
    const val SC_MULTI_STATUS: Int = 207

    /**
     * `208 Already Reported` (WebDAV - RFC 5842, p.30, section 7.1)
     */
    const val SC_ALREADY_REPORTED: Int = 208

    /**
     * `226 IM Used` (Delta encoding in HTTP - RFC 3229, p. 30, section 10.4.1)
     */
    const val SC_IM_USED: Int = 226

    // --- 3xx Redirection ---
    /** `3xx Redirection` (HTTP Semantics)  */
    const val SC_REDIRECTION: Int = 300

    /** `300 Multiple Choices` (HTTP Semantics)  */
    const val SC_MULTIPLE_CHOICES: Int = 300

    /** `301 Moved Permanently` (HTTP Semantics)  */
    const val SC_MOVED_PERMANENTLY: Int = 301

    /** `302 Moved Temporarily` (Sometimes `Found`) (HTTP Semantics)  */
    const val SC_MOVED_TEMPORARILY: Int = 302

    /** `303 See Other` (HTTP Semantics)  */
    const val SC_SEE_OTHER: Int = 303

    /** `304 Not Modified` (HTTP Semantics)  */
    const val SC_NOT_MODIFIED: Int = 304

    /** `305 Use Proxy` (HTTP Semantics)  */
    const val SC_USE_PROXY: Int = 305

    /** `307 Temporary Redirect` (HTTP Semantics)  */
    const val SC_TEMPORARY_REDIRECT: Int = 307

    /** `308 Permanent Redirect` (HTTP Semantics)  */
    const val SC_PERMANENT_REDIRECT: Int = 308

    // --- 4xx Client Error ---
    /** `4xx Client Error` (HTTP Semantics)  */
    const val SC_CLIENT_ERROR: Int = 400

    /** `400 Bad Request` (HTTP Semantics)  */
    const val SC_BAD_REQUEST: Int = 400

    /** `401 Unauthorized` (HTTP Semantics)  */
    const val SC_UNAUTHORIZED: Int = 401

    /** `402 Payment Required` (HTTP Semantics)  */
    const val SC_PAYMENT_REQUIRED: Int = 402

    /** `403 Forbidden` (HTTP Semantics)  */
    const val SC_FORBIDDEN: Int = 403

    /** `404 Not Found` (HTTP Semantics)  */
    const val SC_NOT_FOUND: Int = 404

    /** `405 Method Not Allowed` (HTTP Semantics)  */
    const val SC_METHOD_NOT_ALLOWED: Int = 405

    /** `406 Not Acceptable` (HTTP Semantics)  */
    const val SC_NOT_ACCEPTABLE: Int = 406

    /** `407 Proxy Authentication Required` (HTTP Semantics) */
    const val SC_PROXY_AUTHENTICATION_REQUIRED: Int = 407

    /** `408 Request Timeout` (HTTP Semantics)  */
    const val SC_REQUEST_TIMEOUT: Int = 408

    /** `409 Conflict` (HTTP Semantics)  */
    const val SC_CONFLICT: Int = 409

    /** `410 Gone` (HTTP Semantics)  */
    const val SC_GONE: Int = 410

    /** `411 Length Required` (HTTP Semantics)  */
    const val SC_LENGTH_REQUIRED: Int = 411

    /** `412 Precondition Failed` (HTTP Semantics)  */
    const val SC_PRECONDITION_FAILED: Int = 412

    /** `413 Request Entity Too Large` (HTTP Semantics)  */
    const val SC_REQUEST_TOO_LONG: Int = 413

    /** `414 Request-URI Too Long` (HTTP Semantics)  */
    const val SC_REQUEST_URI_TOO_LONG: Int = 414

    /** `415 Unsupported Media Type` (HTTP Semantics)  */
    const val SC_UNSUPPORTED_MEDIA_TYPE: Int = 415

    /** `416 Requested Range Not Satisfiable` (HTTP Semantics)  */
    const val SC_REQUESTED_RANGE_NOT_SATISFIABLE: Int = 416

    /** `417 Expectation Failed` (HTTP Semantics)  */
    const val SC_EXPECTATION_FAILED: Int = 417

    /** `421 Misdirected Request` (HTTP Semantics)  */
    const val SC_MISDIRECTED_REQUEST: Int = 421

    /** `422 Unprocessable Content` (HTTP Semantics)  */
    const val SC_UNPROCESSABLE_CONTENT: Int = 422

    /** `426 Upgrade Required` (HTTP Semantics)  */
    const val SC_UPGRADE_REQUIRED: Int = 426

    /**
     * Static constant for a 419 error.
     * `419 Insufficient Space on Resource`
     * (WebDAV - draft-ietf-webdav-protocol-05?)
     * or `419 Proxy Reauthentication Required`
     * (HTTP/1.1 drafts?)
     */
    const val SC_INSUFFICIENT_SPACE_ON_RESOURCE: Int = 419

    /**
     * Static constant for a 420 error.
     * `420 Method Failure`
     * (WebDAV - draft-ietf-webdav-protocol-05?)
     */
    const val SC_METHOD_FAILURE: Int = 420

    @Deprecated("Use {@link #SC_UNPROCESSABLE_CONTENT}")
    val SC_UNPROCESSABLE_ENTITY: Int = SC_UNPROCESSABLE_CONTENT

    /** `423 Locked` (WebDAV - RFC 2518)  */
    const val SC_LOCKED: Int = 423

    /** `424 Failed Dependency` (WebDAV - RFC 2518)  */
    const val SC_FAILED_DEPENDENCY: Int = 424

    /** `425 Too Early` (Using Early Data in HTTP - RFC 8470)  */
    const val SC_TOO_EARLY: Int = 425

    /** `428 Precondition Required` (Additional HTTP Status Codes - RFC 6585)  */
    const val SC_PRECONDITION_REQUIRED: Int = 428

    /** `429 Too Many Requests` (Additional HTTP Status Codes - RFC 6585)  */
    const val SC_TOO_MANY_REQUESTS: Int = 429

    /** `431 Request Header Fields Too Large` (Additional HTTP Status Codes - RFC 6585)  */
    const val SC_REQUEST_HEADER_FIELDS_TOO_LARGE: Int = 431

    /** `451 Unavailable For Legal Reasons` (Legal Obstacles - RFC 7725)  */
    const val SC_UNAVAILABLE_FOR_LEGAL_REASONS: Int = 451

    // --- 5xx Server Error ---
    /** `500 Server Error` (HTTP Semantics)  */
    const val SC_SERVER_ERROR: Int = 500

    /** `500 Internal Server Error` (HTTP Semantics)  */
    const val SC_INTERNAL_SERVER_ERROR: Int = 500

    /** `501 Not Implemented` (HTTP Semantics)  */
    const val SC_NOT_IMPLEMENTED: Int = 501

    /** `502 Bad Gateway` (HTTP Semantics)  */
    const val SC_BAD_GATEWAY: Int = 502

    /** `503 Service Unavailable` (HTTP Semantics)  */
    const val SC_SERVICE_UNAVAILABLE: Int = 503

    /** `504 Gateway Timeout` (HTTP Semantics)  */
    const val SC_GATEWAY_TIMEOUT: Int = 504

    /** `505 HTTP Version Not Supported` (HTTP Semantics)  */
    const val SC_HTTP_VERSION_NOT_SUPPORTED: Int = 505

    /** `506 Variant Also Negotiates` ( Transparent Content Negotiation - RFC 2295)  */
    const val SC_VARIANT_ALSO_NEGOTIATES: Int = 506

    /** `507 Insufficient Storage` (WebDAV - RFC 2518)  */
    const val SC_INSUFFICIENT_STORAGE: Int = 507

    /**
     * `508 Loop Detected` (WebDAV - RFC 5842, p.33, section 7.2)
     */
    const val SC_LOOP_DETECTED: Int = 508

    /**
     * `510 Not Extended` (An HTTP Extension Framework - RFC 2774, p. 10, section 7)
     */
    const val SC_NOT_EXTENDED: Int = 510

    /** `511 Network Authentication Required` (Additional HTTP Status Codes - RFC 6585)  */
    const val SC_NETWORK_AUTHENTICATION_REQUIRED: Int = 511

    private val REASON_PHRASES = arrayOf(
        arrayOfNulls(0),
        arrayOfNulls(3),
        arrayOfNulls(8),
        arrayOfNulls(8),
        arrayOfNulls(25),
        arrayOfNulls<String>(8)
    )

    fun getStatusText(statusCode: Int): String {
        if (statusCode < 0) {
            throw IllegalArgumentException("status code may not be negative")
        }

        val classIndex = statusCode / 100
        val codeIndex = statusCode - classIndex * 100
        return if (classIndex >= 1
            && classIndex <= REASON_PHRASES.size - 1
            && codeIndex >= 0
            && codeIndex <= REASON_PHRASES[classIndex].size - 1
        ) {
            REASON_PHRASES[classIndex][codeIndex]!!
        } else {
            "unknown"
        }
    }

    /**
     * Stores the given reason phrase, by status code.
     * Helper method to initialize the static lookup table.
     *
     * @param status    the status code for which to define the phrase
     * @param reason    the reason phrase for this status code
     */
    private fun setReason(status: Int, reason: String) {
        val category = status / 100
        val subcode = status - 100 * category
        REASON_PHRASES[category][subcode] = reason
    }

    init {
        // HTTP 1.1 Server status codes -- see RFC 9110
        setReason(SC_OK, "OK")
        setReason(SC_CREATED, "Created")
        setReason(SC_ACCEPTED, "Accepted")
        setReason(SC_NO_CONTENT, "No Content")
        setReason(SC_MOVED_PERMANENTLY, "Moved Permanently")
        setReason(SC_MOVED_TEMPORARILY, "Moved Temporarily")
        setReason(SC_NOT_MODIFIED, "Not Modified")
        setReason(SC_BAD_REQUEST, "Bad Request")
        setReason(SC_UNAUTHORIZED, "Unauthorized")
        setReason(SC_FORBIDDEN, "Forbidden")
        setReason(SC_NOT_FOUND, "Not Found")
        setReason(SC_INTERNAL_SERVER_ERROR, "Internal Server Error")
        setReason(SC_NOT_IMPLEMENTED, "Not Implemented")
        setReason(SC_BAD_GATEWAY, "Bad Gateway")
        setReason(SC_SERVICE_UNAVAILABLE, "Service Unavailable")
        setReason(SC_CONTINUE, "Continue")
        setReason(SC_TEMPORARY_REDIRECT, "Temporary Redirect")
        setReason(SC_METHOD_NOT_ALLOWED, "Method Not Allowed")
        setReason(SC_CONFLICT, "Conflict")
        setReason(SC_PRECONDITION_FAILED, "Precondition Failed")
        setReason(SC_REQUEST_TOO_LONG, "Request Too Long")
        setReason(SC_REQUEST_URI_TOO_LONG, "Request-URI Too Long")
        setReason(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type")
        setReason(SC_MULTIPLE_CHOICES, "Multiple Choices")
        setReason(SC_SEE_OTHER, "See Other")
        setReason(SC_USE_PROXY, "Use Proxy")
        setReason(SC_PAYMENT_REQUIRED, "Payment Required")
        setReason(SC_NOT_ACCEPTABLE, "Not Acceptable")
        setReason(SC_PROXY_AUTHENTICATION_REQUIRED, "Proxy Authentication Required")
        setReason(SC_REQUEST_TIMEOUT, "Request Timeout")
        setReason(SC_SWITCHING_PROTOCOLS, "Switching Protocols")
        setReason(SC_NON_AUTHORITATIVE_INFORMATION, "Non Authoritative Information")
        setReason(SC_RESET_CONTENT, "Reset Content")
        setReason(SC_PARTIAL_CONTENT, "Partial Content")
        setReason(SC_GATEWAY_TIMEOUT, "Gateway Timeout")
        setReason(SC_HTTP_VERSION_NOT_SUPPORTED, "Http Version Not Supported")
        setReason(SC_GONE, "Gone")
        setReason(SC_LENGTH_REQUIRED, "Length Required")
        setReason(SC_REQUESTED_RANGE_NOT_SATISFIABLE, "Requested Range Not Satisfiable")
        setReason(SC_EXPECTATION_FAILED, "Expectation Failed")
        setReason(SC_MISDIRECTED_REQUEST, "Misdirected Request")
        setReason(SC_PROCESSING, "Processing")
        setReason(SC_MULTI_STATUS, "Multi-Status")
        setReason(SC_ALREADY_REPORTED, "Already Reported")
        setReason(SC_IM_USED, "IM Used")
        setReason(SC_UNPROCESSABLE_CONTENT, "Unprocessable Content")
        setReason(SC_INSUFFICIENT_SPACE_ON_RESOURCE, "Insufficient Space On Resource")
        setReason(SC_METHOD_FAILURE, "Method Failure")
        setReason(SC_LOCKED, "Locked")
        setReason(SC_INSUFFICIENT_STORAGE, "Insufficient Storage")
        setReason(SC_LOOP_DETECTED, "Loop Detected")
        setReason(SC_NOT_EXTENDED, "Not Extended")
        setReason(SC_FAILED_DEPENDENCY, "Failed Dependency")
        setReason(SC_TOO_EARLY, "Too Early")
        setReason(SC_UPGRADE_REQUIRED, "Upgrade Required")
        setReason(SC_PRECONDITION_REQUIRED, "Precondition Required")
        setReason(SC_TOO_MANY_REQUESTS, "Too Many Requests")
        setReason(SC_REQUEST_HEADER_FIELDS_TOO_LARGE, "Request Header Fields Too Large")
        setReason(SC_NETWORK_AUTHENTICATION_REQUIRED, "Network Authentication Required")
        setReason(SC_EARLY_HINTS, "Early Hints")
        setReason(SC_PERMANENT_REDIRECT, "Permanent Redirect")
        setReason(SC_UNAVAILABLE_FOR_LEGAL_REASONS, "Unavailable For Legal Reasons")
        setReason(SC_VARIANT_ALSO_NEGOTIATES, "Variant Also Negotiates")
    }
}
