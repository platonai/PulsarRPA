package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.persist.ProtocolStatus

class CancellationException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

class PrivacyLeakException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
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

class IllegalContextStateException: IllegalStateException {
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
