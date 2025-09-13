package ai.platon.pulsar.common.http

interface HttpStatusCode {

    /**
     * Return the integer value of this status code.
     */
    val value: Int

    /**
     * Whether this status code is in the Informational class (`1xx`).
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.1)
     */
    fun is1xxInformational(): Boolean

    /**
     * Whether this status code is in the Successful class (`2xx`).
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.2)
     */
    fun is2xxSuccessful(): Boolean

    /**
     * Whether this status code is in the Redirection class (`3xx`).
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.3)
     */
    fun is3xxRedirection(): Boolean

    /**
     * Whether this status code is in the Client Error class (`4xx`).
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.4)
     */
    fun is4xxClientError(): Boolean

    /**
     * Whether this status code is in the Server Error class (`5xx`).
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.5)
     */
    fun is5xxServerError(): Boolean

    /**
     * Whether this status code is in the Client or Server Error class
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.4)
     *
     * @see [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616.section-10.3)
     *
     * @see .is4xxClientError
     * @see .is5xxServerError
     */
    fun isError(): Boolean

    /**
     * Whether this `HttpStatusCode` shares the same integer [value][.value] as the other status code.
     *
     * Useful for comparisons that take deprecated aliases into account or compare arbitrary implementations
     * of `HttpStatusCode` (e.g. in place of [HttpStatus enum equality][HttpStatus.equals]).
     * @param other the other `HttpStatusCode` to compare
     * @return true if the two `HttpStatusCode` objects share the same integer `value()`, false otherwise
     * @since 6.0.5
     */
    fun isSameCodeAs(other: HttpStatusCode): Boolean {
        return value == other.value
    }

    /**
     * Return an `HttpStatusCode` object for the given integer value.
     * @param code the status code as integer
     * @return the corresponding `HttpStatusCode`
     * @throws IllegalArgumentException if `code` is not a three-digit
     * positive number
     */
    fun valueOf(code: Int): HttpStatusCode {
        require(code in 100..999
        ) { "Status code '$code' should be a three-digit positive integer" }
        val status = HttpStatus.resolve(code)
        return status ?: DefaultHttpStatusCode(code)
    }
}
