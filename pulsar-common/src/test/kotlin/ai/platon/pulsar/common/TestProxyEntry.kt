package ai.platon.pulsar.common

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.urls.UrlUtils
import org.junit.Test
import java.net.Proxy
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-1-14.
 */
class TestProxyEntry {
    
    @Test
    fun testProxySchema() {
        val proxy = ProxyEntry("127.0.0.1", 10808, "abc", "abc", Proxy.Type.SOCKS)
        // println(proxy.toURI())
        assertEquals("socks5", proxy.protocol)
        assertEquals("socks5://abc:abc@127.0.0.1:10808", proxy.toURI().toString())
    }
    
    @Test
    fun testTestUrls() {
        ResourceLoader
                .readAllLines(ProxyEntry.PROXY_TEST_WEB_SITES_FILE)
                .mapNotNullTo(ProxyEntry.TEST_URLS) { UrlUtils.getURLOrNull(it) }
        assertTrue(ProxyEntry.TEST_URLS.isNotEmpty())
        assertTrue(URL("http://www.dongqiudi.com") in ProxyEntry.TEST_URLS)
    }

    @Test
    fun testParseProxyEntry() {
        val proxies = arrayOf(
                "58.218.200.226:6925 at:2019-08-24T15:34:28.255Z, spd:0.0",
                "58.218.200.228:4169 at:2019-08-24T15:34:28.255Z, spd:0.0",
                "58.218.200.226:6008 at:2019-08-24T15:34:28.255Z, ttl:2019-08-24T16:31:24.215Z, spd:0.0",
                "58.218.200.226:6008 at:2019-08-24T15:34:28.255Z, ttl:2019-08-24T16:31:24.215Z, usr:abc, pwd:abc",
        )
        proxies.forEach {
            assertEquals(it.substringBefore(", "), ProxyEntry.parse(it)?.serialize()?.substringBefore(", "))
        }
    }

    @Test
    fun testParsingProxyEntryWithAuth() {
        val proxies = mapOf(
                "58.218.200.226:6008 at:2019-08-24T15:34:28.255Z, ttl:2019-08-24T16:31:24.215Z, usr:abc, pwd:123" to
                        ProxyEntry("58.218.200.226", 6008, "abc", "123"),
        )

        proxies.forEach { (proxyString, expected) ->
            val actual = ProxyEntry.parse(proxyString)
            assertNotNull(actual)
            assertEquals(expected.host, actual.host)
            assertEquals(expected.port, actual.port)
            assertEquals(expected.username, actual.username)
            assertEquals(expected.password, actual.password)
            assertEquals(expected, actual)
        }
    }
}
