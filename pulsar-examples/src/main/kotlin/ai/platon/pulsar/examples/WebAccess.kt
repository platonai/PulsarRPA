package ai.platon.pulsar.examples

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_AFTER_FETCH_BATCH_HANDLER
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_BEFORE_FETCH_BATCH_HANDLER
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.component.BatchHandler
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.zip.Deflater

class BeforeBatchHandler: BatchHandler() {
    override fun invoke(pages: Iterable<WebPage>) {
        val size = Iterables.size(pages)
        println("Before fetching - $size pages")
    }
}

class AfterBatchHandler: BatchHandler() {
    override fun invoke(pages: Iterable<WebPage>) {
        val size = Iterables.size(pages)
        val length = pages.joinToString { StringUtil.readableByteCount(it.aveContentBytes.toLong()) }
        val lengthAfterCompress = pages.asSequence()
                .map { it.content?.array()?:"".toByteArray() }
                .joinToString { StringUtil.readableByteCount(compress(it).second.toLong()) }
        println("After fetching - Fetched $size pages, length: \n$length\n$lengthAfterCompress")
    }

    fun compress(input: ByteArray): Pair<ByteArray, Int> {
        val output = ByteArray(input.size)
        val compresser = Deflater()
        compresser.setInput(input)
        compresser.finish()
        val compressedDataLength = compresser.deflate(output)
        return output to compressedDataLength
    }
}

open class WebAccess(
        private var beforeBatchHandler: BatchHandler = BeforeBatchHandler(),
        private var afterBatchHandler: BatchHandler = AfterBatchHandler()
) {
    val log = LoggerFactory.getLogger(WebAccess::class.java)

    val env = PulsarEnv.getOrCreate()
    val pc = PulsarContext.getOrCreate()
    val i = pc.createSession()

    fun load(url: String, args: String) {
        load(url, LoadOptions.parse(args))
    }

    fun load(url: String, options: LoadOptions) {
        val page = i.load(url)
        val doc = i.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        doc.select(options.outlinkSelector) { it.attr("abs:href") }.asSequence()
                .filter { Urls.isValidUrl(it) }
                .mapTo(HashSet()) { it.substringBefore(".com") }
                .asSequence()
                .filter { !it.isBlank() }
                .mapTo(HashSet()) { "$it.com" }
                .filter { NetUtil.testHttpNetwork(URL(it)) }
                .take(10)
                .joinToString("\n") { it }
                .also { println(it) }

        val path = i.export(doc)
        log.info("Export to: file://{}", path)
    }

    fun loadOutPages(portalUrl: String, args: String) {
        loadOutPages(portalUrl, LoadOptions.parse(args))
    }

    fun loadOutPages(portalUrl: String, options: LoadOptions) {
        val page = i.load(portalUrl, options)
        val document = i.parse(page)
        document.absoluteLinks()
        document.stripScripts()
        val path = i.export(document)
        log.info("Portal page is exported to: file://$path")

        val links = document.select(options.outlinkSelector) { it.attr("abs:href") }
                .mapTo(mutableSetOf()) { i.normalize(it) }
                .take(20).map { it.url }
        log.info("Total " + links.size + " items to load")

        i.volatileConfig.putBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, beforeBatchHandler)
        i.volatileConfig.putBean(FETCH_AFTER_FETCH_BATCH_HANDLER, afterBatchHandler)

        val pages = i.loadAll(links, options)

        val query = options.query
        if (query != null) {
            pages.map { i.parse(it) }.map { it.first(query) }.forEachIndexed { i, e ->
                val baseUri = e?.baseUri()?:"(null)"
                val title = e?.text()?:"(null)"
                println("$i\t$baseUri\n$title")
            }
        }
    }

    fun loadAllNews(portalUrl: String, options: LoadOptions) {
        val portal = i.load(portalUrl, options)
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = i.parallelLoadAll(links, LoadOptions.Companion.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }

    fun extractAds() {
        val url = "https://wuhan.baixing.com/xianhualipin/a1100414743.html"
        val doc = i.loadAndParse(url)
        doc.select("a[href~=mssp.baidu]").map {  }
    }

    fun scan(baseUri: String) {
        // val contractBaseUri = "http://www.ccgp-hubei.gov.cn:8040/fcontractAction!download.action?path="
        pc.scan(baseUri).forEachRemaining {
            val size = it.content?.array()?.size?:0
            println(size)
        }
    }

    fun truncate() {
        pc.webDb.truncate()
    }
}
