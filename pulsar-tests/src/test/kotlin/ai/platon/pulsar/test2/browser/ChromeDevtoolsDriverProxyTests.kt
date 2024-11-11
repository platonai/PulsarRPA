package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.warnUnexpected
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.delay
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.*

class ChromeDevtoolsDriverProxyTests : WebDriverTestBase() {
    
    private final val providerProxies = """
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
    
    final val proxies = providerProxies.lines().map {
        val parts = it.split("|")
        val ip = parts[0]
        val port = parts[1].toInt()
        val username = parts[2]
        val password = parts[3]
        val expires = parts[4]
        ProxyEntry(ip, port, username, password)
    }
    val proxyEntry = proxies[2]
    
//    val proxyEntry = ProxyEntry("113.219.171.252", 2018)
    val ipTestUrl = "https://ip.tool.chinaz.com/"
    val browserId = BrowserId.RANDOM
    
    @BeforeTest
    fun setup() {
        assumeTrue(proxyEntry.test(), "Test skipped because the proxy is not available")
        browserId.fingerprint.setProxy(proxyEntry)
    }
    
    @AfterTest
    fun tearDown() {
        kotlin.runCatching { FileUtils.deleteDirectory(browserId.userDataDir.toFile()) }.onFailure { println(it.brief()) }
    }
    
    @Test
    fun `When navigate to a HTML page with proxy then success`() = runWebDriverTest(browserId) { driver ->
        open(ipTestUrl, driver, 1)
        
        val navigateEntry = driver.navigateEntry
        assertTrue { navigateEntry.documentTransferred }
        assertTrue { navigateEntry.networkRequestCount.get() > 0 }
        assertTrue { navigateEntry.networkResponseCount.get() > 0 }
        
        require(driver is AbstractWebDriver)
        assertEquals(200, driver.mainResponseStatus)
        assertTrue { driver.mainResponseStatus == 200 }
        assertTrue { driver.mainResponseHeaders.isNotEmpty() }
        assertEquals(200, navigateEntry.mainResponseStatus)
        assertTrue { navigateEntry.mainResponseStatus == 200 }
        assertTrue { navigateEntry.mainResponseHeaders.isNotEmpty() }

        assertTrue { browserId.fingerprint.proxyURI?.host == proxyEntry.host }
        assertTrue { driver.selectFirstTextOrNull(".ipLocalContent")?.contains(proxyEntry.host) == true }
        
        // readlnOrNull()
        delay(3000)
    }
}
