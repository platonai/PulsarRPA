package ai.platon.pulsar.common

open class NotSupportedException : RuntimeException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class IllegalApplicationStateException: IllegalStateException {
    constructor() : super() {}

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

fun Throwable.stringify(prefix: String = "", postfix: String = "") = stringifyException(this, prefix, postfix)

fun Throwable.brief(prefix: String = "", postfix: String = "") = simplifyException(this, prefix, postfix)
