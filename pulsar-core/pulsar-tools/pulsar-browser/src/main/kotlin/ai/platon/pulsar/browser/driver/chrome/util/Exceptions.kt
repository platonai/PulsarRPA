package ai.platon.pulsar.browser.driver.chrome.util

open class ChromeDriverException(
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)

/**
 * Thrown when failed to launch Chrome.
 * This is a fatal exception and the browser should be closed.
 * */
open class ChromeLaunchException(message: String, cause: Throwable? = null): ChromeDriverException(message, cause)

/**
 * Thrown when the connection to Chrome fails.
 * This is a fatal exception and the browser should be closed.
 * */
open class ChromeLaunchTimeoutException(message: String, cause: Throwable? = null): ChromeLaunchException(message, cause)

open class ChromeProtocolException(
    message: String,
    cause: Throwable? = null,
): ChromeDriverException(message, cause)

/**
 * Thrown when the connection to Chrome fails.
 * This is a fatal exception and the browser should be closed.
 * */
open class ChromeIOException(
    message: String,
    cause: Throwable? = null,
    var isOpen: Boolean = true,
) : ChromeProtocolException(message, cause)

open class ChromeServiceException : ChromeProtocolException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ChromeRPCException : ChromeProtocolException {
    var code = -1L

    constructor(message: String) : super(message)

    constructor(code: Long, message: String) : super(message) {
        this.code = code
    }

    constructor(message: String, cause: Throwable) : super(message, cause)
}

open class ChromeRPCTimeoutException : ChromeRPCException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}
