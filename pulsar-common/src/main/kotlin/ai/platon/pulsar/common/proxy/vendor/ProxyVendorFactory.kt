package ai.platon.pulsar.common.proxy.vendor

import ai.platon.pulsar.common.proxy.ProxyEntry
import org.slf4j.LoggerFactory

open class ProxyVendorException : Exception {

    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

abstract class ProxyParser {
    val log = LoggerFactory.getLogger(ProxyParser::class.java)
    abstract fun parse(text: String, format: String): List<ProxyEntry>
}

class DefaultProxyParser: ProxyParser() {
    override fun parse(text: String, format: String): List<ProxyEntry> {
        return text.split("\n").mapNotNull { ProxyEntry.parse(text) }
    }
}

object ProxyVendorFactory {
    fun getProxyParser(vendor: String): ProxyParser {
        return when (vendor) {
            "zm" -> ai.platon.pulsar.common.proxy.vendor.zm.ZMProxyParser()
            else -> DefaultProxyParser()
        }
    }
}
