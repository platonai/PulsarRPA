package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.common.browser.BrowserErrorCode

open class WebDriverException(
    message: String? = null,
    val driver: WebDriver? = null,
    cause: Throwable? = null
): RuntimeException(message, cause) {

    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}

open class WebDriverCancellationException(
    message: String? = null,
    driver: WebDriver? = null,
    cause: Throwable? = null
): WebDriverException(message, driver, cause) {
    constructor(message: String?, cause: Throwable) : this(message, null, cause)
    
    constructor(cause: Throwable?) : this(null, null, cause)
}

open class IllegalWebDriverStateException(
    message: String? = null,
    driver: WebDriver? = null,
    cause: Throwable? = null
): WebDriverException(message, driver, cause) {
    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}

open class BrowserUnavailableException(
    message: String? = null,
    cause: Throwable? = null
): IllegalWebDriverStateException(message, null, cause) {
    constructor(cause: Throwable?) : this(null, cause)
}

class BrowserLaunchException : BrowserUnavailableException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

open class BrowserErrorPageException(
    errorCode: BrowserErrorCode,
    message: String? = null,
    pageContent: String? = null,
    cause: Throwable? = null
): RuntimeException(message, cause)
