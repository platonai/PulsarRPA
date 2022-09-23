package ai.platon.pulsar.crawl.fetch.driver

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

open class BrowserException(
    message: String? = null,
    cause: Throwable? = null
): RuntimeException(message, cause) {

    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}
