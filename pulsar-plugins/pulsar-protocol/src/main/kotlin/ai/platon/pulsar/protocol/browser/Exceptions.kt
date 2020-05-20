package ai.platon.pulsar.protocol.browser

class DriverLaunchException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}
