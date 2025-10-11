package ai.platon.pulsar.common.browser

/**
 *
 * */
enum class BrowserErrorCode {
    ERR_CONNECTION_TIMED_OUT,
    ERR_INTERNET_DISCONNECTED,
    ERR_NAME_NOT_RESOLVED,
    ERR_SSL_PROTOCOL_ERROR,
    ERR_NETWORK_CHANGED,
    ERR_CONNECTION_REFUSED,
    ERR_CACHE_MISS,
    ERR_NO_SUPPORTED_PROXIES,
    ERR_CONNECTION_CLOSED,
    ERR_EMPTY_RESPONSE,
    ERR_CONNECTION_RESET,
    ERR_TIMED_OUT,
    ERR_PROXY_CONNECTION_FAILED,
    ERR_SSL_BAD_RECORD_MAC_ALERT,
    ERR_UNKNOWN
    ;

    fun isUnknown(): Boolean {
        return this == ERR_UNKNOWN
    }

    companion object {
        fun valueOfOrNull(error: String): BrowserErrorCode? {
            return kotlin.runCatching { valueOf(error) }.getOrNull()
        }

        fun valueOfOrUnknown(error: String): BrowserErrorCode {
            return valueOfOrNull(error) ?: ERR_UNKNOWN
        }
    }
}
