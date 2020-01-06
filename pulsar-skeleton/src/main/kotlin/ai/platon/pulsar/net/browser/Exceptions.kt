package ai.platon.pulsar.net.browser

import ai.platon.pulsar.persist.ProtocolStatus

class ContextResetException: Exception {
    var sponsorThreadId = 0L

    constructor() : super() {}

    constructor(sponsorThreadId: Long, message: String) : super(message) {
        this.sponsorThreadId = sponsorThreadId
    }

    constructor(sponsorThreadId: Long, message: String, cause: Throwable) : super(message, cause) {
        this.sponsorThreadId = sponsorThreadId
    }

    constructor(sponsorThreadId: Long, cause: Throwable) : super(cause) {
        this.sponsorThreadId = sponsorThreadId
    }
}

class IncompleteContentException: Exception {
    var status: ProtocolStatus = ProtocolStatus.STATUS_EXCEPTION
    var content: String = ""

    constructor() : super() {}

    constructor(message: String, status: ProtocolStatus, content: String) : super(message) {
        this.content = content
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

class IllegalBrowseContextStateException: Exception {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

class WebDriverPoolExhaust: Exception {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}
