package ai.platon.pulsar.common.browser

enum class ChromeError {
    ERR_CONNECTION_TIMED_OUT,
    ERR_NO_SUPPORTED_PROXIES,
    ERR_CONNECTION_CLOSED,
    ERR_EMPTY_RESPONSE,
    ERR_CONNECTION_RESET,
    ERR_TIMED_OUT,
    ERR_PROXY_CONNECTION_FAILED,
    ERR_SSL_BAD_RECORD_MAC_ALERT;

    companion object {
        fun valueOfOrNull(error: String): ChromeError? {
            return kotlin.runCatching { valueOf(error) }.getOrNull()
        }
    }
}
