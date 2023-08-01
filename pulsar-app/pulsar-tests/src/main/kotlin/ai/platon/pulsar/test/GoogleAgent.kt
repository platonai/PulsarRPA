package ai.platon.pulsar.test

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.urls.DegenerateHyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.context.MultiPrivacyContextManager
import ai.platon.pulsar.ql.context.SQLContexts
import com.github.kklisura.cdt.protocol.commands.Browser
import org.apache.http.client.utils.URIBuilder
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class GoogleAgent {
    private val logger = getLogger(this)
    private val args = "-i 7s -parse"
    private val googleBaseUrl = "https://www.google.com"
    private val submittedDegeneratedLinks = AtomicInteger()
    private val submittedSearchTasks = AtomicInteger()

    private val context = SQLContexts.create()
    private val session = context.createSession()
    private val proxyPool get() = context.getBean(ProxyPool::class)
    private val privacyContextManager get() = context.getBean(MultiPrivacyContextManager::class)

    fun initProxies() {
        // only works before 2023-08-25
        // # IP:PORT:USER:PASS
        val proxyString = """
            146.247.127.238:12323:14a678fa9996c:505721cc2c
            191.96.34.9:12323:14a678fa9996c:505721cc2c
            185.158.105.182:12323:14a678fa9996c:505721cc2c
            194.121.51.251:12323:14a678fa9996c:505721cc2c
            152.89.0.179:12323:14a678fa9996c:505721cc2c
        """.trimIndent()
        val proxies = proxyString
                .split("\n")
                .map { it.trim().split(":") }
                .filter { it.size == 4 }
                .map { l -> ProxyEntry(l[0], l[1].toInt()).also { it.username = l[2]; it.password = l[3] } }
                .onEach { it.declaredTTL = Instant.now() + Duration.ofDays(30) }
        proxies.forEach {
            proxyPool.offer(it)
            // ensure enough proxies
            proxyPool.offer(it)
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

        println("================")
        println(privacyContextManager::class)
        println(privacyContextManager.proxyPoolManager?.javaClass)

        val async = false
        val businessNames = ResourceLoader.readAllLines("entity/business.names.com.txt")
                .take(5)
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
}

fun main() {
    BrowserSettings.enableProxy()

    val agent = GoogleAgent()
    agent.google()

    readLine()
}
