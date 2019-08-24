package ai.platon.pulsar.common

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.vendor.ProxyVendorFactory
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals

class TestProxy {
    @Test
    fun testParseProxyEntry() {
        val proxies = arrayOf(
                "58.218.200.226:6925 at:2019-08-24T15:34:28.255Z, spd:0.0",
                "58.218.200.228:4169 at:2019-08-24T15:34:28.255Z, spd:0.0",
                "58.218.200.226:6008 at:2019-08-24T15:34:28.255Z, ttl:2019-08-24T16:31:24.215Z, spd:0.0"
        )
        proxies.forEach {
            assertEquals(it, ProxyEntry.parse(it).toString())
        }
    }

    @Test
    fun testParseVendorZM() {
        val json = ResourceLoader.readString("proxy/vendor/zm/result_sample.json")
        val parser = ProxyVendorFactory.getProxyParser("zm")
        val proxyEntries = parser.parse(json, "json")

        proxyEntries.forEach {
            println("$it - ttl:${it.ttl}")
        }

        assertEquals(3, proxyEntries.size)
        assertEquals(toReadableLocalDateTime(proxyEntries[0].ttl!!), "2019-08-24T21:55:02")
        assertEquals(toReadableLocalDateTime(proxyEntries[1].ttl!!), "2019-08-24T22:07:31")
        assertEquals(toReadableLocalDateTime(proxyEntries[2].ttl!!), "2019-08-24T21:58:21")
    }

    private fun toReadableLocalDateTime(instant: Instant): String {
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime().toString()
    }
}
