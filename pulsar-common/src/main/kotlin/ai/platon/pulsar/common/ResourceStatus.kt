package ai.platon.pulsar.common

/**
 * Keep consistent with standard http status
 *
 * @link {https://en.wikipedia.org/wiki/List_of_HTTP_status_codes}
 * @link {https://developer.mozilla.org/en-US/docs/Web/HTTP/Status}
 * @link {http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpStatus.html}
 *
 * @author vincent
 * @version $Id: $Id
*/
object ResourceStatus {
    private val REASON_PHRASES = arrayOf(
            arrayOfNulls(0),
            arrayOfNulls(3),
            arrayOfNulls(8),
            arrayOfNulls(8),
            arrayOfNulls(25),
            arrayOfNulls<String>(8)
    )
    const val SC_CONTINUE = 100
    const val SC_SWITCHING_PROTOCOLS = 101
    const val SC_PROCESSING = 102
    const val SC_OK = 200
    const val SC_CREATED = 201
    const val SC_ACCEPTED = 202
    const val SC_NON_AUTHORITATIVE_INFORMATION = 203
    const val SC_NO_CONTENT = 204
    const val SC_RESET_CONTENT = 205
    const val SC_PARTIAL_CONTENT = 206
    const val SC_MULTI_STATUS = 207
    const val SC_MULTIPLE_CHOICES = 300
    const val SC_MOVED_PERMANENTLY = 301
    const val SC_MOVED_TEMPORARILY = 302
    const val SC_SEE_OTHER = 303
    const val SC_NOT_MODIFIED = 304
    const val SC_USE_PROXY = 305
    const val SC_TEMPORARY_REDIRECT = 307
    const val SC_BAD_REQUEST = 400
    const val SC_UNAUTHORIZED = 401
    const val SC_PAYMENT_REQUIRED = 402
    const val SC_FORBIDDEN = 403
    const val SC_NOT_FOUND = 404
    const val SC_METHOD_NOT_ALLOWED = 405
    const val SC_NOT_ACCEPTABLE = 406
    const val SC_PROXY_AUTHENTICATION_REQUIRED = 407
    const val SC_REQUEST_TIMEOUT = 408
    const val SC_CONFLICT = 409
    const val SC_GONE = 410
    const val SC_LENGTH_REQUIRED = 411
    const val SC_PRECONDITION_FAILED = 412
    const val SC_REQUEST_TOO_LONG = 413
    const val SC_REQUEST_URI_TOO_LONG = 414
    const val SC_UNSUPPORTED_MEDIA_TYPE = 415
    const val SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416
    const val SC_EXPECTATION_FAILED = 417
    const val SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419
    const val SC_METHOD_FAILURE = 420
    const val SC_UNPROCESSABLE_ENTITY = 422
    const val SC_LOCKED = 423
    const val SC_FAILED_DEPENDENCY = 424
    const val SC_INTERNAL_SERVER_ERROR = 500
    const val SC_NOT_IMPLEMENTED = 501
    const val SC_BAD_GATEWAY = 502
    const val SC_SERVICE_UNAVAILABLE = 503
    const val SC_GATEWAY_TIMEOUT = 504
    const val SC_HTTP_VERSION_NOT_SUPPORTED = 505
    const val SC_INSUFFICIENT_STORAGE = 507

    fun getStatusText(statusCode: Int): String {
        if (statusCode < 0) {
            throw IllegalArgumentException("status code may not be negative")
        }

        val classIndex = statusCode / 100
        val codeIndex = statusCode - classIndex * 100
        if (classIndex >= 1
                && classIndex <= REASON_PHRASES.size - 1
                && codeIndex >= 0
                && codeIndex <= REASON_PHRASES[classIndex].size - 1) {
            return REASON_PHRASES[classIndex][codeIndex]!!
        } else {
            return "unknown"
        }
    }

    private fun addStatusCodeMap(statusCode: Int, reasonPhrase: String) {
        val classIndex = statusCode / 100
        REASON_PHRASES[classIndex][statusCode - classIndex * 100] = reasonPhrase
    }

    init {
        addStatusCodeMap(200, "OK")
        addStatusCodeMap(201, "Created")
        addStatusCodeMap(202, "Accepted")
        addStatusCodeMap(204, "No Content")
        addStatusCodeMap(301, "Moved Permanently")
        addStatusCodeMap(302, "Moved Temporarily")
        addStatusCodeMap(304, "Not Modified")
        addStatusCodeMap(400, "Bad Request")
        addStatusCodeMap(401, "Unauthorized")
        addStatusCodeMap(403, "Forbidden")
        addStatusCodeMap(404, "Not Found")
        addStatusCodeMap(500, "Internal Server Error")
        addStatusCodeMap(501, "Not Implemented")
        addStatusCodeMap(502, "Bad Gateway")
        addStatusCodeMap(503, "Service Unavailable")
        addStatusCodeMap(100, "Continue")
        addStatusCodeMap(307, "Temporary Redirect")
        addStatusCodeMap(405, "Method Not Allowed")
        addStatusCodeMap(409, "Conflict")
        addStatusCodeMap(412, "Precondition Failed")
        addStatusCodeMap(413, "Request Too Long")
        addStatusCodeMap(414, "Request-URI Too Long")
        addStatusCodeMap(415, "Unsupported Media Type")
        addStatusCodeMap(300, "Multiple Choices")
        addStatusCodeMap(303, "See Other")
        addStatusCodeMap(305, "Use Proxy")
        addStatusCodeMap(402, "Payment Required")
        addStatusCodeMap(406, "Not Acceptable")
        addStatusCodeMap(407, "Proxy Authentication Required")
        addStatusCodeMap(408, "Request Timeout")
        addStatusCodeMap(101, "Switching Protocols")
        addStatusCodeMap(203, "Non Authoritative Information")
        addStatusCodeMap(205, "Reset Content")
        addStatusCodeMap(206, "Partial Content")
        addStatusCodeMap(504, "Gateway Timeout")
        addStatusCodeMap(505, "Http Version Not Supported")
        addStatusCodeMap(410, "Gone")
        addStatusCodeMap(411, "Length Required")
        addStatusCodeMap(416, "Requested Range Not Satisfiable")
        addStatusCodeMap(417, "Expectation Failed")
        addStatusCodeMap(102, "Processing")
        addStatusCodeMap(207, "Multi-Status")
        addStatusCodeMap(422, "Unprocessable Entity")
        addStatusCodeMap(419, "Insufficient Space On Resource")
        addStatusCodeMap(420, "Method Failure")
        addStatusCodeMap(423, "Locked")
        addStatusCodeMap(507, "Insufficient Storage")
        addStatusCodeMap(424, "Failed Dependency")
    }
}
