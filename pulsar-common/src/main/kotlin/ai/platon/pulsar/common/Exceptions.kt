package ai.platon.pulsar.common

import org.slf4j.Logger

open class NoSuchCriticalObjectException : RuntimeException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class NotSupportedException : RuntimeException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

fun Throwable.stringify(prefix: String = "", postfix: String = "") = stringifyException(this, prefix, postfix)

@Deprecated("Inappropriate name.", ReplaceWith("Throwable.brief"))
fun Throwable.simplify(prefix: String = "", postfix: String = "") = simplifyException(this, prefix, postfix)

fun Throwable.brief(prefix: String = "", postfix: String = "") = simplifyException(this, prefix, postfix)
