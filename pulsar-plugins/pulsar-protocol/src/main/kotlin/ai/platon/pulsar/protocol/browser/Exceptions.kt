package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.crawl.fetch.driver.WebDriverException

class UnsupportedWebDriverException : WebDriverException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

/**
 * TODO: a better name: BrowserLaunchException
 * */
class DriverLaunchException : WebDriverException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}
