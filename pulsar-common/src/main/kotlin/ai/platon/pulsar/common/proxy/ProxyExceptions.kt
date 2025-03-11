package ai.platon.pulsar.common.proxy

open class ProxyException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class NoProxyException(message: String, cause: Throwable? = null) : ProxyException(message, cause)

open class ProxyRetiredException(message: String, cause: Throwable? = null) : ProxyException(message, cause)

open class ProxyRetryException(message: String, cause: Throwable? = null) : ProxyException(message, cause)

open class ProxyGoneException(message: String, cause: Throwable? = null) : ProxyException(message, cause)

open class ProxyVendorException(message: String, cause: Throwable? = null) : ProxyException(message, cause)

open class ProxyInsufficientBalanceException(message: String, cause: Throwable? = null) : ProxyVendorException(message, cause)

open class ProxyVendorUntrustedException(message: String, cause: Throwable? = null) : ProxyVendorException(message, cause)
