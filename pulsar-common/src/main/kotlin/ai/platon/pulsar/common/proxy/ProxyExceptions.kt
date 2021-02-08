package ai.platon.pulsar.common.proxy

open class ProxyException : RuntimeException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class NoProxyException(message: String) : ProxyException(message)

open class ProxyRetiredException(message: String) : ProxyException(message)

open class ProxyGoneException(message: String) : ProxyException(message)

open class ProxyVendorException : ProxyException {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

open class ProxyInsufficientBalanceException(message: String) : ProxyException(message)

open class ProxyVendorUntrustedException(message: String) : ProxyException(message)
