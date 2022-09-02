package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException

open class SessionException(
    message: String? = null,
    driver: WebDriver? = null,
    cause: Throwable? = null
): WebDriverException(message, driver, cause) {
    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}

open class SessionLostException(
    message: String? = null,
    driver: WebDriver? = null,
    cause: Throwable? = null
): WebDriverException(message, driver, cause) {
    constructor(message: String?, cause: Throwable) : this(message, null, cause)

    constructor(cause: Throwable?) : this(null, null, cause)
}
