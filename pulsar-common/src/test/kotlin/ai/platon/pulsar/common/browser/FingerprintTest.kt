package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class FingerprintTest {
    val fingerprints = listOf(
        Fingerprint(BrowserType.PULSAR_CHROME),
        Fingerprint(BrowserType.PLAYWRIGHT_CHROME),
        
        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1"),
        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.2"),
        
        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa"),
        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sb"),
        
        Fingerprint(BrowserType.PULSAR_CHROME),
        Fingerprint(BrowserType.PULSAR_CHROME),
    )
    
    val ua =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
    
    @Test
    fun testSetProxy() {
        val fingerprint = Fingerprint(BrowserType.PULSAR_CHROME)
        fingerprint.setProxy("http", "localhost:8080", null, null)
        assertEquals("http://localhost:8080", fingerprint.proxyURI.toString())
    }
    
    @Test
    fun testCompareTo() {
        val fingerprint1 = Fingerprint(BrowserType.PULSAR_CHROME)
        val fingerprint2 = Fingerprint(BrowserType.PULSAR_CHROME)
        assertEquals(0, fingerprint1.compareTo(fingerprint2))
        fingerprint2.proxyURI = URI("http://localhost:8080")
        assertTrue(fingerprint1 < fingerprint2)
        fingerprint1.proxyURI = URI("http://localhost:8080")
    }
    
    @Test
    fun testEquals() {
        val fingerprint1 = Fingerprint(BrowserType.PULSAR_CHROME)
        val fingerprint2 = Fingerprint(BrowserType.PULSAR_CHROME)
        assertEquals(fingerprint1, fingerprint2)
        fingerprint2.proxyURI = URI("http://localhost:8080")
        assertNotEquals(fingerprint1, fingerprint2)
        fingerprint1.proxyURI = URI("http://localhost:8080")
        assertEquals(fingerprint1, fingerprint2)
    }
    @Test
    fun testHashCode() {
        val fingerprint1 = Fingerprint(BrowserType.PULSAR_CHROME)
        val fingerprint2 = Fingerprint(BrowserType.PULSAR_CHROME)
        assertEquals(fingerprint1.hashCode(), fingerprint2.hashCode())
        fingerprint2.proxyURI = URI("http://localhost:8080")
        assertNotEquals(fingerprint1.hashCode(), fingerprint2.hashCode())
        fingerprint1.proxyURI = URI("http://localhost:8080")
        assertEquals(fingerprint1.hashCode(), fingerprint2.hashCode())
    }
    
    @Test
    fun testToString() {
        val fingerprint = Fingerprint(BrowserType.PULSAR_CHROME)
        assertEquals("PULSAR_CHROME", fingerprint.toString())
    }
    
    @Test
    fun testToJSON() {
        val fingerprint = Fingerprint(
            BrowserType.PULSAR_CHROME, URI("http://sa:sa@localhost:8080"), ua
        )
        val json = prettyPulsarObjectMapper().writeValueAsString(fingerprint)
        val obj = prettyPulsarObjectMapper().readValue(json, Fingerprint::class.java)
        assertEquals(fingerprint, obj)
    }
    
    @Test
    fun testEquality() {
        var f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        var f2 = Fingerprint(BrowserType.PULSAR_CHROME)
        kotlin.test.assertEquals(f1, f2)
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        kotlin.test.assertEquals(f1, f2)
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa")
        kotlin.test.assertEquals(f1, f2)
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        f2 = Fingerprint(BrowserType.PULSAR_CHROME)
        kotlin.test.assertEquals(f1, f2)
    }
    
    @Test
    fun testComparison() {
        var f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        var f2 = Fingerprint(BrowserType.PLAYWRIGHT_CHROME)
        
        kotlin.test.assertTrue("L should be less then U in alphabetical order") { "L" < "U" }
        kotlin.test.assertTrue("PLAYWRIGHT_CHROME should < PULSAR_CHROME") {
            BrowserType.PLAYWRIGHT_CHROME.name < BrowserType.PULSAR_CHROME.name
        }
        kotlin.test.assertTrue { f1.compareTo(f2) > 0 }
        kotlin.test.assertTrue { f1 > f2 }
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.2")
        kotlin.test.assertTrue { f1 < f2 }
        
        println("Compare with username ...")
        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sb")
        kotlin.test.assertTrue { f1 < f2 }
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        f2 = Fingerprint(BrowserType.PULSAR_CHROME)
        kotlin.test.assertTrue { f1 < f2 }
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.2")
        kotlin.test.assertTrue { f1 < f2 }
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sb")
        kotlin.test.assertTrue { f1 < f2 }
        
        f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        kotlin.test.assertTrue { f1 < f2 }
    }
    
    @Test
    fun testParseProviderFormat() {
        val providerProxies = """
            113.219.171.252|2018|gdhx22x|ntmf23x123|2024-12-21
            125.124.254.178|5888|gdhx22x|ntmf23x123|2024-12-21
            111.179.91.136|2021|xjkw20k|ntmf23x123|2024-12-21
            58.52.216.7|2021|xjkw20k|ntmf23x123|2024-12-21
            222.243.55.104|2018|ntmf23x|ntmf23x123|2024-12-21
            58.49.229.123|2021|ntmf23x|ntmf23x123|2024-12-21
            223.244.7.17|2018|olhb23q|ntmf23x123|2024-12-21
            223.15.243.217|1000|olhb23q|ntmf23x123|2024-12-21
            218.14.199.43|2019|sgrw19j|sgrw19j|2024-12-21
            182.242.57.34|2018|sgrw19j|sgrw19j|2024-12-21
        """.trimIndent()
        
        val proxies = providerProxies.lines().map {
            val parts = it.split("|")
            val ip = parts[0]
            val port = parts[1].toInt()
            val username = parts[2]
            val password = parts[3]
            val expires = parts[4]
            val proxy = ProxyEntry(ip, port, username, password)
            Fingerprint(BrowserType.PULSAR_CHROME, proxy)
        }
        
        assertEquals(10, proxies.size)
        assertEquals("125.124.254.178", proxies[1].proxyURI?.host)
        assertEquals(5888, proxies[1].proxyURI?.port)
        assertEquals("gdhx22x:ntmf23x123", proxies[1].proxyURI?.userInfo)
        
        proxies.forEachIndexed { i, proxy ->
            val json = prettyPulsarObjectMapper().writeValueAsString(proxy)
            val path = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve("default").resolve("PULSAR_CHROME")
                .resolve("cx.${i.inc()}")
                .resolve("fingerprint.json")
            // path.toFile().writeText(json)
        }
    }
}
