package ai.platon.pulsar.common

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.urls.Urls
import org.junit.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-1-14.
 */
class TestProxy {

    @Test
    fun testProxyEntry() {
        ResourceLoader
            .readAllLines(ProxyEntry.PROXY_TEST_WEB_SITES_FILE)
            .mapNotNullTo(ProxyEntry.TEST_URLS) { Urls.getURLOrNull(it) }
        assertTrue(ProxyEntry.TEST_URLS.isNotEmpty())
        assertTrue(URL("http://www.dongqiudi.com") in ProxyEntry.TEST_URLS)
//        ResourceLoader.readAllLines(ProxyEntry.PROXY_TEST_WEB_SITES_FILE).mapNotNullTo(ProxyEntry.TEST_URLS) { Urls.getURLOrNull(it) }
//        ProxyEntry.TEST_URLS.forEach { println(it) }

//        println("hello")
//        val testProxy = ProxyEntry("117.90.220.193", 4216)
//        println(testProxy)
    }

    @Test
    fun testParseProxyEntry() {
        val proxies = arrayOf(
            "58.218.200.226:6925 at:2019-08-24T15:34:28.255Z, spd:0.0",
            "58.218.200.228:4169 at:2019-08-24T15:34:28.255Z, spd:0.0",
            "58.218.200.226:6008 at:2019-08-24T15:34:28.255Z, ttl:2019-08-24T16:31:24.215Z, spd:0.0"
        )
        proxies.forEach {
            assertEquals(it.substringBefore(", "), ProxyEntry.parse(it)?.serialize()?.substringBefore(", "))
        }
    }
}
