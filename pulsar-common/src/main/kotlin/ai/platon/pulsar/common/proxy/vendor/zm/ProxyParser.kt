package ai.platon.pulsar.common.proxy.vendor.zm

import ai.platon.pulsar.common.DateTimeDetector
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.vendor.ProxyParser
import ai.platon.pulsar.common.proxy.vendor.ProxyVendorException
import com.google.gson.GsonBuilder
import java.lang.RuntimeException
import java.time.Instant

private class ProxyItem(
        val ip: String = "",
        val port: Int = 0,
        val expire_time: String = "",
        val city: String = "",
        val isp: String = "",
        val outip: String = ""
)

private class ProxyResult(
        val code: Int = 0,
        val msg: String = "0",
        val success: Boolean = false,
        val data: List<ProxyItem> = listOf()
)

class ZMProxyParser: ProxyParser() {
    val gson = GsonBuilder().create()
    val dateTimeDetector = DateTimeDetector()

    override fun parse(text: String, format: String): List<ProxyEntry> {
        if (format == "json") {
            val result = gson.fromJson(text, ProxyResult::class.java)
            if (result.success) {
                return result.data.map { ProxyEntry(it.ip, it.port, declaredTTL = parseInstant(it.expire_time)) }
            }
            if (result.code != 0) {
                throw ProxyVendorException("Proxy vendor exception - $text")
            }
        }
        return listOf()
    }

    private fun parseInstant(str: String): Instant {
        return dateTimeDetector.parseDateTimeStrictly(str).toInstant()
    }
}
