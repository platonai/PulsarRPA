package ai.platon.pulsar.common.proxy

open class ProxyException : RuntimeException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class NoProxyException(message: String) : ProxyException(message)

open class ProxyExpireException(message: String) : ProxyException(message)

open class ProxyGoneException(message: String) : ProxyException(message)

open class ProxyVendorException : Exception {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class ProxyVendorUntrustedException(message: String) : RuntimeException(message)
