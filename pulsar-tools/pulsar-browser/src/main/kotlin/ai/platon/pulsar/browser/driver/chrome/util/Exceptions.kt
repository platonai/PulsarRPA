package ai.platon.pulsar.browser.driver.chrome.util

open class ChromeDriverException: RuntimeException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ChromeProtocolException: ChromeDriverException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ChromeProcessException: ChromeProtocolException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ChromeProcessTimeoutException : ChromeProcessException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ChromeLaunchException : ChromeDriverException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class WebSocketServiceException : ChromeProtocolException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ChromeServiceException : ChromeProtocolException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class ScreenshotException : ChromeServiceException {
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

open class ChromeLostException : ChromeRPCException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}
