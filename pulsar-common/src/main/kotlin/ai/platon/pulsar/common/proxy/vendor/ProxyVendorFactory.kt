package ai.platon.pulsar.common.proxy.vendor

import ai.platon.pulsar.common.proxy.ProxyEntry

abstract class ProxyParser {
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
