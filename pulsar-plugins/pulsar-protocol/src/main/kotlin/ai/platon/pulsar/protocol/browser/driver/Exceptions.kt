package ai.platon.pulsar.protocol.browser.driver

open class WebDriverException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String?) : super(message) {
    }

    constructor(message: String?, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable?) : super(cause) {
    }
}

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
