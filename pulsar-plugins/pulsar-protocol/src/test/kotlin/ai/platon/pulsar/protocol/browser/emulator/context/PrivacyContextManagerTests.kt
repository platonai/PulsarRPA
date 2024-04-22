package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.UserAgent
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyAgent
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.SequentialPrivacyAgentGenerator
import ai.platon.pulsar.persist.WebPageExt
import ai.platon.pulsar.protocol.browser.emulator.DefaultWebDriverPoolManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.*

class PrivacyContextManagerTests {
    private val contextPathBase = Files.createTempDirectory("test-")
    private val contextPath = contextPathBase.resolve("cx.5kDMDS2")
    private val contextPath2 = contextPathBase.resolve("cx.7KmtAC2")
    private val conf = ImmutableConfig()
    private val driverPoolManager = DefaultWebDriverPoolManager(conf)
    
    @BeforeTest
    fun setup() {
        BrowserSettings.privacy(6).maxTabs(10).withSequentialBrowsers(15)
    }
    
    @Test
    fun testPrivacyContextReport() {
        var report = String.format(
            "Privacy context has lived for %s | %s | %s" +
                " | success: %s(%s pages/s) | small: %s(%s) | traffic: %s(%s/s) | tasks: %s total run: %s | proxy: %s",
            // Privacy context has lived for {} | {} | {}
            Duration.ofMinutes(1), "03092518dNOgA1", "active ready",
            // success: {}({} pages/s)
            1023, String.format("%.2f", 0.45),
            // small: {}({})
            101, String.format("%.1f%%", 100 * 0.01),
            // traffic: {}({}/s)
            "1G",
            "1M",
            // tasks: {} total run: {}
            123, 234,
            // proxy: {}
            "8.8.8.8"
        )
        
        // println(report)
        
        assertTrue { report.contains("Privacy context has lived for PT1M | 03092518dNOgA1 | active ready") }
        assertTrue { report.contains("success: 1023(0.45 pages/s)") }
        assertTrue { report.contains("small: 101(1.0%)") }
        assertTrue { report.contains("traffic: 1G(1M/s)") }
        assertTrue { report.contains("tasks: 123 total run: 234") }
    }
    
    @Test
    fun testPrivacyContextComparison() {
        val privacyManager = MultiPrivacyContextManager(driverPoolManager, conf)
        val fingerprint = Fingerprint(BrowserType.MOCK_CHROME)
        
        val pc = privacyManager.computeNextContext(fingerprint)
        assertTrue { pc.isActive }
        privacyManager.close(pc)
        assertTrue { !pc.isActive }
        assertFalse { privacyManager.temporaryContexts.containsKey(pc.privacyAgent) }
        assertFalse { privacyManager.temporaryContexts.containsValue(pc) }
        
        val pc2 = privacyManager.computeNextContext(fingerprint)
        assertTrue { pc2.isActive }
        assertNotEquals(pc.privacyAgent, pc2.privacyAgent)
        assertNotEquals(pc, pc2)
        assertTrue { privacyManager.temporaryContexts.containsKey(pc2.privacyAgent) }
        assertTrue { privacyManager.temporaryContexts.containsValue(pc2) }
    }
    
    @Test
    fun testPrivacyContextClosing() {
        val privacyManager = MultiPrivacyContextManager(driverPoolManager, conf)
        val userAgents = UserAgent()
        
        repeat(100) {
            val proxyServer = "127.0.0." + Random.nextInt(200)
            val userAgent = userAgents.getRandomUserAgent()
            val fingerprint = Fingerprint(BrowserType.PULSAR_CHROME, proxyServer, userAgent = userAgent)
            val pc = privacyManager.computeNextContext(fingerprint)
            
            assertTrue { pc.isActive }
            privacyManager.close(pc)
            assertTrue { !pc.isActive }
            assertFalse { privacyManager.temporaryContexts.containsKey(pc.privacyAgent) }
            assertFalse { privacyManager.temporaryContexts.containsValue(pc) }
        }
    }
    
    @Test
    fun testPrivacyContextClosingConcurrently() {
        val privacyManager = MultiPrivacyContextManager(driverPoolManager, conf)
        val userAgents = UserAgent()
        
        val volatileContexts = ConcurrentLinkedDeque<PrivacyContext>()
        val producer = Executors.newSingleThreadScheduledExecutor()
        val closer = Executors.newSingleThreadScheduledExecutor()
        
        producer.scheduleWithFixedDelay({
            val proxyServer = "127.0.0." + Random.nextInt(200)
            val userAgent = userAgents.getRandomUserAgent()
            val fingerprint = Fingerprint(BrowserType.MOCK_CHROME, proxyServer, userAgent = userAgent)
            val pc = privacyManager.computeNextContext(fingerprint)
            
            volatileContexts.add(pc)
            assertTrue { pc.isActive }
        }, 1, 800, TimeUnit.MILLISECONDS)
        
        closer.scheduleWithFixedDelay({
            volatileContexts.forEach { pc ->
                // proxy server can be changed, which will be improved in the further
                pc.privacyAgent.fingerprint.proxyServer = "127.0.0." + Random.nextInt(200)
                
                privacyManager.close(pc)
                assertTrue { !pc.isActive }
                assertFalse { privacyManager.temporaryContexts.containsKey(pc.privacyAgent) }
                assertFalse { privacyManager.temporaryContexts.containsValue(pc) }
            }
        }, 2, 1, TimeUnit.SECONDS)
        
        producer.awaitTermination(15, TimeUnit.SECONDS)
        
        producer.shutdownNow()
        closer.shutdownNow()
    }
    
    @Test
    fun `When a privacy context closed then it's removed from the active queue`() {
        val manager = MultiPrivacyContextManager(driverPoolManager, conf)
        
        val agent = PrivacyAgent(contextPath, BrowserType.MOCK_CHROME)
        val privacyContext = manager.computeIfAbsent(agent)
        
        assertTrue { manager.temporaryContexts.containsKey(agent) }
        manager.close(privacyContext)
        assertFalse { manager.temporaryContexts.containsKey(agent) }
    }
    
    @Test
    fun `When tasks run then contexts rotates`() {
        val manager = MultiPrivacyContextManager(driverPoolManager, conf)
        val url = "about:blank"
        val page = WebPageExt.newTestWebPage(url)
        
        runBlocking {
            repeat(10) {
                val task = FetchTask.create(page)
                task.fingerprint.userAgent = RandomStringUtils.randomAlphanumeric(10)
                manager.run(task) { _, driver -> mockFetch(task, driver) }
                assertTrue { manager.temporaryContexts.size <= manager.maxAllowedBadContexts }
            }
        }
    }
    
    @Ignore("Failed, will correct the test later")
    @Test
    fun `When task run then maintainer started`() {
        val manager = MultiPrivacyContextManager(driverPoolManager, conf)
        val url = "about:blank"
        val page = WebPageExt.newTestWebPage(url)
        
        runBlocking {
            val task = FetchTask.create(page)
            manager.run(task) { _, driver -> mockFetch(task, driver) }
            var n = 3600
            while (n-- > 0 && manager.maintainCount.get() <= 0) {
                delay(1000)
            }
            
            assertTrue { n > 0 }
            assertTrue { manager.maintainCount.get() > 0 }
        }
    }
    
    private suspend fun mockFetch(task: FetchTask, driver: WebDriver): FetchResult {
        return FetchResult.canceled(task)
    }
}
