package ai.platon.pulsar.test

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.proxy.ProxyEntry2
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.urls.DegenerateHyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.SQLContexts
import org.apache.http.client.utils.URIBuilder
import java.net.Proxy
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class GoogleAgent {
    private val logger = getLogger(this)
    private val args = "-i 7s -parse -refresh"
    private val googleBaseUrl = "https://www.google.com"
    private val submittedDegeneratedLinks = AtomicInteger()
    private val submittedSearchTasks = AtomicInteger()
    
    private val context = SQLContexts.create()
    private val session = context.createSession()
    private val proxyPool get() = context.getBean(ProxyPool::class)
    
    fun initProxies() {
        // # IP:PORT:USER:PASS
        val proxyString = """
127.0.0.1:10808:abc:abc
        """.trimIndent()
        
        val proxies = proxyString
            .split("\n").asSequence()
            .map { it.trim() }
            .filter { !it.startsWith("// ") }
            .map { it.split(":") }
            .filter { it.size == 4 }
            .map { ProxyEntry2(it[0].trim(), it[1].trim().toInt(), it[2], it[3]) }
            .onEach { it.proxyType = Proxy.Type.SOCKS }
            .onEach { it.declaredTTL = Instant.now() + Duration.ofDays(30) }
            .toMutableList()
        
        if (proxies.isEmpty()) {
            logger.info("No proxy available")
            return
        }
        
        proxies.forEach { proxy ->
            if (!NetUtil.testTcpNetwork(proxy.host, proxy.port)) {
                logger.info("Proxy not available: {}", proxy.toURI())
                return
            }
        }
        
        proxies.forEach {
            proxyPool.offer(it.toProxyEntry())
            // ensure enough proxies
            proxyPool.offer(it.toProxyEntry())
        }
        
        logger.info("There are {} proxies in pool", proxyPool.size)
    }
    
    fun google() {
        if (!ProxyPoolManager.isProxyEnabled(session.unmodifiedConfig)) {
            logger.warn("Proxy is disabled")
            return
        }
        
        if (proxyPool.isEmpty()) {
            initProxies()
        }
        
        val async = false
        val limit = 20
        val businessNames = ResourceLoader.readAllLines("entity/business.names.com.txt").shuffled().take(limit)
        val contactNames = listOf("Email", "Phone", "Facebook")
        businessNames.forEach { businessName ->
            contactNames.forEach { contactName ->
                val keyword = "$businessName $contactName"
                if (async) {
                    val degeneratedHyperlink = DegenerateHyperlink(googleBaseUrl, "google") { google(keyword) }
                    session.submit(degeneratedHyperlink)
                    submittedDegeneratedLinks.incrementAndGet()
                } else {
                    google(keyword, async = async)
                }
            }
        }
        
        PulsarContexts.await()
    }
    
    fun google(keyword: String, async: Boolean = true) {
        val builder = URIBuilder("$googleBaseUrl/search")
        builder.addParameter("q", keyword)
        val url = builder.build().toURL().toString()
        val options = session.options(args)
        val be = options.event.browseEvent
        val le = options.event.loadEvent
        
        be.onDocumentActuallyReady.addLast { page, driver ->
            driver.scrollTo("h3:nth-child(3)")
            driver.scrollTo("h3:nth-child(5)")
            driver.scrollTo("h3:nth-child(8)")
            
            driver.click("textarea[name=q]")
            driver.scrollToTop()
            
            println(String.format("%d.\t%s", page.id, page.url))
            val resultStats = driver.firstText("#result-stats")
            println(resultStats)
            val texts = driver.allTexts("h3")
            println(texts)
        }
        
        le.onHTMLDocumentParsed.addLast { page, document ->
            extract(page, document)
        }

//        BrowserSettings.disableProxy()
        if (async) {
            session.submit(url, options)
        } else {
            session.load(url, options)
        }
        submittedSearchTasks.incrementAndGet()
    }
    
    private fun extract(page: WebPage, document: FeaturedDocument) {
        logger.info("Extract | {} | {}", page.protocolStatus, page.url)
    }
    
    private fun test(proxy: ProxyEntry2): Boolean {
        return if (!NetUtil.testTcpNetwork(proxy.host, proxy.port)) {
            logger.info("Proxy not available: {}", proxy.toURI())
            false
        } else true
    }
}

fun main() {
    BrowserSettings.enableProxy()
    
    val agent = GoogleAgent()
    agent.google()
    
    readLine()
}
