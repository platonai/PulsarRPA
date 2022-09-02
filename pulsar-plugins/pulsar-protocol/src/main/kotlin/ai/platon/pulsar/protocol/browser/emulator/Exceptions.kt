package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.crawl.fetch.driver.WebDriverException

class NavigateTaskCancellationException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

open class WebDriverPoolException: WebDriverException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

class WebDriverPoolExhaustedException(message: String) : WebDriverPoolException(message)
