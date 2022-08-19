package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.crawl.fetch.driver.WebDriverException

open class SessionException: WebDriverException {
    constructor() : super() {}

    constructor(message: String?) : super(message) {
    }

    constructor(message: String?, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable?) : super(cause) {
    }
}

open class NoSuchSessionException: SessionException {
    constructor() : super() {}

    constructor(message: String?) : super(message) {
    }

    constructor(message: String?, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable?) : super(cause) {
    }
}
