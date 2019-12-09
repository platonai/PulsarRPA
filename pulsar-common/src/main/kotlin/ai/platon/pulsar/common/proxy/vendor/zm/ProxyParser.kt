package ai.platon.pulsar.common.proxy.vendor.zm

import ai.platon.pulsar.common.DateTimeDetector
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.vendor.ProxyParser
import ai.platon.pulsar.common.proxy.vendor.ProxyVendorException
import com.google.gson.GsonBuilder
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
                if (result.code == 113) {
                    val ip = result.msg.substringAfter("请添加白名单")
                    val link = "wapi.http.cnapi.cc/index/index/save_white?neek=76534&appkey=2d5f64c71bdf2b6e632f951c7aab2c9b&white=$ip"
                    log.warn(result.msg + " using the following link:\n$link")
                    throw ProxyVendorException("Proxy vendor exception, please add $ip to the vendor's while list")
                } else {
                    throw ProxyVendorException("Proxy vendor exception - $text")
                }
            }
        }
        return listOf()
    }

    private fun parseInstant(str: String): Instant {
        return dateTimeDetector.parseDateTimeStrictly(str).toInstant()
    }
}
