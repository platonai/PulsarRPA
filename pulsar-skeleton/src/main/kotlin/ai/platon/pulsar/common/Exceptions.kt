package ai.platon.pulsar.common

open class ServiceUnavailableException: RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}

open class IllegalBusinessPreconditionException: IllegalStateException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}

open class IllegalApplicationStateException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

@Deprecated("Inappropriate name", ReplaceWith("IllegalApplicationStateException"))
class IllegalApplicationContextStateException: IllegalApplicationStateException {
    constructor() : super() {}
    
    constructor(message: String) : super(message) {
    }
    
    constructor(message: String, cause: Throwable) : super(message, cause) {}
    
    constructor(cause: Throwable) : super(cause) {}
}
