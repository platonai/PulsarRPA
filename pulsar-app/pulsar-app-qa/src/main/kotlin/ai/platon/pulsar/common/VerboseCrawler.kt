package ai.platon.pulsar.common

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

open class VerboseCrawler(
        val session: PulsarSession = PulsarContexts.createSession()
): AutoCloseable {
    val log = LoggerFactory.getLogger(VerboseCrawler::class.java)
    val closed = AtomicBoolean()
    val isAppActive get() = !closed.get() && session.isActive

    constructor(context: PulsarContext): this(context.createSession())

    fun load(url: String, args: String) {
        return load(url, LoadOptions.parse(args, session.sessionConfig))
    }

    fun load(url: String, options: LoadOptions) {
        val page = session.load(url)
        val doc = session.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        doc.select(options.outLinkSelector) { it.attr("abs:href") }.asSequence()
                .filter { Urls.isValidUrl(it) }
                .mapTo(HashSet()) { it.substringBefore(".com") }
                .asSequence()
                .filter { it.isNotBlank() }
                .mapTo(HashSet()) { "$it.com" }
                .filter { NetUtil.testHttpNetwork(URL(it)) }
                .take(10)
                .joinToString("\n") { it }
                .also { println(it) }

        val path = session.export(doc)
        log.info("Export to: file://{}", path)
    }

    fun loadOutPages(portalUrl: String, args: String): Collection<WebPage> {
        return loadOutPages(portalUrl, LoadOptions.parse(args, session.sessionConfig))
    }

    fun loadOutPages(portalUrl: String, options: LoadOptions): Collection<WebPage> {
        val page = session.load(portalUrl, options)
        if (!page.protocolStatus.isSuccess) {
            log.warn("Failed to load page | {}", portalUrl)
        }

        val document = session.parse(page)
        document.absoluteLinks()
        document.stripScripts()
        val path = session.export(document)
        log.info("Portal page is exported to: file://$path")

        val links = document.select(options.outLinkSelector) { it.attr("abs:href") }
                .mapTo(mutableSetOf()) { session.normalize(it, options) }
                .take(options.topLinks).map { it.spec }
        log.info("Total {} items to load", links.size)

        val itemOptions = options.createItemOptions(session.sessionConfig).apply { parse = true }
        return session.loadAll(links, itemOptions)
    }

    fun loadAllNews(portalUrl: String, options: LoadOptions) {
        val portal = session.load(portalUrl, options)
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = session.parallelLoadAll(links, LoadOptions.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }

    fun extractAds() {
        val url = "https://wuhan.baixing.com/xianhualipin/a1100414743.html"
        val doc = session.loadDocument(url, "")
        doc.select("a[href~=mssp.baidu]").map {  }
    }

    fun scan(baseUri: String) {
        // val contractBaseUri = "http://www.ccgp-hubei.gov.cn:8040/fcontractAction!download.action?path="
        session.context.scan(baseUri).iterator().forEachRemaining {
            val size = it.content?.array()?.size?:0
            println(size)
        }
    }

    fun truncate() {
        session.context.webDb.truncate()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }
}
