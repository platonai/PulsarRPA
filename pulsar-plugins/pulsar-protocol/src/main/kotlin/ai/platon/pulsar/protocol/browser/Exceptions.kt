package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserUnavailableException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException

class UnsupportedWebDriverException : WebDriverException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}
