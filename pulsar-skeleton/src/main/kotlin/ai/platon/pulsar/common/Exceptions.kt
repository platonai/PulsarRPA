package ai.platon.pulsar.common

class IllegalApplicationContextStateException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}
