package ai.platon.pulsar.common.proxy

open class ProxyException : RuntimeException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class ProxyInactiveException(message: String) : ProxyException(message)

open class NoProxyException(message: String) : ProxyException(message)

open class ProxyGoneException(message: String) : ProxyException(message)
